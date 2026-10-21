package com.shohankhan.hisaabos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.data.db.TxListItem
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldSoft
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.ExpenseRedSoft
import com.shohankhan.hisaabos.ui.theme.InfoBlue
import com.shohankhan.hisaabos.ui.theme.InfoBlueSoft
import com.shohankhan.hisaabos.ui.theme.NeutralSoft
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import com.shohankhan.hisaabos.ui.theme.WarningAmber
import com.shohankhan.hisaabos.ui.theme.WarningAmberSoft

fun txDirection(type: TxType): Int = when (type) {
    TxType.INCOME, TxType.DEBT_BORROWED, TxType.LOAN_COLLECTION, TxType.LOAN_DISBURSEMENT, TxType.REFUND -> 1
    TxType.EXPENSE, TxType.SHOP_PAYMENT, TxType.DEBT_REPAID, TxType.LENDING, TxType.LOAN_REPAYMENT, TxType.EMI_PAYMENT -> -1
    else -> 0
}

@Composable
fun TxIconColors(type: TxType): Triple<Color, Color, androidx.compose.ui.graphics.vector.ImageVector> {
    val direction = txDirection(type)
    return when {
        type.isAccrualExpense -> Triple(WarningAmber, WarningAmberSoft, txIcon(type))
        direction > 0 -> Triple(Emerald, EmeraldSoft, txIcon(type))
        direction < 0 -> Triple(ExpenseRed, ExpenseRedSoft, txIcon(type))
        else -> Triple(InfoBlue, InfoBlueSoft, txIcon(type))
    }
}

@Composable
fun TxRow(
    item: TxListItem,
    showDate: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val tx = item.tx
    val (tint, background, icon) = TxIconColors(tx.type)
    val title = tx.title?.takeIf { it.isNotBlank() } ?: Bn.txType(tx.type)
    val subtitleParts = buildList {
        item.accountName?.let { add(it) }
        item.categoryName?.let { add(it) }
        if (tx.type == TxType.CREDIT_PURCHASE || tx.type == TxType.SHOP_PAYMENT) {
            item.shopName?.let { add(it) }
        }
        if (tx.personId != null) item.personName?.let { add(it) }
        if (showDate) add(BizDate.relativeBn(tx.businessDate))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon = icon, tint = tint, background = background, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitleParts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        val direction = txDirection(tx.type)
        val color = when {
            tx.type.isAccrualExpense -> WarningAmber
            direction > 0 -> Emerald
            direction < 0 -> ExpenseRed
            else -> TextSecondary
        }
        val displayMinor = when (tx.type) {
            TxType.EMI_PURCHASE, TxType.CREDIT_PURCHASE -> tx.amountMinor
            else -> tx.amountMinor
        }
        AmountText(
            minor = if (direction < 0) -displayMinor else displayMinor,
            signed = direction != 0,
            color = color,
            style = MaterialTheme.typography.titleSmall,
        )
        if (tx.type.isAccrualExpense) {
            Spacer(Modifier.width(6.dp))
            Pill(text = "বাকি", color = WarningAmber, background = WarningAmberSoft)
        }
    }
}

@Composable
fun TxRowCompact(item: TxListItem, onClick: (() -> Unit)? = null) = TxRow(item = item, showDate = true, onClick = onClick)
