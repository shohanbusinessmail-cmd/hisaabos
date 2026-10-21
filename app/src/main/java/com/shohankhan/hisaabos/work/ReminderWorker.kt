package com.shohankhan.hisaabos.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shohankhan.hisaabos.di.AppContainer
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.RecurrenceFrequency
import com.shohankhan.hisaabos.domain.model.ScheduleOwner
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.notifications.Notifications
import com.shohankhan.hisaabos.ui.text.Bn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Daily, fully local check. It never writes transactions — it only surfaces
 * reminders the user can act on inside the app. This keeps the ledger honest:
 * no automatic payments and no invented entries.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container = AppContainer(applicationContext)
        val today = LocalDate.now()
        val horizon = today.plusDays(2).toString()
        val reminders = container.repository.openReminders()
            .filter { it.dueDate <= horizon }
        reminders.take(20).forEach { reminder ->
            val dueLabel = BizDate.formatBn(reminder.dueDate)
            Notifications.post(
                applicationContext,
                reminder.id.toInt(),
                "${Bn.reminders}: ${reminder.title}",
                reminder.amountMinor?.let { "পরিমাণ ${Money.format(it)} · তারিখ $dueLabel" } ?: "তারিখ $dueLabel",
            )
        }

        // Loan and EMI installments due in the next 2 days.
        container.repository.openLoans().forEach { loan ->
            val schedule = container.repository.schedule(ScheduleOwner.LOAN, loan.id)
            val paid = container.repository.forLink(LinkKind.LOAN, loan.id).mapNotNull { it.installmentIndex }.toSet()
            schedule.firstOrNull { it.sequence !in paid && it.dueDate <= horizon }?.let { row ->
                if (row.dueDate >= today.minusDays(1).toString()) {
                    Notifications.post(
                        applicationContext,
                        (loan.id * 31 + row.sequence).toInt(),
                        "ঋণের কিস্তি: ${loan.institution}",
                        "${row.sequence} নম্বর কিস্তি ${Money.format(row.amountMinor)} · ${BizDate.formatBn(row.dueDate)}",
                    )
                }
            }
        }
        container.repository.openEmis().forEach { emi ->
            val schedule = container.repository.schedule(ScheduleOwner.EMI, emi.id)
            val paid = container.repository.forLink(LinkKind.EMI, emi.id).mapNotNull { it.installmentIndex }.toSet()
            schedule.firstOrNull { it.sequence !in paid && it.dueDate <= horizon }?.let { row ->
                if (row.dueDate >= today.minusDays(1).toString()) {
                    Notifications.post(
                        applicationContext,
                        (emi.id * 37 + row.sequence).toInt(),
                        "কিস্তি: ${emi.productName}",
                        "${row.sequence} নম্বর কিস্তি ${Money.format(row.amountMinor)} · ${BizDate.formatBn(row.dueDate)}",
                    )
                }
            }
        }

        // Recurring rules only remind; the user decides whether to record them.
        container.repository.recurringRules().forEach { rule ->
            if (rule.nextRunDate <= today.toString()) {
                Notifications.post(
                    applicationContext,
                    rule.id.toInt() + 900,
                    "নিয়মিত লেনদেনের সময়",
                    "${rule.title} · ${Money.format(rule.amountMinor)} · ${Bn.txType(rule.type)}",
                )
                container.repository.advanceRecurring(rule)
            }
        }
        Result.success()
    }

    companion object {
        private const val WORK_NAME = "hisaabos-daily-reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}

/** Advances a recurring rule after its reminder was posted, without posting money. */
private suspend fun com.shohankhan.hisaabos.data.repo.HisaabRepository.advanceRecurring(
    rule: com.shohankhan.hisaabos.data.db.RecurringRuleEntity,
) {
    val next = when (rule.frequency) {
        RecurrenceFrequency.DAILY -> LocalDate.parse(rule.nextRunDate).plusDays(1)
        RecurrenceFrequency.WEEKLY -> LocalDate.parse(rule.nextRunDate).plusWeeks(1)
        RecurrenceFrequency.MONTHLY -> LocalDate.parse(rule.nextRunDate).plusMonths(1)
    }
    updateRecurringNextRun(rule.id, next.toString())
}
