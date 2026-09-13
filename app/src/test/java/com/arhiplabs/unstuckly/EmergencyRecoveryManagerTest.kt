package com.arhiplabs.unstuckly

import android.content.SharedPreferences
import com.arhiplabs.unstuckly.data.model.Rule
import com.arhiplabs.unstuckly.data.repository.RuleRepository
import com.arhiplabs.unstuckly.domain.EmergencyRecoveryManager
import com.arhiplabs.unstuckly.domain.EmergencyRecoveryManager.Companion.CHALLENGE_COOLDOWN_MS
import com.arhiplabs.unstuckly.domain.EmergencyRecoveryManager.Companion.TIMELOCK_DURATION_MS
import com.arhiplabs.unstuckly.fakes.FakeRuleDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class EmergencyRecoveryManagerTest {

    private lateinit var fakeRuleDao: FakeRuleDao
    private lateinit var ruleRepository: RuleRepository
    private lateinit var fakePrefs: FakeTestSharedPreferences
    private var currentTime = 1_000_000_000L

    private val validNewPassphrase = "I promise to stay focused on my learning goals today 2026!"

    @Before
    fun setUp() = runBlocking {
        fakeRuleDao = FakeRuleDao(
            listOf(
                Rule(
                    id = 1L,
                    productivePackages = listOf("com.duolingo"),
                    blockedPackages = listOf("com.zhiliaoapp.musically"),
                    emergencyPassword = "OldPassphraseMustBeVeryLong35CharactersMinimum!",
                    isActive = true
                )
            )
        )
        ruleRepository = RuleRepository(fakeRuleDao)
        fakePrefs = FakeTestSharedPreferences()
    }

    private fun createManager(): EmergencyRecoveryManager {
        return EmergencyRecoveryManager(
            prefs = fakePrefs,
            ruleRepository = ruleRepository,
            timeProvider = { currentTime }
        )
    }

    @Test
    fun testChallenge_initiallyAvailable() {
        val manager = createManager()
        assertTrue(manager.canUseChallenge())
        assertEquals(0L, manager.getChallengeCooldownRemainingMillis())
        assertTrue(manager.recoveryState.value.isChallengeAvailable)
    }

    @Test
    fun testChallenge_completeReset_enforcesSevenDaysCooldown() = runBlocking {
        val manager = createManager()

        // Complete 150-char challenge reset
        val result = manager.completeChallengeReset(validNewPassphrase)
        assertTrue(result)

        // Passphrase in repository updated
        val updatedRule = ruleRepository.getPrimaryActiveRule()
        assertEquals(validNewPassphrase, updatedRule?.emergencyPassword)

        // Challenge now in 7-day cooldown
        assertFalse(manager.canUseChallenge())
        assertEquals(CHALLENGE_COOLDOWN_MS, manager.getChallengeCooldownRemainingMillis())

        // 3 days later, still in cooldown
        currentTime += TimeUnit.DAYS.toMillis(3)
        manager.refreshState()
        assertFalse(manager.canUseChallenge())
        assertEquals(TimeUnit.DAYS.toMillis(4), manager.getChallengeCooldownRemainingMillis())

        // Attempting to complete challenge reset during cooldown fails
        val secondAttempt = manager.completeChallengeReset("AnotherNewPassphrase35CharsLengthIsRequiredHere!")
        assertFalse(secondAttempt)

        // 7 days + 1 ms later, challenge is available again
        currentTime += TimeUnit.DAYS.toMillis(4) + 1
        manager.refreshState()
        assertTrue(manager.canUseChallenge())
        assertEquals(0L, manager.getChallengeCooldownRemainingMillis())
    }

    @Test
    fun testChallenge_invalidPassphraseFails() = runBlocking {
        val manager = createManager()
        val tooShort = "Short"
        val result = manager.completeChallengeReset(tooShort)
        assertFalse(result)

        // Rule password remains unchanged
        val rule = ruleRepository.getPrimaryActiveRule()
        assertEquals("OldPassphraseMustBeVeryLong35CharactersMinimum!", rule?.emergencyPassword)
    }

    @Test
    fun testTimeLock_cycle() = runBlocking {
        val manager = createManager()

        // Initially no timelock running
        assertFalse(manager.recoveryState.value.isTimeLockActive)
        assertFalse(manager.recoveryState.value.isTimeLockReady)

        // Start 24h timelock
        manager.startTimeLockReset()
        assertTrue(manager.recoveryState.value.isTimeLockActive)
        assertFalse(manager.recoveryState.value.isTimeLockReady)
        assertEquals(TIMELOCK_DURATION_MS, manager.recoveryState.value.timeLockRemainingMillis)

        // 12 hours later -> still active, not ready
        currentTime += TimeUnit.HOURS.toMillis(12)
        manager.refreshState()
        assertTrue(manager.recoveryState.value.isTimeLockActive)
        assertFalse(manager.recoveryState.value.isTimeLockReady)
        assertEquals(TimeUnit.HOURS.toMillis(12), manager.recoveryState.value.timeLockRemainingMillis)

        // Cannot complete reset yet
        val earlyComplete = manager.completeTimeLockReset(validNewPassphrase)
        assertFalse(earlyComplete)

        // 24 hours total passed -> ready!
        currentTime += TimeUnit.HOURS.toMillis(12) + 1000
        manager.refreshState()
        assertFalse(manager.recoveryState.value.isTimeLockActive)
        assertTrue(manager.recoveryState.value.isTimeLockReady)

        // Complete reset
        val completed = manager.completeTimeLockReset(validNewPassphrase)
        assertTrue(completed)

        // State reset after completion
        assertFalse(manager.recoveryState.value.isTimeLockActive)
        assertFalse(manager.recoveryState.value.isTimeLockReady)

        // Rule updated
        val updatedRule = ruleRepository.getPrimaryActiveRule()
        assertEquals(validNewPassphrase, updatedRule?.emergencyPassword)
    }

    @Test
    fun testTimeLock_cancel() {
        val manager = createManager()
        manager.startTimeLockReset()
        assertTrue(manager.recoveryState.value.isTimeLockActive)

        manager.cancelTimeLockReset()
        assertFalse(manager.recoveryState.value.isTimeLockActive)
        assertFalse(manager.recoveryState.value.isTimeLockReady)
    }
}

class FakeTestSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = data
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = data[key] as? MutableSet<String> ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()

        override fun putString(key: String?, value: String?) = apply { temp[key ?: ""] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { temp[key ?: ""] = values }
        override fun putInt(key: String?, value: Int) = apply { temp[key ?: ""] = value }
        override fun putLong(key: String?, value: Long) = apply { temp[key ?: ""] = value }
        override fun putFloat(key: String?, value: Float) = apply { temp[key ?: ""] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { temp[key ?: ""] = value }
        override fun remove(key: String?) = apply { temp[key ?: ""] = null }
        override fun clear() = apply { data.clear() }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            for ((k, v) in temp) {
                if (v == null) data.remove(k) else data[k] = v
            }
        }
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
}
