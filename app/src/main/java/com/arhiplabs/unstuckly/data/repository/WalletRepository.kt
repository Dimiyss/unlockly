package com.arhiplabs.unstuckly.data.repository

import com.arhiplabs.unstuckly.data.db.WalletDao
import com.arhiplabs.unstuckly.data.model.Wallet
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class WalletRepository(private val walletDao: WalletDao) {
    val walletFlow: Flow<Wallet?> = walletDao.getWalletFlow()

    suspend fun getWallet(): Wallet {
        val current = walletDao.getWallet()
        if (current == null) {
            val initial = Wallet()
            walletDao.insertOrUpdateWallet(initial)
            return initial
        }

        // Auto-check if day has changed to expire wallet & reset daily limits immediately
        if (isDifferentDay(current.lastResetAt, System.currentTimeMillis())) {
            val resetWallet = current.copy(
                availableSeconds = 0L, // All wallet expires at end of day
                earnedTodaySeconds = 0L,
                spentTodaySeconds = 0L,
                productiveStudySecondsToday = 0L,
                emergencyUnlockUsedToday = false,
                lastResetAt = System.currentTimeMillis()
            )
            walletDao.insertOrUpdateWallet(resetWallet)
            return resetWallet
        }

        return current
    }

    suspend fun updateWallet(wallet: Wallet) {
        walletDao.insertOrUpdateWallet(wallet)
    }

    suspend fun addProductiveStudySeconds(seconds: Long) {
        val current = getWallet()
        val updated = current.copy(
            productiveStudySecondsToday = current.productiveStudySecondsToday + seconds
        )
        walletDao.insertOrUpdateWallet(updated)
    }

    suspend fun addEarnedSeconds(seconds: Long) {
        val current = getWallet()
        val updated = current.copy(
            availableSeconds = current.availableSeconds + seconds,
            earnedTodaySeconds = current.earnedTodaySeconds + seconds
        )
        walletDao.insertOrUpdateWallet(updated)
    }

    suspend fun deductSpentSeconds(seconds: Long) {
        val current = getWallet()
        val newAvailable = maxOf(0L, current.availableSeconds - seconds)
        val updated = current.copy(
            availableSeconds = newAvailable,
            spentTodaySeconds = current.spentTodaySeconds + seconds
        )
        walletDao.insertOrUpdateWallet(updated)
    }

    suspend fun recordEmergencyUnlockUsed(rewardSeconds: Long = 900L) {
        val current = getWallet()
        val updated = current.copy(
            availableSeconds = current.availableSeconds + rewardSeconds,
            earnedTodaySeconds = current.earnedTodaySeconds + rewardSeconds,
            emergencyUnlockUsedToday = true
        )
        walletDao.insertOrUpdateWallet(updated)
    }

    suspend fun setProActive(isPro: Boolean) {
        val current = getWallet()
        val updated = current.copy(isProActive = isPro)
        walletDao.insertOrUpdateWallet(updated)
    }

    suspend fun setUnfreezeExpiration(timestampMs: Long) {
        val current = getWallet()
        val updated = current.copy(unfreezeExpirationTimestamp = timestampMs)
        walletDao.insertOrUpdateWallet(updated)
    }

    suspend fun setBoost(multiplier: Float, expirationTimestampMs: Long) {
        val current = getWallet()
        val updated = current.copy(
            boostMultiplier = multiplier,
            boostExpirationTimestamp = expirationTimestampMs
        )
        walletDao.insertOrUpdateWallet(updated)
    }

    suspend fun performMidnightReset() {
        val current = walletDao.getWallet() ?: Wallet()
        val updated = current.copy(
            availableSeconds = 0L, // All wallet expires at the end of the day
            earnedTodaySeconds = 0L,
            spentTodaySeconds = 0L,
            productiveStudySecondsToday = 0L,
            emergencyUnlockUsedToday = false,
            lastResetAt = System.currentTimeMillis()
        )
        walletDao.insertOrUpdateWallet(updated)
    }

    private fun isDifferentDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR) ||
               cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR)
    }
}
