package com.arhiplabs.unstuckly.domain

import android.content.Context
import android.content.SharedPreferences
import com.arhiplabs.unstuckly.data.repository.RuleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

data class EmergencyRecoveryState(
    val lastChallengeTimestamp: Long = 0L,
    val timeLockStartTimestamp: Long = 0L,
    val isChallengeAvailable: Boolean = true,
    val challengeCooldownRemainingMillis: Long = 0L,
    val isTimeLockActive: Boolean = false,
    val isTimeLockReady: Boolean = false,
    val timeLockRemainingMillis: Long = 0L
)

class EmergencyRecoveryManager(
    private val prefs: SharedPreferences,
    private val ruleRepository: RuleRepository,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    constructor(
        context: Context,
        ruleRepository: RuleRepository,
        timeProvider: () -> Long = { System.currentTimeMillis() }
    ) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        ruleRepository,
        timeProvider
    )

    private val _recoveryState = MutableStateFlow(computeState())
    val recoveryState: StateFlow<EmergencyRecoveryState> = _recoveryState.asStateFlow()

    fun refreshState() {
        _recoveryState.value = computeState()
    }

    private fun computeState(): EmergencyRecoveryState {
        val now = timeProvider()
        val lastChallenge = prefs.getLong(KEY_LAST_CHALLENGE_TIMESTAMP, 0L)
        val timeLockStart = prefs.getLong(KEY_TIMELOCK_START_TIMESTAMP, 0L)

        // 7-day cooldown for typing challenge
        val challengeElapsed = if (lastChallenge > 0L) now - lastChallenge else Long.MAX_VALUE
        val isChallengeAvailable = challengeElapsed >= CHALLENGE_COOLDOWN_MS
        val challengeCooldownRemaining = if (isChallengeAvailable) 0L else (CHALLENGE_COOLDOWN_MS - challengeElapsed).coerceAtLeast(0L)

        // 24-hour timelock status
        val isTimeLockRunning = timeLockStart > 0L
        val timeLockElapsed = if (isTimeLockRunning) now - timeLockStart else 0L
        val isTimeLockReady = isTimeLockRunning && timeLockElapsed >= TIMELOCK_DURATION_MS
        val isTimeLockActive = isTimeLockRunning && !isTimeLockReady
        val timeLockRemaining = if (isTimeLockActive) (TIMELOCK_DURATION_MS - timeLockElapsed).coerceAtLeast(0L) else 0L

        return EmergencyRecoveryState(
            lastChallengeTimestamp = lastChallenge,
            timeLockStartTimestamp = timeLockStart,
            isChallengeAvailable = isChallengeAvailable,
            challengeCooldownRemainingMillis = challengeCooldownRemaining,
            isTimeLockActive = isTimeLockActive,
            isTimeLockReady = isTimeLockReady,
            timeLockRemainingMillis = timeLockRemaining
        )
    }

    fun canUseChallenge(): Boolean {
        refreshState()
        return _recoveryState.value.isChallengeAvailable
    }

    fun getChallengeCooldownRemainingMillis(): Long {
        refreshState()
        return _recoveryState.value.challengeCooldownRemainingMillis
    }

    fun startTimeLockReset() {
        val now = timeProvider()
        prefs.edit().putLong(KEY_TIMELOCK_START_TIMESTAMP, now).apply()
        refreshState()
    }

    fun cancelTimeLockReset() {
        prefs.edit().putLong(KEY_TIMELOCK_START_TIMESTAMP, 0L).apply()
        refreshState()
    }

    suspend fun completeChallengeReset(newPassphrase: String): Boolean {
        val validation = RuleEngine.validateEmergencyPassphrase(newPassphrase)
        if (!validation.isValid) {
            return false
        }
        if (!canUseChallenge()) {
            return false
        }

        updateRulePassphrase(newPassphrase)

        val now = timeProvider()
        prefs.edit()
            .putLong(KEY_LAST_CHALLENGE_TIMESTAMP, now)
            .putLong(KEY_TIMELOCK_START_TIMESTAMP, 0L)
            .apply()

        refreshState()
        return true
    }

    suspend fun completeTimeLockReset(newPassphrase: String): Boolean {
        refreshState()
        if (!_recoveryState.value.isTimeLockReady) {
            return false
        }
        val validation = RuleEngine.validateEmergencyPassphrase(newPassphrase)
        if (!validation.isValid) {
            return false
        }

        updateRulePassphrase(newPassphrase)

        prefs.edit().putLong(KEY_TIMELOCK_START_TIMESTAMP, 0L).apply()
        refreshState()
        return true
    }

    private suspend fun updateRulePassphrase(newPassphrase: String) {
        val primaryRule = ruleRepository.getPrimaryActiveRule()
        if (primaryRule != null) {
            ruleRepository.saveRule(primaryRule.copy(emergencyPassword = newPassphrase.trim()))
        }
    }

    companion object {
        const val PREFS_NAME = "unstuckly_recovery_prefs"
        const val KEY_LAST_CHALLENGE_TIMESTAMP = "key_last_challenge_timestamp"
        const val KEY_TIMELOCK_START_TIMESTAMP = "key_timelock_start_timestamp"

        val CHALLENGE_COOLDOWN_MS = TimeUnit.DAYS.toMillis(7) // 7 days
        val TIMELOCK_DURATION_MS = TimeUnit.HOURS.toMillis(24) // 24 hours

        fun formatRemainingTime(millis: Long): String {
            if (millis <= 0L) return "0m"
            val days = TimeUnit.MILLISECONDS.toDays(millis)
            val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

            return when {
                days > 0 -> "${days}d ${hours}h"
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    }
}
