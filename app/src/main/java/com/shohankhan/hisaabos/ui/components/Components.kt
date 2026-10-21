package com.shohankhan.hisaabos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.CardSurface
import com.shohankhan.hisaabos.ui.theme.Divider
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.EmeraldSoft
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.ExpenseRedSoft
import com.shohankhan.hisaabos.ui.theme.NeutralSoft
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import com.shohankhan.hisaabos.ui.theme.WarningAmber
import com.shohankhan.hisaabos.ui.theme.WarningAmberSoft
import java.time.Instant
import java.time.ZoneOffset

val LocalHideAmounts = staticCompositionLocalOf { false }
val LocalBanglaDigits = staticCompositionLocalOf { true }

@Composable
fun HideAmountsProvider(hidden: Boolean, banglaDigits: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHideAmounts provides hidden, LocalBanglaDigits provides banglaDigits) {
        content()
    }
}

fun txIcon(type: TxType): ImageVector = when (type) {
    TxType.INCOME -> Icons.AutoMirrored.Outlined.TrendingUp
    TxType.EXPENSE -> Icons.Outlined.ShoppingBag
    TxType.TRANSFER -> Icons.Outlined.SwapHoriz
    TxType.CREDIT_PURCHASE -> Icons.Outlined.ShoppingCart
    TxType.SHOP_PAYMENT -> Icons.Outlined.Payments
    TxType.DEBT_BORROWED -> Icons.Outlined.CallReceived
    TxType.DEBT_REPAID -> Icons.Outlined.ArrowUpward
    TxType.LENDING -> Icons.Outlined.CallMade
    TxType.LOAN_COLLECTION -> Icons.Outlined.ArrowDownward
    TxType.LOAN_DISBURSEMENT -> Icons.Outlined.AccountBalance
    TxType.LOAN_REPAYMENT -> Icons.Outlined.Payments
    TxType.EMI_PURCHASE -> Icons.Outlined.CreditCard
    TxType.EMI_PAYMENT -> Icons.Outlined.CreditCard
    TxType.SAVINGS_DEPOSIT -> Icons.Outlined.Savings
    TxType.SAVINGS_WITHDRAWAL -> Icons.Outlined.Savings
    TxType.OPENING_BALANCE -> Icons.Outlined.AccountBalanceWallet
    TxType.ADJUSTMENT -> Icons.Outlined.Tune
    TxType.REFUND -> Icons.Outlined.Receipt
}

/** Money text: respects the global hide-amounts switch and Bangla digit preference. */
@Composable
fun AmountText(
    minor: Long,
    modifier: Modifier = Modifier,
    signed: Boolean = false,
    hiddenReplacement: String = "৳ ••••",
    color: Color = Color.Unspecified,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = 1,
) {
    val hidden = LocalHideAmounts.current
    val bangla = LocalBanglaDigits.current
    val text = when {
        hidden -> hiddenReplacement
        signed -> Money.formatSigned(minor, bangla)
        else -> Money.format(minor, bangla)
    }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = Bn.back, tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        actions()
    }
}

@Composable
fun HisaabCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = CardSurface,
    borderColor: Color = Divider,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(containerColor)
        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(contentPadding)
    Column(base, content = content)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = Emerald)
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Emerald,
            contentColor = Color.White,
            disabledContainerColor = Emerald.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.8f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    danger: Boolean = false,
) {
    val color = if (danger) ExpenseRed else EmeraldDeep
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color = Emerald,
    background: Color = EmeraldSoft,
    size: Int = 40,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 2.8).dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.5).dp))
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon = icon, size = 56)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSoft, contentColor = EmeraldDeep),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier, tone: NoteTone = NoteTone.INFO) {
    val (bg, fg, icon) = when (tone) {
        NoteTone.INFO -> Triple(EmeraldSoft, EmeraldDeep, Icons.Outlined.Info)
        NoteTone.WARNING -> Triple(WarningAmberSoft, WarningAmber, Icons.Outlined.ErrorOutline)
        NoteTone.ERROR -> Triple(ExpenseRedSoft, ExpenseRed, Icons.Outlined.ErrorOutline)
        NoteTone.SUCCESS -> Triple(EmeraldSoft, EmeraldDeep, Icons.Outlined.CheckCircle)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = fg, modifier = Modifier.weight(1f))
    }
}

enum class NoteTone { INFO, WARNING, ERROR, SUCCESS }

@Composable
fun HisaabTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    isError: Boolean = false,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it, color = TextSecondary) } },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            shape = RoundedCornerShape(14.dp),
            trailingIcon = trailing,
            supportingText = supportingText?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        )
    }
}

/** Amount field with a fixed ৳ prefix and decimal keypad. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = Bn.howMuch,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() || it == '.' || (it in '০'..'৯') }.take(14)
                onValueChange(filtered)
            },
            label = { Text(label) },
            leadingIcon = { Text("৳", style = MaterialTheme.typography.titleLarge, color = Emerald) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(14.dp),
            supportingText = supportingText?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "নির্বাচন করুন",
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = TextSecondary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text(Bn.none) }, onClick = { expanded = false })
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = BizDate.formatBn(value),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextSecondary) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        shape = RoundedCornerShape(14.dp),
    )
    if (showPicker) {
        val initial = BizDate.parse(value)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onSelect(date.toString())
                    }
                    showPicker = false
                }) { Text(Bn.confirm) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(Bn.cancel) } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Emerald,
    background: Color = EmeraldSoft,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon = icon, tint = tint, background = background, size = 46)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SimpleRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Divider))
}

@Composable
fun AmountVisibilityToggle(hidden: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (hidden) Bn.showAmounts else Bn.hideAmounts,
            tint = TextSecondary,
        )
    }
}

@Composable
fun ProgressTrack(progress: Float, modifier: Modifier = Modifier, color: Color = Emerald, track: Color = Divider) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun Pill(text: String, color: Color = Emerald, background: Color = EmeraldSoft, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LoadingScreen(message: String = Bn.loading) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Emerald)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    background: Color = NeutralSoft,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 2)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun CloseIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Close, contentDescription = Bn.close, tint = TextSecondary)
    }
}

/** A compact tappable row used for accounts, categories and nav entries. */
@Composable
fun SelectableRow(
    selected: Boolean,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) EmeraldSoft else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (selected) EmeraldDeep else TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (selected) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Emerald, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun WalletIcon() = Icon(Icons.Outlined.Wallet, contentDescription = null)

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Emerald else CardSurface,
        border = BorderStroke(1.dp, if (selected) Emerald else Divider),
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun IconPickerButton(icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SmallLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
}

@Composable
fun ColumnSpacer(height: Int = 8) = Spacer(Modifier.height(height.dp))

@Composable
fun FlowRowWrap(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
fun MoneyDisplayInline(minor: Long, color: Color = TextPrimary) {
    AmountText(minor = minor, color = color, style = MaterialTheme.typography.titleMedium)
}
