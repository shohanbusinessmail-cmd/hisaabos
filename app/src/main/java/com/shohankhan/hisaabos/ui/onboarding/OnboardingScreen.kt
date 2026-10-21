package com.shohankhan.hisaabos.ui.onboarding

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.TxDraft
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.ui.app.AppViewModel
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.BrandLockup
import com.shohankhan.hisaabos.ui.components.BrandMark
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PinDots
import com.shohankhan.hisaabos.ui.components.PinPad
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Divider
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldSoft
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import kotlinx.coroutines.launch

class OnboardingViewModel(private val repository: HisaabRepository) : ViewModel() {

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun createOpeningAccount(name: String, amountText: String, onDone: () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            val amount = if (amountText.isBlank()) null else Money.parse(amountText)
            if (amountText.isNotBlank() && (amount == null || amount < 0)) {
                error = "সঠিক টাকার পরিমাণ লিখুন"
                busy = false
                return@launch
            }
            try {
                val accountId = repository.createAccount(
                    name = name.ifBlank { "নগদ" },
                    type = AccountType.CASH,
                )
                if (amount != null && amount > 0) {
                    val result = repository.service.post(
                        TxDraft(
                            type = TxType.OPENING_BALANCE,
                            amountMinor = amount,
                            accountId = accountId,
                            title = "শুরুর ব্যালেন্স",
                        )
                    )
                    if (result !is WriteResult.Success) {
                        error = Bn.errorGeneric
                        busy = false
                        return@launch
                    }
                }
                busy = false
                onDone()
            } catch (t: Throwable) {
                error = Bn.errorGeneric
                busy = false
            }
        }
    }

    fun skip(onDone: () -> Unit) = onDone()
}

@Composable
fun OnboardingScreen(onboardingViewModel: OnboardingViewModel, appViewModel: AppViewModel, onFinished: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinStage by remember { mutableIntStateOf(0) } // 0 = create, 1 = confirm
    var pinError by remember { mutableStateOf<String?>(null) }
    var accountName by remember { mutableStateOf("নগদ") }
    var openingAmount by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepIndicator(step = step, total = 4)
        Spacer(Modifier.height(24.dp))

        when (step) {
            0 -> WelcomeStep(onStart = { step = 1 })
            1 -> PinStep(
                pin = pin,
                confirmPin = confirmPin,
                stage = pinStage,
                error = pinError,
                busy = onboardingViewModel.busy,
                onDigit = { digit ->
                    pinError = null
                    if (pinStage == 0) {
                        if (pin.length < 8) pin += digit
                    } else if (confirmPin.length < 8) confirmPin += digit
                },
                onBackspace = {
                    pinError = null
                    if (pinStage == 0) {
                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    } else if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                },
                onContinue = {
                    when {
                        pin.length < 4 -> pinError = "অন্তত ৪ সংখ্যার পিন দিন"
                        pinStage == 0 -> pinStage = 1
                        confirmPin != pin -> {
                            pinError = Bn.pinMismatch
                            confirmPin = ""
                        }
                        else -> appViewModel.setPin(pin) { step = 2 }
                    }
                },
            )
            2 -> OpeningStep(
                accountName = accountName,
                onAccountNameChange = { accountName = it },
                amount = openingAmount,
                onAmountChange = { openingAmount = it },
                busy = onboardingViewModel.busy,
                error = localError ?: onboardingViewModel.error,
                onSkip = { step = 3 },
                onContinue = {
                    val parsed = if (openingAmount.isBlank()) 0L else Money.parse(openingAmount)
                    if (openingAmount.isNotBlank() && (parsed == null || parsed < 0)) {
                        localError = "সঠিক টাকার পরিমাণ লিখুন"
                    } else {
                        onboardingViewModel.createOpeningAccount(accountName, openingAmount) { step = 3 }
                    }
                },
            )
            else -> TourStep(
                onFinish = {
                    onboardingViewModel.skip {
                        appViewModel.completeOnboarding(onFinished)
                    }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        if (step == 2) {
            TextButton(onClick = { step = 3 }) { Text(Bn.skipForNow, color = TextSecondary) }
        }
        if (step > 0 && step < 3) {
            TextButton(onClick = { if (step == 2) step = 1 else step -= 1 }) { Text(Bn.back, color = TextSecondary) }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(if (index <= step) Emerald else Divider, CircleShape),
            )
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        BrandLockup(size = 92.dp)
        Spacer(Modifier.height(28.dp))
        InfoNote(text = "সম্পূর্ণ অফলাইন — কোনো ইন্টারনেট, রেজিস্ট্রেশন বা অ্যাকাউন্ট লাগবে না")
        Spacer(Modifier.height(12.dp))
        InfoNote(text = "আপনার সব হিসাব শুধু এই ফোনেই সংরক্ষিত থাকে", tone = NoteTone.INFO)
        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = Bn.start, onClick = onStart)
    }
}

@Composable
private fun PinStep(
    pin: String,
    confirmPin: String,
    stage: Int,
    error: String?,
    busy: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onContinue: () -> Unit,
) {
    val current = if (stage == 0) pin else confirmPin
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = Emerald,
            modifier = Modifier
                .size(44.dp)
                .background(EmeraldSoft, CircleShape)
                .padding(10.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            if (stage == 0) Bn.setUpPin else Bn.confirmPin,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        InfoNote(text = if (stage == 0) Bn.pinExplainer else Bn.pinWarning, tone = NoteTone.INFO)
        Spacer(Modifier.height(20.dp))
        PinDots(length = current.length, count = maxOf(4, current.length))
        Spacer(Modifier.height(10.dp))
        if (error != null) Text(error, color = com.shohankhan.hisaabos.ui.theme.ExpenseRed)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.widthIn(max = 360.dp)) {
            PinPad(onDigit = onDigit, onBackspace = onBackspace, enabled = !busy)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = if (stage == 0) Bn.continueLabel else Bn.confirm,
                onClick = onContinue,
                enabled = current.length >= 4 && !busy,
                loading = busy,
            )
        }
    }
}

@Composable
private fun OpeningStep(
    accountName: String,
    onAccountNameChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Bn.openingBalanceTitle, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            Bn.openingBalanceExplainer,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(20.dp))
        HisaabTextField(
            value = accountName,
            onValueChange = onAccountNameChange,
            label = "হিসাবের নাম",
            placeholder = "যেমন: নগদ",
        )
        Spacer(Modifier.height(12.dp))
        AmountField(value = amount, onValueChange = onAmountChange, label = "এখন হাতে কত টাকা?")
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = com.shohankhan.hisaabos.ui.theme.ExpenseRed)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = Bn.continueLabel, onClick = onContinue, loading = busy)
        Spacer(Modifier.height(6.dp))
        Text(
            "এটি আয় হিসেবে গণ্য হবে না — শুধু আপনার শুরুর অবস্থা।",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun TourStep(onFinish: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(Bn.tourTitle, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Spacer(Modifier.height(14.dp))
        Bn.tourItems.forEach { (title, body) ->
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Emerald, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(text = Bn.finish, onClick = onFinish)
    }
}
