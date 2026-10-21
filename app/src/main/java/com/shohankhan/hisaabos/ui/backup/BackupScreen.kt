package com.shohankhan.hisaabos.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.hisaabos.backup.BackupCodec
import com.shohankhan.hisaabos.backup.BackupDecodeResult
import com.shohankhan.hisaabos.backup.BackupEnvelope
import com.shohankhan.hisaabos.backup.RestoreMode
import com.shohankhan.hisaabos.backup.RestoreResult
import com.shohankhan.hisaabos.di.LocalAppContainer
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.KeyValueRow
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@Composable
fun BackupScreen(onBack: () -> Unit, onWiped: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var encrypted by remember { mutableStateOf(true) }
    var passphrase by remember { mutableStateOf("") }
    var pendingEnvelope by remember { mutableStateOf<BackupEnvelope?>(null) }
    var pendingText by remember { mutableStateOf<String?>(null) }
    var askPassphrase by remember { mutableStateOf(false) }
    var askMode by remember { mutableStateOf(false) }
    var askWipe by remember { mutableStateOf(false) }
    var counts by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            error = null
            runCatching {
                val json = container.backupService.export(if (encrypted) passphrase.toCharArray() else null)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    ?: error("ফাইল লেখা যায়নি")
                container.preferences.markExportDone(System.currentTimeMillis())
            }.onSuccess {
                message = if (encrypted) "এনক্রিপ্ট করা ব্যাকআপ সংরক্ষণ হয়েছে" else "ব্যাকআপ সংরক্ষণ হয়েছে"
            }.onFailure {
                error = "ব্যাকআপ তৈরি করা যায়নি"
            }
            busy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            error = null
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { text ->
                if (text.isNullOrBlank()) {
                    error = "ফাইলটি খালি"
                    return@onSuccess
                }
                when (val result = BackupCodec.decode(text, null)) {
                    is BackupDecodeResult.Success -> {
                        pendingEnvelope = result.envelope
                        askMode = true
                    }
                    is BackupDecodeResult.Error -> {
                        pendingText = text
                        askPassphrase = true
                    }
                    BackupDecodeResult.NeedsPassphrase -> {
                        pendingText = text
                        askPassphrase = true
                    }
                }
            }.onFailure { error = "ফাইল পড়া যায়নি" }
        }
    }

    fun restore(envelope: BackupEnvelope, mode: RestoreMode) {
        scope.launch {
            busy = true
            when (val result = container.backupService.restore(envelope, mode)) {
                is RestoreResult.Success -> {
                    counts = result.imported.entries.joinToString(" · ") { "${it.key}=${it.value}" }.ifBlank { "কোনো সারি নেই" }
                    message = "পুনরুদ্ধার সম্পন্ন"
                }
                is RestoreResult.Invalid -> error = result.message
                is RestoreResult.Failed -> error = result.message
            }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(title = Bn.backupTitle, subtitle = "শুধু আপনার ডিভাইসে", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            HisaabCard {
                Text("শেষ ব্যাকআপ", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(
                    settings?.lastBackupAtMillis?.takeIf { it > 0 }
                        ?.let { BizDate.formatBn(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()) }
                        ?: "কখনো ব্যাকআপ নেওয়া হয়নি",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "হিসাবওএস-এ কোনো ক্লাউড নেই। ফাইলটি আপনি নিজে নিরাপদ জায়গায় রাখবেন — ফোন হারালে বা অ্যাপ মুছে গেলে এটিই একমাত্র উদ্ধারপথ।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.height(14.dp))
            SectionHeader(title = Bn.exportData)
            InfoNote(text = Bn.backupPlainWarning, tone = NoteTone.WARNING)
            Spacer(Modifier.height(8.dp))
            HisaabCard {
                Row {
                    com.shohankhan.hisaabos.ui.components.CategoryChip("প্লেইন JSON", !encrypted) { encrypted = false }
                    Spacer(Modifier.height(0.dp))
                    com.shohankhan.hisaabos.ui.components.CategoryChip("পাসফ্রেজ দিয়ে এনক্রিপ্ট", encrypted) { encrypted = true }
                }
                if (encrypted) {
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = Bn.backupPassphrase,
                        placeholder = Bn.backupPassphraseHint,
                    )
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    text = Bn.createBackup,
                    icon = if (encrypted) Icons.Outlined.Lock else Icons.Outlined.Download,
                    enabled = !busy && (!encrypted || passphrase.length >= 8),
                    loading = busy,
                    onClick = { exportLauncher.launch("hisaabos-backup-${BizDate.today()}.json") },
                )
                if (encrypted && passphrase.isNotEmpty() && passphrase.length < 8) {
                    Spacer(Modifier.height(6.dp))
                    Text("পাসফ্রেজ অন্তত ৮ অক্ষরের দিন।", style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionHeader(title = Bn.restoreBackup)
            SecondaryButton(
                text = Bn.restoreFromBackup,
                icon = Icons.Outlined.Upload,
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            )
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                InfoNote(text = message!!, tone = NoteTone.SUCCESS)
            }
            if (counts != null) {
                Spacer(Modifier.height(8.dp))
                HisaabCard {
                    Text("পুনরুদ্ধার করা সারি", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(counts!!, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                InfoNote(text = error!!, tone = NoteTone.ERROR)
            }

            Spacer(Modifier.height(14.dp))
            SectionHeader(title = Bn.backupWarning)
            Text(
                "পুনরুদ্ধার দুইভাবে হতে পারে — সব তথ্য প্রতিস্থাপন (বর্তমান সব মুছে নতুনটি বসবে) বা একত্র করা (নতুন রেকর্ড যোগ হবে)। দুটোই একটি ডেটাবেস ট্রানজেকশনে হয়; ব্যর্থ হলে আগের তথ্য অক্ষত থাকে।",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Spacer(Modifier.height(16.dp))
            SecondaryButton(
                text = Bn.resetAllData,
                icon = Icons.Outlined.DeleteForever,
                danger = true,
                onClick = { askWipe = true },
            )
            Spacer(Modifier.height(90.dp))
        }
    }

    if (askPassphrase) {
        var pass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askPassphrase = false },
            title = { Text(Bn.backupEncrypt) },
            text = {
                Column {
                    Text("এই ব্যাকআপ পাসফ্রেজ দিয়ে সুরক্ষিত। পাসফ্রেজটি লিখুন।", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = pass, onValueChange = { pass = it }, label = Bn.backupPassphrase)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val text = pendingText
                    if (text != null) {
                        when (val result = BackupCodec.decode(text, pass.toCharArray())) {
                            is BackupDecodeResult.Success -> {
                                pendingEnvelope = result.envelope
                                askPassphrase = false
                                askMode = true
                            }
                            is BackupDecodeResult.Error -> error = result.message
                            BackupDecodeResult.NeedsPassphrase -> error = "পাসফ্রেজ দিন"
                        }
                    }
                }) { Text(Bn.confirm, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { askPassphrase = false }) { Text(Bn.cancel) } },
        )
    }

    if (askMode) {
        AlertDialog(
            onDismissRequest = { askMode = false },
            title = { Text(Bn.restoreModeTitle) },
            text = {
                Column {
                    Text(Bn.restoreReplaceBody, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(text = Bn.restoreReplace, onClick = {
                        pendingEnvelope?.let { restore(it, RestoreMode.REPLACE_ALL) }
                        askMode = false
                    })
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(text = Bn.restoreMerge, onClick = {
                        pendingEnvelope?.let { restore(it, RestoreMode.MERGE) }
                        askMode = false
                    })
                }
            },
            confirmButton = { TextButton(onClick = { askMode = false }) { Text(Bn.cancel) } },
        )
    }

    if (askWipe) {
        var confirmText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askWipe = false },
            title = { Text(Bn.resetAllData) },
            text = {
                Column {
                    InfoNote(text = Bn.resetWarning, tone = NoteTone.ERROR)
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = confirmText, onValueChange = { confirmText = it }, label = "নিশ্চিত করতে \"মুছুন\" লিখুন")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (confirmText == "মুছুন") {
                        scope.launch {
                            container.repository.wipeEverything()
                            container.preferences.completeOnboarding()
                            askWipe = false
                            onWiped()
                        }
                    }
                }) { Text(Bn.delete, color = ExpenseRed) }
            },
            dismissButton = { TextButton(onClick = { askWipe = false }) { Text(Bn.cancel) } },
        )
    }
}
