package com.shohankhan.hisaabos.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.model.CategoryKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CategoriesState(
    val loading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val error: String? = null,
)

class CategoriesViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesState())
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.invalidation("categories", "transactions").collect { reload() }
        }
    }

    private suspend fun reload() {
        _state.value = _state.value.copy(
            loading = false,
            categories = repository.observeCategories().first(),
        )
    }

    fun setKind(kind: CategoryKind) {
        _state.value = _state.value.copy(kind = kind)
    }

    fun save(existing: CategoryEntity?, name: String, iconKey: String, colorArgb: Long, kind: CategoryKind) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "ক্যাটাগরির নাম লিখুন")
            return
        }
        viewModelScope.launch {
            val duplicate = repository.countCategoryName(name.trim(), kind, existing?.id ?: -1)
            if (duplicate > 0) {
                _state.value = _state.value.copy(error = "এই নামে একটি ক্যাটাগরি আগেই আছে")
                return@launch
            }
            if (existing == null) {
                repository.createCategoryDetailed(name, kind, iconKey, colorArgb)
            } else {
                repository.updateCategory(
                    existing.copy(name = name.trim(), iconKey = iconKey, colorArgb = colorArgb, kind = kind)
                )
            }
            _state.value = _state.value.copy(error = null)
            reload()
        }
    }

    fun archive(id: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveCategory(id, archived)
            reload()
        }
    }

    /** Deletes only when unused; otherwise the UI must offer reassignment or archiving. */
    fun deleteOrReport(id: Long, onBlocked: (Int) -> Unit) {
        viewModelScope.launch {
            val usage = repository.categoryUsage(id)
            if (usage > 0) {
                onBlocked(usage)
            } else {
                repository.deleteCategory(id)
                reload()
            }
        }
    }

    fun reassignAndDelete(fromId: Long, toId: Long?) {
        viewModelScope.launch {
            repository.reassignCategory(fromId, toId)
            repository.deleteCategory(fromId)
            reload()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
