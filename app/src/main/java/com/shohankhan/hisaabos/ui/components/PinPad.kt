package com.shohankhan.hisaabos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.ui.theme.CardSurface
import com.shohankhan.hisaabos.ui.theme.Divider
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldSoft
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

/** Six-digit indicator using filled dots; never shows the digits themselves. */
@Composable
fun PinDots(length: Int, count: Int = 4, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (index < length) Emerald else Divider),
            )
        }
    }
}

/**
 * On-screen numeric keypad. Bangla numerals are displayed but the value is kept
 * as Latin digits so the PIN hash is stable regardless of the input language.
 */
@Composable
fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "<"),
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { key ->
                    Box(modifier = Modifier.weight(1f)) {
                        when (key) {
                            "" -> Spacer(Modifier.height(60.dp))
                            "<" -> KeyButton(onClick = onBackspace, enabled = enabled) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "মুছুন",
                                    tint = TextSecondary,
                                )
                            }
                            else -> KeyButton(onClick = { onDigit(key) }, enabled = enabled) {
                                Text(
                                    Money.toBanglaDigits(key),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(onClick: () -> Unit, enabled: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) CardSurface else CardSurface.copy(alpha = 0.5f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
