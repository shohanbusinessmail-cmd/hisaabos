package com.shohankhan.hisaabos.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

private enum class MoreGroup(val title: String) {
    ACCOUNTS("হিসাব ও তালিকা"),
    PLANNING("পরিকল্পনা"),
    DATA("তথ্য ও নিরাপত্তা"),
    APP("অ্যাপ"),
}

private data class MoreItem(
    val group: MoreGroup,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun MoreScreen(
    onBackup: () -> Unit,
    onSecurity: () -> Unit,
    onAppearance: () -> Unit,
    onAbout: () -> Unit,
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onShops: () -> Unit,
    onPeople: () -> Unit,
    onLoans: () -> Unit,
    onEmi: () -> Unit,
    onSavings: () -> Unit,
    onBudgets: () -> Unit,
    onReminders: () -> Unit,
    onRecurring: () -> Unit,
    onVerify: () -> Unit,
) {
    val items = listOf(
        MoreItem(MoreGroup.ACCOUNTS, Bn.accounts, "নগদ, ব্যাংক ও মোবাইল ব্যাংকিং", Icons.Outlined.AccountBalance, onAccounts),
        MoreItem(MoreGroup.ACCOUNTS, Bn.categories, "আয় ও খরচের নিজের ক্যাটাগরি", Icons.Outlined.ReceiptLong, onCategories),
        MoreItem(MoreGroup.ACCOUNTS, Bn.shops, "দোকান ও দোকানদারের বাকির খাতা", Icons.Outlined.ReceiptLong, onShops),
        MoreItem(MoreGroup.ACCOUNTS, Bn.people, "ব্যক্তিগত ধার-দেনা ও পাওনা", Icons.Outlined.AccountBalance, onPeople),
        MoreItem(MoreGroup.PLANNING, Bn.loansTitle, "এনজিও ও ব্যাংক ঋণ", Icons.Outlined.AccountBalance, onLoans),
        MoreItem(MoreGroup.PLANNING, Bn.emiPurchase, "কিস্তিতে কেনা পণ্য", Icons.Outlined.ReceiptLong, onEmi),
        MoreItem(MoreGroup.PLANNING, Bn.savings, "লক্ষ্য ও জমা", Icons.Outlined.AccountBalance, onSavings),
        MoreItem(MoreGroup.PLANNING, Bn.budgets, "খরচের সীমা", Icons.Outlined.ReceiptLong, onBudgets),
        MoreItem(MoreGroup.PLANNING, Bn.reminders, "মনে করানো ও নিয়মিত লেনদেন", Icons.Outlined.ReceiptLong, onReminders),
        MoreItem(MoreGroup.PLANNING, Bn.recurring, "নিয়মিত লেনদেনের টেমপ্লেট", Icons.Outlined.ReceiptLong, onRecurring),
        MoreItem(MoreGroup.DATA, Bn.backupTitle, "ফাইল তৈরি ও পুনরুদ্ধার", Icons.Outlined.Settings, onBackup),
        MoreItem(MoreGroup.DATA, Bn.security, "পিন, বায়োমেট্রিক ও লক", Icons.Outlined.Lock, onSecurity),
        MoreItem(MoreGroup.DATA, Bn.verifyBooks, "ব্যালেন্স ও লেনদেন যাচাই", Icons.Outlined.Settings, onVerify),
        MoreItem(MoreGroup.APP, Bn.appearance, "বাংলা সংখ্যা ও লুকানো অঙ্ক", Icons.Outlined.Palette, onAppearance),
        MoreItem(MoreGroup.APP, Bn.about, "সংস্করণ, গোপনীয়তা ও যোগাযোগ", Icons.Outlined.CameraAlt, onAbout),
    )
    Column(Modifier.fillMaxSize().background(AppBackground).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(Bn.navMore, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("সব হিসাব এক জায়গা থেকে", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(14.dp))
        MoreGroup.entries.forEach { group ->
            Text(group.title, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
            items.filter { it.group == group }.forEach { item ->
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp).clickable { item.onClick() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = item.icon, tint = Emerald, background = Emerald.copy(alpha = 0.1f), size = 36)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}
