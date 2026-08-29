package com.antigravity.unlockly

import android.app.Application
import com.antigravity.unlockly.data.db.AppDatabase
import com.antigravity.unlockly.data.repository.RuleRepository
import com.antigravity.unlockly.data.repository.WalletRepository
import com.antigravity.unlockly.domain.EarnSessionManager
import com.antigravity.unlockly.domain.WalletManager
import com.antigravity.unlockly.domain.BlockingCoordinator
import com.antigravity.unlockly.domain.HealthMonitor
import com.antigravity.unlockly.worker.MidnightResetWorker

class UnlocklyApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val ruleRepository by lazy { RuleRepository(database.ruleDao()) }
    val walletRepository by lazy { WalletRepository(database.walletDao()) }
    
    val walletManager by lazy { WalletManager(walletRepository) }
    val earnSessionManager by lazy { EarnSessionManager(walletManager, ruleRepository) }
    val blockingCoordinator by lazy { BlockingCoordinator(this, walletManager, ruleRepository) }
    val healthMonitor by lazy { HealthMonitor(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        MidnightResetWorker.scheduleDailyReset(this)
    }

    companion object {
        lateinit var instance: UnlocklyApplication
            private set
    }
}
