package com.antigravity.unlockly.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class Wallet(
    @PrimaryKey
    val id: Int = 1,
    val availableSeconds: Long = 0,
    val earnedTodaySeconds: Long = 0,
    val spentTodaySeconds: Long = 0,
    val productiveStudySecondsToday: Long = 0L,
    val isProActive: Boolean = false,
    val unfreezeExpirationTimestamp: Long = 0L,
    val boostMultiplier: Float = 1.0f,
    val boostExpirationTimestamp: Long = 0L,
    val emergencyUnlockUsedToday: Boolean = false,
    val lastResetAt: Long = System.currentTimeMillis()
) {
    val isUnfrozen: Boolean
        get() = System.currentTimeMillis() < unfreezeExpirationTimestamp

    val isBoostActive: Boolean
        get() = boostMultiplier > 1.0f && System.currentTimeMillis() < boostExpirationTimestamp

    val effectiveBoostMultiplier: Float
        get() = if (isBoostActive) boostMultiplier else 1.0f
}
