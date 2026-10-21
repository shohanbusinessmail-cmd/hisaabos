package com.shohankhan.hisaabos.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.ui.components.CategoryChip
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.Pill
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.categoryColorOptions
import com.shohankhan.hisaabos.ui.components.categoryIcon
import com.shohankhan.hisaabos.ui.components.categoryIconOptions
import com.shohankhan.hisaabos.ui.components.colorFromArgb
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Divider
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun CategoriesScreen(
    state: CategoriesState,
    onBack: () -> Unit,
    onKindChange: (CategoryKind) -> Unit,
    onSave: (CategoryEntity?, String, String, Long, CategoryKind) -> Unit,
    onArchive: (Long, Boolean) -> Unit,
    onDelete: (Long, onBlocked: (Int) -> Unit) -> Unit,
    onReassignAndDelete: (Long, Long?) -> Unit,
    onClearError: () -> Unit,
) {
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf<Pair<Long, Int>?>(null) }

    val visible = state.categories.filter { it.kind == state.kind || it.kind == CategoryKind.BOTH }
    val active = visible.filter { !it.isArchived }
    val archived = visible.filter { it.isArchived }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.categories,
            subtitle = "নিজের ইচ্ছেমতো ক্যাটাগরি তৈরি করুন",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    editing = null
                    showEditor = true
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newCategory, tint = Emerald)
                }
            },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip(Bn.expenseCategory, state.kind == CategoryKind.EXPENSE) { onKindChange(CategoryKind.EXPENSE) }
                CategoryChip(Bn.incomeCategory, state.kind == CategoryKind.INCOME) { onKindChange(CategoryKind.INCOME) }
                CategoryChip(Bn.allCategories, state.kind == CategoryKind.BOTH) { onKindChange(CategoryKind.BOTH) }
            }
            Spacer(Modifier.height(12.dp))
            if (state.error != null) {
                InfoNote(text = state.error, tone = NoteTone.ERROR, modifier = Modifier.padding(bottom = 8.dp))
                TextButton(onClick = onClearError) { Text(Bn.close) }
            }
            if (active.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Category,
                    title = "কোনো ক্যাটাগরি নেই",
                    message = "হিসাবওএস-এ আগে থেকে কোনো আয়-ব্যয়ের ক্যাটাগরি দেওয়া নেই। আপনার নিজের ভাষায় ক্যাটাগরি তৈরি করুন।",
                    actionLabel = Bn.newCategory,
                    onAction = {
                        editing = null
                        showEditor = true
                    },
                )
            }
            active.forEach { category ->
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = categoryIcon(category.iconKey),
                            tint = colorFromArgb(category.colorArgb),
                            background = colorFromArgb(category.colorArgb).copy(alpha = 0.12f),
                            size = 38,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(category.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(
                                when (category.kind) {
                                    CategoryKind.EXPENSE -> "খরচের ক্যাটাগরি"
                                    CategoryKind.INCOME -> "আয়ের ক্যাটাগরি"
                                    CategoryKind.BOTH -> "আয় ও খরচ দুইটোতেই"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        IconButton(onClick = {
                            editing = category
                            showEditor = true
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = Bn.edit, tint = TextSecondary)
                        }
                        IconButton(onClick = { onArchive(category.id, true) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = Bn.archive, tint = TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        TextButton(onClick = { onDelete(category.id) { count -> blocked = category.id to count } }) {
                            Text(Bn.delete, color = ExpenseRed, style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(onClick = { onArchive(category.id, true) }) {
                            Text(Bn.archive, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            if (archived.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(text = if (showArchived) "আর্কাইভ লুকান" else "আর্কাইভ দেখান", onClick = { showArchived = !showArchived })
                if (showArchived) {
                    archived.forEach { category ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(categoryIcon(category.iconKey), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(category.name, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onArchive(category.id, false) }) { Text(Bn.unarchive, color = Emerald) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(90.dp))
        }
    }

    if (showEditor) {
        var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
        var kind by remember(editing) { mutableStateOf(editing?.kind ?: state.kind.takeIf { it != CategoryKind.BOTH } ?: CategoryKind.EXPENSE) }
        var iconKey by remember(editing) { mutableStateOf(editing?.iconKey ?: "tag") }
        var color by remember(editing) { mutableStateOf(editing?.colorArgb ?: categoryColorOptions.first()) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) Bn.newCategory else Bn.edit) },
            text = {
                Column {
                    HisaabTextField(value = name, onValueChange = { name = it }, label = Bn.categoryName)
                    Spacer(Modifier.height(8.dp))
                    DropdownField(
                        label = Bn.categoryKind,
                        options = listOf(CategoryKind.EXPENSE, CategoryKind.INCOME, CategoryKind.BOTH),
                        selected = kind,
                        optionLabel = {
                            when (it) {
                                CategoryKind.EXPENSE -> Bn.expenseCategory
                                CategoryKind.INCOME -> Bn.incomeCategory
                                CategoryKind.BOTH -> "দুইটোই"
                            }
                        },
                        onSelect = { kind = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("আইকন", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        categoryIconOptions.take(6).forEach { option ->
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .border(
                                        width = if (option.key == iconKey) 2.dp else 1.dp,
                                        color = if (option.key == iconKey) Emerald else Divider,
                                        shape = CircleShape,
                                    )
                                    .clickable { iconKey = option.key },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(option.icon, contentDescription = option.label, tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("রঙ", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        categoryColorOptions.forEach { value ->
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .border(
                                        width = if (value == color) 2.dp else 0.dp,
                                        color = TextPrimary,
                                        shape = CircleShape,
                                    )
                                    .padding(2.dp)
                                    .background(colorFromArgb(value), CircleShape)
                                    .clickable { color = value },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(editing, name, iconKey, color, kind)
                    showEditor = false
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text(Bn.cancel) } },
        )
    }

    blocked?.let { (categoryId, count) ->
        val target = remember { mutableStateOf<CategoryEntity?>(null) }
        AlertDialog(
            onDismissRequest = { blocked = null },
            title = { Text("ক্যাটাগরি মুছবেন?") },
            text = {
                Column {
                    InfoNote(
                        text = "এই ক্যাটাগরিতে $countটি লেনদেন আছে। মুছলে সেগুলো অন্য ক্যাটাগরিতে সরাতে হবে, নাহলে শুধু আর্কাইভ হবে।",
                        tone = NoteTone.WARNING,
                    )
                    Spacer(Modifier.height(8.dp))
                    DropdownField(
                        label = "নতুন ক্যাটাগরি",
                        options = state.categories.filter { it.id != categoryId && !it.isArchived },
                        selected = target.value,
                        optionLabel = { it.name },
                        onSelect = { target.value = it },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val to = target.value
                    if (to != null) {
                        onReassignAndDelete(categoryId, to.id)
                    } else {
                        onArchive(categoryId, true)
                    }
                    blocked = null
                }) { Text(Bn.confirm, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { blocked = null }) { Text(Bn.cancel) } },
        )
    }
}
