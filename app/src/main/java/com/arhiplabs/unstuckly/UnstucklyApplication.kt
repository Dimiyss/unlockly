package com.arhiplabs.unstuckly

import android.app.Application
import com.arhiplabs.unstuckly.data.db.AppDatabase
import com.arhiplabs.unstuckly.data.repository.RuleRepository
import com.arhiplabs.unstuckly.data.repository.WalletRepository
import com.arhiplabs.unstuckly.domain.EarnSessionManager
import com.arhiplabs.unstuckly.domain.WalletManager
import com.arhiplabs.unstuckly.domain.BlockingCoordinator
import com.arhiplabs.unstuckly.domain.HealthMonitor
import com.arhiplabs.unstuckly.worker.MidnightResetWorker

class UnstucklyApplication : Application() {

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
        lateinit var instance: UnstucklyApplication
            private set
    }
}
