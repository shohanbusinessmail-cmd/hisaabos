package com.shohankhan.hisaabos.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.ui.components.CategoryChip
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.Pill
import com.shohankhan.hisaabos.ui.components.categoryColorOptions
import com.shohankhan.hisaabos.ui.components.categoryIcon
import com.shohankhan.hisaabos.ui.components.categoryIconOptions
import com.shohankhan.hisaabos.ui.components.colorFromArgb
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
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
    var blocked by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    val items = state.categories.filter { it.kind == state.kind }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.categories,
            subtitle = "নিজের ইচ্ছেমতো ক্যাটাগরি তৈরি করুন",
            onBack = onBack,
            actions = {
                IconButton(onClick = { editing = null; showEditor = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newCategory, tint = Emerald)
                }
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip(Bn.expenseCategory, state.kind == CategoryKind.EXPENSE) { onKindChange(CategoryKind.EXPENSE) }
                CategoryChip(Bn.incomeCategory, state.kind == CategoryKind.INCOME) { onKindChange(CategoryKind.INCOME) }
            }
            Spacer(Modifier.height(12.dp))
            if (state.error != null) {
                InfoNote(
                    text = state.error,
                    tone = NoteTone.ERROR,
                    modifier = Modifier.clickable { onClearError() },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Category,
                    title = "কোনো ${if (state.kind == CategoryKind.EXPENSE) "খরচের" else "আয়ের"} ক্যাটাগরি নেই",
                    message = "অ্যাপে আগে থেকে কোনো ক্যাটাগরি দেওয়া নেই। নিজের প্রয়োজন অনুযায়ী তৈরি করুন।",
                    actionLabel = Bn.newCategory,
                    onAction = { editing = null; showEditor = true },
                )
            } else {
                HisaabCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items.forEachIndexed { index, category ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { editing = category; showEditor = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBadge(
                                icon = categoryIcon(category.iconKey),
                                tint = colorFromArgb(category.colorArgb),
                                background = colorFromArgb(category.colorArgb).copy(alpha = 0.12f),
                                size = 38,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(category.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                if (category.isArchived) {
                                    Pill("আর্কাইভ", color = TextSecondary, background = com.shohankhan.hisaabos.ui.theme.NeutralSoft)
                                }
                            }
                            IconButton(onClick = { editing = category; showEditor = true }) {
                                Icon(Icons.Outlined.Edit, contentDescription = Bn.edit, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                if (category.isArchived) onArchive(category.id, false)
                                else onDelete(category.id) { count -> blocked = category.id to count }
                            }) {
                                Icon(
                                    if (category.isArchived) Icons.Outlined.Inventory2 else Icons.Outlined.DeleteOutline,
                                    contentDescription = Bn.delete,
                                    tint = if (category.isArchived) Emerald else TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        if (index < items.size - 1) DividerLine(Modifier.padding(horizontal = 16.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "ক্যাটাগরি মুছলে পুরনো লেনদেন মুছে যায় না — ব্যবহার থাকলে অন্য ক্যাটাগরিতে সরানো যায় বা আর্কাইভ করা যায়।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(96.dp))
        }
    }

    if (showEditor) {
        CategoryEditorDialog(
            existing = editing,
            kind = state.kind,
            onDismiss = { showEditor = false },
            onSave = { category, name, icon, color, kind ->
                showEditor = false
                onSave(category, name, icon, color, kind)
            },
        )
    }
    if (blocked != null) {
        ReassignDialog(
            count = blocked!!.second,
            categories = state.categories.filter { it.kind == state.kind && it.id != blocked!!.first },
            onDismiss = { blocked = null },
            onArchive = {
                onArchive(blocked!!.first, true)
                blocked = null
            },
            onMove = { target ->
                onReassignAndDelete(blocked!!.first, target)
                blocked = null
            },
        )
    }
}

@Composable
private fun CategoryEditorDialog(
    existing: CategoryEntity?,
    kind: CategoryKind,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity?, String, String, Long, CategoryKind) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var iconKey by remember(existing) { mutableStateOf(existing?.iconKey ?: "tag") }
    var color by remember(existing) { mutableStateOf(existing?.colorArgb ?: 0xFF087443) }
    var localKind by remember(existing) { mutableStateOf(existing?.kind ?: kind) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) Bn.newCategory else "ক্যাটাগরি সম্পাদনা") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                HisaabTextField(value = name, onValueChange = { name = it }, label = Bn.categoryName)
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = Bn.categoryKind,
                    options = listOf(CategoryKind.EXPENSE, CategoryKind.INCOME),
                    selected = localKind,
                    optionLabel = { Bn.categoryKind(it) },
                    onSelect = { localKind = it },
                )
                Spacer(Modifier.height(8.dp))
                Text("আইকন", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
                    categoryIconOptions.take(6).forEach { option ->
                        IconButton(onClick = { iconKey = option.key }) {
                            Icon(
                                option.icon,
                                contentDescription = option.label,
                                tint = if (iconKey == option.key) Emerald else TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    categoryIconOptions.drop(6).forEach { option ->
                        IconButton(onClick = { iconKey = option.key }) {
                            Icon(
                                option.icon,
                                contentDescription = option.label,
                                tint = if (iconKey == option.key) Emerald else TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Text("রং", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryColorOptions.take(5).forEach { argb ->
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(colorFromArgb(argb))
                                .clickable { color = argb },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (color == argb) Text("✓", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(existing, name, iconKey, color, localKind) },
                enabled = name.isNotBlank(),
            ) { Text(Bn.save, color = Emerald) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Bn.cancel) } },
    )
}

@Composable
private fun ReassignDialog(
    count: Int,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onArchive: () -> Unit,
    onMove: (Long?) -> Unit,
) {
    var target by remember { mutableStateOf<CategoryEntity?>(categories.firstOrNull()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ক্যাটাগরি মুছবেন?") },
        text = {
            Column {
                Text("এই ক্যাটাগরিতে $count টি লেনদেন আছে। সেগুলো মুছে ফেলা হবে না — অন্য ক্যাটাগরিতে সরান অথবা ক্যাটাগরি আর্কাইভ করুন।")
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "নতুন ক্যাটাগরি",
                    options = categories,
                    selected = target,
                    optionLabel = { it.name },
                    onSelect = { target = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onMove(target?.id) }, enabled = target != null) { Text("সরিয়ে মুছুন", color = ExpenseRed) }
        },
        dismissButton = {
            TextButton(onClick = onArchive) { Text("আর্কাইভ করুন", color = Emerald) }
        },
    )
}
