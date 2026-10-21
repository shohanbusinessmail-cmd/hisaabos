package com.shohankhan.hisaabos.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Wallet
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
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun MoreScreen(
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onPeople: () -> Unit,
    onShops: () -> Unit,
    onLoans: () -> Unit,
    onEmi: () -> Unit,
    onSavings: () -> Unit,
    onBudgets: () -> Unit,
    onRecurring: () -> Unit,
    onReminders: () -> Unit,
    onBackup: () -> Unit,
    onSecurity: () -> Unit,
    onAppearance: () -> Unit,
    onAbout: () -> Unit,
    onVerify: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(Bn.moreTitle, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("সেটিংস, টুলস ও নিরাপত্তা", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(14.dp))

        SectionHeader(title = Bn.financesGroup)
        HisaabCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            MoreRow(Icons.Outlined.Wallet, Bn.accounts, "নগদ, ব্যাংক, ওয়ালেট", onAccounts)
            MoreDivider()
            MoreRow(Icons.Outlined.Category, Bn.categories, "নিজের ক্যাটাগরি তৈরি ও পরিচালনা", onCategories)
            MoreDivider()
            MoreRow(Icons.Outlined.People, Bn.people, "ধার দেওয়া-নেওয়ার হিসাব", onPeople)
            MoreDivider()
            MoreRow(Icons.Outlined.Storefront, Bn.shops, "দোকানভিত্তিক বাকির খাতা", onShops)
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader(title = Bn.planningGroup)
        HisaabCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            MoreRow(Icons.Outlined.AccountBalance, Bn.loansTitle, "এনজিও ও ব্যাংক ঋণ", onLoans)
            MoreDivider()
            MoreRow(Icons.Outlined.CreditCard, Bn.emiPurchase, "কিস্তির চুক্তি ও পরিশোধ", onEmi)
            MoreDivider()
            MoreRow(Icons.Outlined.Savings, Bn.savings, "লক্ষ্য ও জমা", onSavings)
            MoreDivider()
            MoreRow(Icons.Outlined.BarChart, Bn.budgets, "খরচের সীমা ঠিক করুন", onBudgets)
            MoreDivider()
            MoreRow(Icons.Outlined.Repeat, Bn.recurring, "নিয়মিত লেনদেনের টেমপ্লেট", onRecurring)
            MoreDivider()
            MoreRow(Icons.Outlined.Notifications, Bn.reminders, "পরিশোধ ও জমার মনে করানো", onReminders)
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader(title = Bn.dataGroup)
        HisaabCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            MoreRow(Icons.Outlined.Download, Bn.backup, "ব্যাকআপ, পুনরুদ্ধার ও রপ্তানি", onBackup)
            MoreDivider()
            MoreRow(Icons.Outlined.Fingerprint, Bn.security, "পিন, বায়োমেট্রিক ও লক", onSecurity)
            MoreDivider()
            MoreRow(Icons.Outlined.VerifiedUser, Bn.verifyBooks, Bn.verifyBooksBody, onVerify)
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader(title = Bn.otherGroup)
        HisaabCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            MoreRow(Icons.Outlined.Palette, Bn.appearance, "শুধু লাইট মোড", onAppearance)
            MoreDivider()
            MoreRow(Icons.Outlined.Info, Bn.about, "সংস্করণ, গোপনীয়তা ও ডেভেলপার", onAbout)
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun MoreDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(com.shohankhan.hisaabos.ui.theme.Divider),
    )
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon = icon, size = 38)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF68766E))
    }
}
