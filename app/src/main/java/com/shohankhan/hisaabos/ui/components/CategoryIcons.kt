package com.shohankhan.hisaabos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Gift
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryIconOption(val key: String, val label: String, val icon: ImageVector)

val categoryIconOptions: List<CategoryIconOption> = listOf(
    CategoryIconOption("tag", "ট্যাগ", Icons.Outlined.Category),
    CategoryIconOption("food", "খাবার", Icons.Outlined.Fastfood),
    CategoryIconOption("transport", "যাতায়াত", Icons.Outlined.DirectionsBus),
    CategoryIconOption("health", "চিকিৎসা", Icons.Outlined.LocalHospital),
    CategoryIconOption("education", "শিক্ষা", Icons.Outlined.School),
    CategoryIconOption("home", "ঘর", Icons.Outlined.Home),
    CategoryIconOption("gift", "উপহার", Icons.Outlined.CardGiftcard),
    CategoryIconOption("salary", "বেতন", Icons.Outlined.Work),
    CategoryIconOption("shopping", "কেনাকাটা", Icons.Outlined.ShoppingBag),
    CategoryIconOption("bills", "বিল", Icons.Outlined.Receipt),
    CategoryIconOption("bank", "ব্যাংক", Icons.Outlined.AccountBalance),
    CategoryIconOption("other", "অন্যান্য", Icons.Outlined.Payments),
)

fun categoryIcon(key: String?): ImageVector =
    categoryIconOptions.firstOrNull { it.key == key }?.icon ?: Icons.Outlined.Category

val categoryColorOptions: List<Long> = listOf(
    0xFF087443, 0xFF1E6F5C, 0xFF3A5A78, 0xFF8A5A00, 0xFFB3261E,
    0xFF6B4FBB, 0xFFB25E00, 0xFF00695C, 0xFF5D4037, 0xFF37474F,
)

fun colorFromArgb(value: Long): Color = Color(value)
