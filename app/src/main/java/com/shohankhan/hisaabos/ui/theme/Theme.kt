package com.shohankhan.hisaabos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shohankhan.hisaabos.R

// ---------------------------------------------------------------- color tokens
// Contrast-checked against #FFFFFF and #F7FAF8 surfaces.
val Emerald = Color(0xFF087443)
val EmeraldDeep = Color(0xFF065A35)
val EmeraldSoft = Color(0xFFE8F6ED)
val EmeraldInk = Color(0xFF0B4A2E)
val AppBackground = Color(0xFFF7FAF8)
val CardSurface = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF17231C)
val TextSecondary = Color(0xFF68766E)
val Divider = Color(0xFFE5ECE7)
val ExpenseRed = Color(0xFFB3261E)
val ExpenseRedSoft = Color(0xFFFDECEA)
val WarningAmber = Color(0xFF8A5A00)
val WarningAmberSoft = Color(0xFFFFF4DC)
val InfoBlue = Color(0xFF3A5A78)
val InfoBlueSoft = Color(0xFFEAF1F7)
val NeutralSoft = Color(0xFFF1F4F2)

private val HisaabLightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldInk,
    secondary = EmeraldDeep,
    onSecondary = Color.White,
    secondaryContainer = EmeraldSoft,
    onSecondaryContainer = EmeraldInk,
    tertiary = InfoBlue,
    onTertiary = Color.White,
    tertiaryContainer = InfoBlueSoft,
    onTertiaryContainer = InfoBlue,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = NeutralSoft,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    outlineVariant = Divider,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedSoft,
    onErrorContainer = ExpenseRed,
    scrim = Color(0x66000000),
)

/** Hind Siliguri (SIL Open Font License 1.1) — bundled so Bangla conjuncts render identically on every device. */
val HindSiliguri = FontFamily(
    Font(R.font.hind_siliguri_regular, FontWeight.Normal),
    Font(R.font.hind_siliguri_medium, FontWeight.Medium),
    Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Bold),
)

private fun base(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = HindSiliguri,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

val HisaabTypography = Typography(
    displayLarge = base(34, FontWeight.Bold, 44),
    displayMedium = base(30, FontWeight.Bold, 40),
    displaySmall = base(26, FontWeight.SemiBold, 34),
    headlineLarge = base(24, FontWeight.SemiBold, 32),
    headlineMedium = base(22, FontWeight.SemiBold, 30),
    headlineSmall = base(20, FontWeight.SemiBold, 28),
    titleLarge = base(18, FontWeight.SemiBold, 26),
    titleMedium = base(16, FontWeight.SemiBold, 24),
    titleSmall = base(15, FontWeight.Medium, 22),
    bodyLarge = base(16, FontWeight.Normal, 26),
    bodyMedium = base(14, FontWeight.Normal, 23),
    bodySmall = base(13, FontWeight.Normal, 20),
    labelLarge = base(15, FontWeight.SemiBold, 22),
    labelMedium = base(13, FontWeight.Medium, 18),
    labelSmall = base(11, FontWeight.Medium, 16),
)

/** Tabular-ish style for aligned monetary figures. */
val MoneyStyle = base(28, FontWeight.Bold, 36)

@Composable
fun HisaabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HisaabLightColors,
        typography = HisaabTypography,
        shapes = HisaabShapes,
        content = content,
    )
}
