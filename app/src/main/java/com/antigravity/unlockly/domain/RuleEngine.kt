package com.antigravity.unlockly.domain

import com.antigravity.unlockly.data.model.Rule

object RuleEngine {
    /**
     * Calculates earned reward seconds given active productive seconds,
     * target productive minutes, and reward minutes.
     * Ratio = (rewardMinutes * 60) / (productiveMinutesTarget * 60)
     */
    fun calculateRewardSeconds(
        activeProductiveSeconds: Long,
        productiveMinutesTarget: Int,
        rewardMinutes: Int,
        boostMultiplier: Float = 1.0f
    ): Long {
        if (productiveMinutesTarget <= 0 || rewardMinutes <= 0) return 0L
        val ratio = (rewardMinutes.toDouble() / productiveMinutesTarget.toDouble()) * boostMultiplier
        return (activeProductiveSeconds * ratio).toLong()
    }

    /**
     * Checks if accrued earned time today exceeds daily cap.
     */
    fun isDailyCapReached(earnedTodaySeconds: Long, dailyCapMinutes: Int): Boolean {
        if (dailyCapMinutes <= 0) return false
        val dailyCapSeconds = dailyCapMinutes * 60L
        return earnedTodaySeconds >= dailyCapSeconds
    }
}
