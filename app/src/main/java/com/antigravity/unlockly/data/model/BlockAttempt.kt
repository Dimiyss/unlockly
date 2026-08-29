package com.antigravity.unlockly.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_attempts")
data class BlockAttempt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val attemptAt: Long = System.currentTimeMillis(),
    val walletAtAttempt: Long,
    val result: String // e.g. "BLOCKED_OVERLAY", "BLOCKED_REDIRECT", "ALLOWED_WITH_BALANCE"
)

data class HealthStatus(
    val usageAccessGranted: Boolean,
    val accessibilityGranted: Boolean,
    val overlayGranted: Boolean,
    val batteryOptimized: Boolean,
    val lastCheckedAt: Long = System.currentTimeMillis()
) {
    val isFullyHealthy: Boolean
        get() = usageAccessGranted && accessibilityGranted && overlayGranted
}
