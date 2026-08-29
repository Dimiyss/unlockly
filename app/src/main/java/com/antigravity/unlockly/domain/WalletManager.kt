package com.antigravity.unlockly.domain

import com.antigravity.unlockly.data.model.Wallet
import com.antigravity.unlockly.data.repository.WalletRepository
import kotlinx.coroutines.flow.Flow

class WalletManager(private val repository: WalletRepository) {

    val walletFlow: Flow<Wallet?> = repository.walletFlow

    suspend fun getWallet(): Wallet = repository.getWallet()

    suspend fun addRewardSeconds(seconds: Long) {
        if (seconds > 0) {
            repository.addEarnedSeconds(seconds)
        }
    }

    suspend fun deductSpentSeconds(seconds: Long) {
        if (seconds > 0) {
            val wallet = getWallet()
            if (!wallet.isUnfrozen) {
                repository.deductSpentSeconds(seconds)
            }
        }
    }

    suspend fun recordEmergencyUnlockUsed(rewardSeconds: Long = 900L) {
        repository.recordEmergencyUnlockUsed(rewardSeconds)
    }

    suspend fun setProActive(isPro: Boolean) {
        repository.setProActive(isPro)
    }

    suspend fun activateUnfreeze(durationMinutes: Int) {
        val expiration = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        repository.setUnfreezeExpiration(expiration)
    }

    suspend fun activateBoost(multiplier: Float, durationMinutes: Int) {
        val expiration = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        repository.setBoost(multiplier, expiration)
    }

    suspend fun performMidnightReset() {
        repository.performMidnightReset()
    }
}
