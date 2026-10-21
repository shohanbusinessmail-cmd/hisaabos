package com.shohankhan.hisaabos.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.ui.components.BrandLockup
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Honest PIN recovery. The app cannot decrypt the PIN, so the only options are
 * resetting the local database (after which a backup can be restored) or going back.
 */
@Composable
fun RecoveryScreen(
    onCancel: () -> Unit,
    onWipeAndRestart: () -> Unit,
    onWipe: suspend () -> Unit,
) {
    var askWipe by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        BrandLockup(size = 68.dp, showTagline = false)
        Spacer(Modifier.height(24.dp))
        Text(Bn.forgotPinTitle, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(10.dp))
        Text(Bn.forgotPinBody, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(18.dp))
        HisaabCard {
            Text(Bn.pinWarning, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "ব্যাকআপ ফাইল থাকলে নতুন পিন দিয়ে শুরু করে সেটি পুনরুদ্ধার করতে পারবেন (আরও → ব্যাকআপ)।",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(16.dp))
        InfoNote(
            text = "নিরাপত্তার কারণে পিন উদ্ধার করার কোনো গোপন উপায় নেই — কোনো ব্যাকডোর নেই।",
            tone = NoteTone.WARNING,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = Bn.resetAllData,
            icon = Icons.Outlined.DeleteForever,
            onClick = { askWipe = true },
        )
        Spacer(Modifier.height(10.dp))
        SecondaryButton(text = Bn.cancel, onClick = onCancel)
    }

    if (askWipe) {
        AlertDialog(
            onDismissRequest = { askWipe = false },
            title = { Text(Bn.resetWarning) },
            text = {
                Column {
                    Text(
                        "ফোনে থাকা সব হিসাব, লেনদেন ও ছবি-তথ্য মুছে যাবে। এ কাজ ফেরানো যাবে না। চালিয়ে যেতে \"মুছুন\" লিখুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed,
                    )
                    Spacer(Modifier.height(8.dp))
                    com.shohankhan.hisaabos.ui.components.HisaabTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        label = "মুছুন লিখুন",
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (confirmText.trim() == "মুছুন") {
                        askWipe = false
                        scope.launch {
                            onWipe()
                            onWipeAndRestart()
                        }
                    }
                }) { Text(Bn.delete, color = ExpenseRed) }
            },
            dismissButton = { TextButton(onClick = { askWipe = false }) { Text(Bn.cancel) } },
        )
    }
}
