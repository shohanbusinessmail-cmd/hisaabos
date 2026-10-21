package com.shohankhan.hisaabos.ui.books

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
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.ProgressTrack
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
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
) {
    val position = snapshot.position
    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(Bn.navBooks, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("আপনার সব হিসাব, দোকান, ঋণ ও সঞ্চয় এক জায়গায়", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(14.dp))

        HisaabCard {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(Bn.netWorth, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    AmountText(minor = position.netWorthMinor, style = MaterialTheme.typography.titleLarge, color = EmeraldDeep)
                }
                Column(Modifier.weight(1f)) {
                    Text(Bn.availableCash, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    AmountText(minor = position.cashAndBankMinor, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
            }
            Spacer(Modifier.height(10.dp))
            DividerLine()
            Spacer(Modifier.height(10.dp))
            Row {
                MiniStat("সম্পদ", position.totalAssetsMinor, Modifier.weight(1f))
                MiniStat("দায়", position.totalLiabilitiesMinor, Modifier.weight(1f), ExpenseRed)
                MiniStat("সঞ্চয়", position.savingsMinor, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "হিসাব ও তালিকা")
        HisaabCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            BooksRow(Icons.Outlined.Wallet, Bn.accounts, "${snapshot.accounts.count { !it.account.isArchived }}টি হিসাব", onOpenAccounts)
            DividerLine(Modifier.padding(horizontal = 16.dp))
            BooksRow(
                Icons.Outlined.Storefront,
                Bn.shops,
                if (position.shopPayableMinor > 0) "বাকি ${Money.format(position.shopPayableMinor)}" else "কোনো বাকি নেই",
                onOpenShops,
            )
            DividerLine(Modifier.padding(horizontal = 16.dp))
            BooksRow(
                Icons.Outlined.People,
                Bn.people,
                if (position.receivableMinor > 0) "পাওনা ${Money.format(position.receivableMinor)}" else "দেনা-পাওনা দেখুন",
                onOpenPeople,
            )
            DividerLine(Modifier.padding(horizontal = 16.dp))
            BooksRow(Icons.Outlined.Category, Bn.categories, "নিজের তৈরি ক্যাটাগরি", onOpenCategories)
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = Bn.planningGroup)
        HisaabCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            BooksRow(
                Icons.Outlined.AccountBalance,
                Bn.loansTitle,
                if (snapshot.loans.any { !it.loan.isClosed }) "ঋণ চলছে" else "কোনো সক্রিয় ঋণ নেই",
                onOpenLoans,
            )
            DividerLine(Modifier.padding(horizontal = 16.dp))
            BooksRow(
                Icons.Outlined.CreditCard,
                Bn.emiPurchase,
                if (snapshot.emis.any { !it.emi.isSettled }) "কিস্তি চলছে" else "কোনো কিস্তি নেই",
                onOpenEmi,
            )
            DividerLine(Modifier.padding(horizontal = 16.dp))
            BooksRow(
                Icons.Outlined.Savings,
                Bn.savings,
                if (snapshot.savings.isEmpty()) "লক্ষ্য ঠিক করুন" else "${snapshot.savings.size}টি পরিকল্পনা",
                onOpenSavings,
            )
        }

        if (snapshot.savings.any { it.plan.targetAmountMinor != null }) {
            Spacer(Modifier.height(16.dp))
            SectionHeader(title = Bn.goalProgress)
            snapshot.savings.filter { it.plan.targetAmountMinor != null }.take(3).forEach { goal ->
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.plan.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text("${goal.progressPercent}%", style = MaterialTheme.typography.labelMedium, color = Emerald)
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressTrack(progress = goal.progressPercent / 100f)
                    Spacer(Modifier.height(4.dp))
                    AmountText(minor = goal.balanceMinor, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun MiniStat(label: String, value: Long, modifier: Modifier = Modifier, color: Color = TextPrimary) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        AmountText(minor = value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun BooksRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}
