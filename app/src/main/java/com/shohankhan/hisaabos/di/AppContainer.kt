package com.shohankhan.hisaabos.di

import android.content.Context
import androidx.room.Room
import com.shohankhan.hisaabos.backup.BackupService
import com.shohankhan.hisaabos.data.db.HisaabDatabase
import com.shohankhan.hisaabos.data.prefs.AppPreferences
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.TransactionService
import com.shohankhan.hisaabos.security.SessionManager

/**
 * Lightweight hand-written dependency graph. Everything is lazily created and
 * tied to the Application process — no reflection, no framework, no network.
 */
class AppContainer(private val appContext: Context) {

    val database: HisaabDatabase by lazy { HisaabDatabase.build(appContext) }
    val preferences: AppPreferences by lazy { AppPreferences(appContext) }
    val sessionManager: SessionManager by lazy { SessionManager() }
    val transactionService: TransactionService by lazy { TransactionService(database) }
    val repository: HisaabRepository by lazy { HisaabRepository(database, transactionService) }
    val backupService: BackupService by lazy { BackupService(database, preferences) }
}
