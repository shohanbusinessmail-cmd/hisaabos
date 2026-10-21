package com.shohankhan.hisaabos.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.hisaabos.data.prefs.AppSettings
import com.shohankhan.hisaabos.security.BiometricGate
import com.shohankhan.hisaabos.ui.app.AppViewModel
import com.shohankhan.hisaabos.ui.components.BrandLockup
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PinDots
import com.shohankhan.hisaabos.ui.components.PinPad
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import com.shohankhan.hisaabos.ui.text.Bn

@Composable
fun LockScreen(
    viewModel: AppViewModel,
    settings: AppSettings,
    onForgotPin: () -> Unit,
) {
    val state by viewModel.pinState.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var showForgot by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = remember { BiometricGate.isAvailable(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        BrandLockup(size = 68.dp, showTagline = false)
        Spacer(Modifier.height(28.dp))
        Text(Bn.enterPin, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(18.dp))
        PinDots(length = pin.length, count = maxOf(4, pin.length))
        Spacer(Modifier.height(14.dp))
        if (state.lockoutSeconds > 0) {
            InfoNote(
                text = "অনেকবার ভুল পিন। আবার চেষ্টা করুন ${state.lockoutSeconds} সেকেন্ড পর।",
                tone = NoteTone.WARNING,
            )
            Spacer(Modifier.height(10.dp))
        } else if (state.error != null) {
            Text(state.error!!, color = ExpenseRed, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(6.dp))
        Column(modifier = Modifier.widthIn(max = 360.dp)) {
            PinPad(
                enabled = state.lockoutSeconds == 0 && !state.busy,
                onDigit = { digit ->
                    viewModel.clearPinError()
                    if (pin.length < 8) pin += digit
                },
                onBackspace = {
                    viewModel.clearPinError()
                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                },
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = Bn.unlock,
                enabled = pin.length >= 4 && state.lockoutSeconds == 0 && !state.busy,
                loading = state.busy,
                onClick = { viewModel.unlockWithPin(pin) },
            )
            if (settings.biometricEnabled && biometricAvailable && activity != null) {
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        viewModel.lockNow()
                        BiometricGate.showPrompt(
                            activity = activity,
                            onSuccess = { viewModel.unlockWithBiometricSuccess() },
                            onFailure = { },
                        )
                    },
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = Emerald)
                    Spacer(Modifier.width(8.dp))
                    Text(Bn.useBiometric, color = Emerald)
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { showForgot = true }) {
                Text(Bn.forgotPin, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "আপনার তথ্য শুধু এই ফোনেই আছে",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showForgot) {
        AlertDialog(
            onDismissRequest = { showForgot = false },
            title = { Text(Bn.forgotPinTitle, fontWeight = FontWeight.SemiBold) },
            text = { Text(Bn.forgotPinBody) },
            confirmButton = {
                TextButton(onClick = {
                    showForgot = false
                    onForgotPin()
                }) { Text(Bn.restoreFromBackup, color = Emerald) }
            },
            dismissButton = {
                TextButton(onClick = { showForgot = false }) { Text(Bn.cancel) }
            },
        )
    }
}
