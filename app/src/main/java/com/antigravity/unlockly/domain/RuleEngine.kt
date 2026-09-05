package com.antigravity.unlockly.domain

data class PassphraseValidationResult(
    val isValid: Boolean,
    val error: String?
)

object RuleEngine {
    const val MIN_PASSPHRASE_LENGTH = 35
    const val MIN_DISTINCT_CHARS = 6
    const val MAX_CONSECUTIVE_IDENTICAL_CHARS = 4
    const val MAX_FREE_TIER_APPS_PER_CATEGORY = 2

    /**
     * Checks if selected app count respects Free tier limits (max 2 per category) or PRO (unlimited).
     */
    fun isAppCountValid(productiveCount: Int, blockedCount: Int, isPro: Boolean): Boolean {
        if (isPro) return true
        return productiveCount <= MAX_FREE_TIER_APPS_PER_CATEGORY && blockedCount <= MAX_FREE_TIER_APPS_PER_CATEGORY
    }

    /**
     * Validates that the emergency passphrase meets length and diversity requirements
     * and is not composed of duplicates or repetitive symbols (e.g., 35 of the same character).
     */
    fun validateEmergencyPassphrase(passphrase: String): PassphraseValidationResult {
        val trimmed = passphrase.trim()
        if (trimmed.length < MIN_PASSPHRASE_LENGTH) {
            return PassphraseValidationResult(
                isValid = false,
                error = "Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters (${trimmed.length}/$MIN_PASSPHRASE_LENGTH)."
            )
        }

        // Check for duplicate symbols / lack of character diversity (e.g., 35 identical characters)
        val distinctChars = trimmed.toSet().size
        if (distinctChars < MIN_DISTINCT_CHARS) {
            return PassphraseValidationResult(
                isValid = false,
                error = "Passphrase cannot be composed of repetitive or duplicate characters. Please enter a real sentence with diverse characters."
            )
        }

        // Check max consecutive identical characters (e.g., "aaaaa")
        var consecutiveCount = 1
        for (i in 1 until trimmed.length) {
            if (trimmed[i] == trimmed[i - 1]) {
                consecutiveCount++
                if (consecutiveCount > MAX_CONSECUTIVE_IDENTICAL_CHARS) {
                    return PassphraseValidationResult(
                        isValid = false,
                        error = "Passphrase cannot contain more than $MAX_CONSECUTIVE_IDENTICAL_CHARS identical consecutive characters."
                    )
                }
            } else {
                consecutiveCount = 1
            }
        }

        // Check single character frequency (no single character should make up > 40% of the passphrase)
        val maxCharFrequency = trimmed.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
        if (maxCharFrequency > (trimmed.length * 0.4).toInt()) {
            return PassphraseValidationResult(
                isValid = false,
                error = "Passphrase contains too many repeated instances of a single character."
            )
        }

        return PassphraseValidationResult(isValid = true, error = null)
    }

    fun isEmergencyPassphraseValid(passphrase: String): Boolean {
        return validateEmergencyPassphrase(passphrase).isValid
    }

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
