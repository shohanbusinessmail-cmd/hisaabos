package com.shohankhan.hisaabos.ui.books

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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Storefront
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
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.report.FinanceSnapshot
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun BooksScreen(
    snapshot: FinanceSnapshot,
    onOpenAccounts: () -> Unit,
    onOpenShops: () -> Unit,
    onOpenPeople: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenLoans: () -> Unit,
    onOpenEmi: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenBudgets: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(AppBackground).verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(Bn.navBooks, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("আপনার সব হিসাব, তালিকা ও পরিকল্পনা", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(14.dp))
        HisaabCard {
            Text("মোট সম্পদ", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            AmountText(minor = snapshot.position.totalAssetsMinor, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            Column {
                KeyValueRow("দায়", Money.format(snapshot.position.totalLiabilitiesMinor))
                KeyValueRow("নিট মূল্য", Money.format(snapshot.position.netWorthMinor), valueColor = Emerald)
                KeyValueRow("দোকানে বাকি", Money.format(snapshot.position.shopPayableMinor), valueColor = ExpenseRed)
                KeyValueRow("আমার পাওনা", Money.format(snapshot.position.receivableMinor), valueColor = Emerald)
                KeyValueRow("আমার দেনা", Money.format(snapshot.position.borrowedOutstandingMinor), valueColor = ExpenseRed)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("হিসাব", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        EntryRow(Bn.accounts, Icons.Outlined.Wallet, onOpenAccounts)
        EntryRow(Bn.categories, Icons.Outlined.Category, onOpenCategories)
        EntryRow(Bn.shops, Icons.Outlined.Storefront, onOpenShops)
        EntryRow(Bn.people, Icons.Outlined.People, onOpenPeople)
        Spacer(Modifier.height(14.dp))
        Text("পরিকল্পনা", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        EntryRow(Bn.loansTitle, Icons.Outlined.AccountBalance, onOpenLoans)
        EntryRow(Bn.emiPurchase, Icons.Outlined.CreditCard, onOpenEmi)
        EntryRow(Bn.savings, Icons.Outlined.Savings, onOpenSavings)
        EntryRow(Bn.budgets, Icons.Outlined.Category, onOpenBudgets)
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun EntryRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    HisaabCard(modifier = Modifier.padding(bottom = 8.dp).clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Emerald, modifier = Modifier.width(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun KeyValueRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
