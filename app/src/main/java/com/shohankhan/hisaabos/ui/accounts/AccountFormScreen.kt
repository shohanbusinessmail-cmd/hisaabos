package com.shohankhan.hisaabos.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.CardSurface
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun AccountFormScreen(
    existing: AccountEntity?,
    error: String?,
    onBack: () -> Unit,
    onSave: (String, AccountType, String, String, String, Boolean) -> Unit,
    onArchiveToggle: ((Boolean) -> Unit)? = null,
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember(existing) { mutableStateOf(existing?.type ?: AccountType.CASH) }
    var institution by remember(existing) { mutableStateOf(existing?.institution.orEmpty()) }
    var opening by remember(existing) { mutableStateOf("") }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var liability by remember(existing) { mutableStateOf(existing?.isLiability ?: false) }

    Column(Modifier.fillMaxSize().background(AppBackground).imePadding()) {
        PageHeader(
            title = if (existing == null) Bn.newAccount else "হিসাব সম্পাদনা",
            onBack = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            HisaabTextField(value = name, onValueChange = { name = it }, label = Bn.accountName, placeholder = "যেমন: নগদ, ডাচ-বাংলা")
            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = Bn.accountType,
                options = AccountType.entries,
                selected = type,
                optionLabel = { Bn.accountType(it) },
                onSelect = { type = it; liability = it == AccountType.CREDIT_CARD },
            )
            Spacer(Modifier.height(12.dp))
            HisaabTextField(value = institution, onValueChange = { institution = it }, label = "${Bn.institution} (${Bn.optional})")
            if (existing == null) {
                Spacer(Modifier.height(12.dp))
                AmountField(
                    value = opening,
                    onValueChange = { opening = it },
                    label = Bn.openingBalance,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (liability) "লায়াবিলিটি হিসাবে এই টাকা ঋণ হিসেবে যোগ হবে।" else "এটি আয় নয়; শুধু হিসাবের শুরুর অবস্থা।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("এটি দায় (ঋণ) হিসাব", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text("ক্রেডিট কার্ড বা ঋণের হিসাব হলে চালু করুন", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Switch(checked = liability, onCheckedChange = { liability = it })
            }
            Spacer(Modifier.height(12.dp))
            HisaabTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "${Bn.noteOptional}",
                singleLine = false,
                maxLines = 3,
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                InfoNote(text = error, tone = NoteTone.ERROR)
            }
            if (existing != null && onArchiveToggle != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { onArchiveToggle(!existing.isArchived) }) {
                    Text(
                        if (existing.isArchived) Bn.unarchive else Bn.archive,
                        color = TextSecondary,
                    )
                }
                InfoNote(
                    text = "আর্কাইভ করলে হিসাবটি নতুন লেনদেনে দেখাবে না, তবে পুরনো লেনদেন ও ব্যালেন্স অক্ষত থাকবে।",
                )
            }
            Spacer(Modifier.height(20.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            PrimaryButton(
                text = Bn.save,
                enabled = name.isNotBlank(),
                onClick = { onSave(name, type, institution, opening, notes, liability) },
            )
        }
    }
}
