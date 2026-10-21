package com.shohankhan.hisaabos.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun PeopleScreen(onBack: () -> Unit, onOpenPerson: (Long) -> Unit) {
    val vm = appViewModel { PeopleViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.people,
            subtitle = "ব্যক্তিগত ধার ও পাওনা",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newPerson, tint = Emerald)
                }
            },
        )
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                HisaabCard {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("আমার পাওনা", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            AmountText(minor = state.totalReceivableMinor, style = MaterialTheme.typography.titleMedium, color = EmeraldDeep)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("আমার দেনা", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            AmountText(minor = state.totalPayableMinor, style = MaterialTheme.typography.titleMedium, color = ExpenseRed)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "ধার নেওয়া আয় নয়, আর ধার শোধ করা খরচ নয় — শুধু দায় ও পাওনা।",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SectionHeader(title = Bn.personLedger)
            }
            if (state.rows.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.People,
                        title = Bn.noPeople,
                        message = "যাঁর সাথে টাকা লেনদেন করেন, তাঁকে যোগ করুন। প্রতিটি ধার আলাদা হিসাবে থাকবে।",
                        actionLabel = Bn.newPerson,
                        onAction = { showAdd = true },
                    )
                }
            }
            items(state.rows, key = { it.person.id }) { row ->
                HisaabCard(
                    modifier = Modifier.padding(bottom = 8.dp).clickable { onOpenPerson(row.person.id) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Outlined.People, size = 40)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(row.person.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                if (row.openDebts == 0) "খোলা ধার নেই" else "${row.openDebts}টি খোলা ধার",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (row.receivableMinor > 0) {
                                Text("পাওনা", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                AmountText(minor = row.receivableMinor, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                            }
                            if (row.payableMinor > 0) {
                                Text("দেনা", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                AmountText(minor = row.payableMinor, style = MaterialTheme.typography.titleSmall, color = ExpenseRed)
                            }
                            if (row.receivableMinor == 0L && row.payableMinor == 0L) {
                                Text("মিটে গেছে", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var createdId by remember { mutableStateOf<Long?>(null) }
        androidx.compose.runtime.LaunchedEffect(createdId) {
            createdId?.let {
                showAdd = false
                onOpenPerson(it)
                createdId = null
            }
        }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(Bn.newPerson) },
            text = {
                Column {
                    HisaabTextField(value = name, onValueChange = { name = it }, label = Bn.borrowerName)
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = phone, onValueChange = { phone = it }, label = Bn.phone, placeholder = Bn.optional)
                    if (state.error != null) {
                        Spacer(Modifier.height(8.dp))
                        InfoNote(text = state.error!!, tone = NoteTone.ERROR)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createPerson(name, phone) { id -> createdId = id }
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(Bn.cancel) } },
        )
    }
}
