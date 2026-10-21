package com.shohankhan.hisaabos.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.BuildConfig
import com.shohankhan.hisaabos.data.prefs.AppSettings
import com.shohankhan.hisaabos.ui.components.BrandLockup
import com.shohankhan.hisaabos.ui.components.BrandMark
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.components.categoryIcon
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun SecurityScreen(
    settings: AppSettings,
    biometricAvailable: Boolean,
    error: String?,
    onBack: () -> Unit,
    onChangePin: (String, String, () -> Unit) -> Unit,
    onBiometric: (Boolean) -> Unit,
    onAutoLock: (Int) -> Unit,
    onScreenProtection: (Boolean) -> Unit,
    onLockNow: () -> Unit,
) {
    var showChange by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(title = Bn.security, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionHeader(title = "প্রবেশ নিয়ন্ত্রণ")
            HisaabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("পিন", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text("৪–৮ সংখ্যার পিন দিয়ে অ্যাপ খোলে", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    TextButton(onClick = { showChange = true }) { Text(Bn.changePin, color = Emerald) }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Bn.biometric, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text(
                            if (biometricAvailable) Bn.biometricHint else Bn.biometricUnavailable,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = settings.biometricEnabled && biometricAvailable,
                        enabled = biometricAvailable,
                        onCheckedChange = onBiometric,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader(title = Bn.autoLock)
            HisaabCard {
                Text(Bn.autoLockHint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to Bn.immediately, 30 to Bn.after30s, 60 to Bn.after1m, 300 to Bn.after5m).forEach { (seconds, label) ->
                        com.shohankhan.hisaabos.ui.components.CategoryChip(label, settings.autoLockSeconds == seconds) {
                            onAutoLock(seconds)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader(title = "গোপনীয়তা")
            HisaabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Bn.screenProtection, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text("স্ক্রিনশট ও রিসেন্ট অ্যাপ প্রিভিউতে তথ্য দেখা যাবে না", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(checked = settings.screenProtection, onCheckedChange = onScreenProtection)
                }
            }
            Spacer(Modifier.height(12.dp))
            InfoNote(
                text = "পিন একটি স্থানীয় লক। এটি ফোনের ডেটাবেস এনক্রিপ্ট করে না এবং ফোনের মালিকানা বা রুট করা ডিভাইস থেকে তথ্য রক্ষা করার নিশ্চয়তা দেয় না। ব্যাকআপে পিন থাকে না, তাই পুনরুদ্ধারের পর নতুন পিন দিতে হবে।",
                tone = NoteTone.INFO,
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(text = Bn.lockNow, onClick = onLockNow, icon = Icons.Outlined.Lock)
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                InfoNote(text = error, tone = NoteTone.ERROR)
            }
            Spacer(Modifier.height(96.dp))
        }
    }

    if (showChange) {
        AlertDialog(
            onDismissRequest = { showChange = false },
            title = { Text(Bn.changePin) },
            text = {
                Column {
                    HisaabTextField(
                        value = current,
                        onValueChange = { current = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = Bn.currentPin,
                        keyboardType = KeyboardType.NumberPassword,
                    )
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = Bn.newPin,
                        keyboardType = KeyboardType.NumberPassword,
                    )
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(
                        value = confirm,
                        onValueChange = { confirm = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = Bn.confirmPin,
                        keyboardType = KeyboardType.NumberPassword,
                    )
                    if (localError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(localError!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    localError = when {
                        newPin.length < 4 -> "নতুন পিন অন্তত ৪ সংখ্যার হতে হবে"
                        newPin != confirm -> Bn.pinMismatch
                        else -> null
                    }
                    if (localError == null) {
                        onChangePin(current, newPin) {
                            showChange = false
                            current = ""
                            newPin = ""
                            confirm = ""
                        }
                    }
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showChange = false }) { Text(Bn.cancel) } },
        )
    }
}

@Composable
fun AppearanceScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onBanglaDigits: (Boolean) -> Unit,
    onHideAmounts: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(title = Bn.appearance, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            HisaabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = Emerald,
                        modifier = Modifier.width(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("শুধু লাইট মোড", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text(
                            "হিসাবওএস ইচ্ছাকৃতভাবে শুধু লাইট থিমে চলে — সব স্ক্রিন, ডায়ালগ ও চার্টে একই রকম।",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HisaabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("বাংলা সংখ্যা", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text("টাকার অঙ্ক ও তারিখ বাংলা সংখ্যায় দেখান", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(checked = settings.showBanglaDigits, onCheckedChange = onBanglaDigits)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("টাকার হিসাব লুকান", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text("চারপাশের কেউ দেখলে অঙ্ক ঢেকে থাকবে", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(checked = settings.hideAmounts, onCheckedChange = onHideAmounts)
                }
            }
            Spacer(Modifier.height(12.dp))
            InfoNote(text = "ডার্ক মোড নেই — এটি ইচ্ছাকৃত ডিজাইন সিদ্ধান্ত, যা ব্র্যান্ডের একরূপতা রক্ষা করে।")
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
fun AboutScreen(settings: AppSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(title = Bn.about, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            BrandLockup(size = 84.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                "${Bn.version} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            HisaabCard {
                Text(Bn.appDescription, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            Spacer(Modifier.height(12.dp))
            HisaabCard(containerColor = com.shohankhan.hisaabos.ui.theme.EmeraldSoft, borderColor = com.shohankhan.hisaabos.ui.theme.EmeraldSoft) {
                Text(Bn.privacyTitle, style = MaterialTheme.typography.titleSmall, color = com.shohankhan.hisaabos.ui.theme.EmeraldDeep, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(Bn.privacyBody, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            Spacer(Modifier.height(12.dp))
            HisaabCard {
                Text(Bn.licensesTitle, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(Bn.licensesBody, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            HisaabCard {
                Text("অ্যাপের মালিক", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(2.dp))
                Text("Shohan Khan", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("helloiamshohan@gmail.com", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    text = Bn.contactOwner,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:helloiamshohan@gmail.com")).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "হিসাবওএস — মতামত")
                                }
                            )
                        }
                    },
                    icon = Icons.Outlined.Email,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(Bn.madeWithCare, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text(
                "সব তথ্য আপনার ডিভাইসে। কোনো ইন্টারনেট অনুমতি নেই, কোনো ট্র্যাকিং নেই।",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
fun VerifyBooksScreen(issues: List<String>, onBack: () -> Unit, onRun: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(title = Bn.verifyBooks, onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            InfoNote(text = Bn.verifyBooksBody)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(text = "যাচাই করুন", onClick = onRun, icon = Icons.Outlined.VerifiedUser)
            Spacer(Modifier.height(12.dp))
            if (issues.isEmpty()) {
                InfoNote(text = "এখনো যাচাই করা হয়নি অথবা কোনো অমিল নেই।", tone = NoteTone.SUCCESS)
            } else {
                HisaabCard {
                    Text("${issues.size}টি সমস্যা", style = MaterialTheme.typography.titleSmall, color = ExpenseRed)
                    Spacer(Modifier.height(6.dp))
                    issues.take(20).forEach { issue ->
                        Text("• $issue", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            }
        }
    }
}
