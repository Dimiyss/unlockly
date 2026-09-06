package com.arhiplabs.unstuckly

import com.arhiplabs.unstuckly.domain.RuleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    @Test
    fun testCalculateRewardSeconds() {
        // 30 mins productive target, 20 mins reward => ratio 2/3
        // 1800 seconds productive (30m) -> 1200 seconds reward (20m)
        val rewardSec = RuleEngine.calculateRewardSeconds(
            activeProductiveSeconds = 1800L,
            productiveMinutesTarget = 30,
            rewardMinutes = 20
        )
        assertEquals(1200L, rewardSec)
    }

    @Test
    fun testCalculateRewardSeconds_1to1Ratio() {
        val rewardSec = RuleEngine.calculateRewardSeconds(
            activeProductiveSeconds = 600L,
            productiveMinutesTarget = 10,
            rewardMinutes = 10
        )
        assertEquals(600L, rewardSec)
    }

    @Test
    fun testIsDailyCapReached() {
        val capMinutes = 120 // 7200 seconds
        assertFalse(RuleEngine.isDailyCapReached(7199L, capMinutes))
        assertTrue(RuleEngine.isDailyCapReached(7200L, capMinutes))
        assertTrue(RuleEngine.isDailyCapReached(8000L, capMinutes))
    }

    @Test
    fun testCalculateRewardSeconds_WithBoost() {
        // 30 mins productive target, 20 mins reward with 2.0x mega boost
        // 1800s productive -> 1200s base * 2.0 = 2400s
        val rewardSec = RuleEngine.calculateRewardSeconds(
            activeProductiveSeconds = 1800L,
            productiveMinutesTarget = 30,
            rewardMinutes = 20,
            boostMultiplier = 2.0f
        )
        assertEquals(2400L, rewardSec)
    }

    @Test
    fun testEmergencyPasswordValidation() {
        val shortPassword = "Too short password"
        val sameSymbol35Chars = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val numbers35Chars = "11111111111111111111111111111111111"
        val excessiveConsecutive = "aaaaa I promise to stay focused today 2026!"
        val validPassphrase = "I promise to stay focused on my learning goals today 2026!"

        // Short password rejected
        assertFalse(RuleEngine.isEmergencyPassphraseValid(shortPassword))
        assertEquals(
            "Passphrase must be at least 35 characters (${shortPassword.length}/35).",
            RuleEngine.validateEmergencyPassphrase(shortPassword).error
        )

        // 35 same symbols rejected
        assertFalse(RuleEngine.isEmergencyPassphraseValid(sameSymbol35Chars))
        assertFalse(RuleEngine.isEmergencyPassphraseValid(numbers35Chars))

        // Excessive consecutive duplicates rejected
        assertFalse(RuleEngine.isEmergencyPassphraseValid(excessiveConsecutive))

        // Valid sentence accepted
        assertTrue(RuleEngine.isEmergencyPassphraseValid(validPassphrase))
        assertEquals(null, RuleEngine.validateEmergencyPassphrase(validPassphrase).error)
    }

    @Test
    fun testWalletUnfreezeAndBoostState() {
        val now = System.currentTimeMillis()
        val walletActive = com.arhiplabs.unstuckly.data.model.Wallet(
            isProActive = true,
            unfreezeExpirationTimestamp = now + 60_000L,
            boostMultiplier = 2.0f,
            boostExpirationTimestamp = now + 60_000L
        )
        assertTrue(walletActive.isUnfrozen)
        assertTrue(walletActive.isBoostActive)
        assertEquals(2.0f, walletActive.effectiveBoostMultiplier)

        val walletExpired = com.arhiplabs.unstuckly.data.model.Wallet(
            isProActive = false,
            unfreezeExpirationTimestamp = now - 1000L,
            boostMultiplier = 2.0f,
            boostExpirationTimestamp = now - 1000L
        )
        assertFalse(walletExpired.isUnfrozen)
        assertFalse(walletExpired.isBoostActive)
        assertEquals(1.0f, walletExpired.effectiveBoostMultiplier)
    }

    @Test
    fun testDailyWalletExpirationAndEmergencyUnlockLimit() {
        val wallet = com.arhiplabs.unstuckly.data.model.Wallet(
            availableSeconds = 900L,
            earnedTodaySeconds = 900L,
            spentTodaySeconds = 400L,
            emergencyUnlockUsedToday = true
        )
        assertTrue(wallet.emergencyUnlockUsedToday)
        assertEquals(900L, wallet.availableSeconds)

        // After midnight reset, availableSeconds expires to 0 and emergency unlock flag resets
        val resetWallet = wallet.copy(
            availableSeconds = 0L,
            earnedTodaySeconds = 0L,
            spentTodaySeconds = 0L,
            emergencyUnlockUsedToday = false
        )
        assertEquals(0L, resetWallet.availableSeconds)
        assertEquals(0L, resetWallet.earnedTodaySeconds)
        assertFalse(resetWallet.emergencyUnlockUsedToday)
    }

    @Test
    fun testTargetStudyRequirementValidation() {
        fun isTargetValid(target: Int, isPro: Boolean): Boolean {
            return if (!isPro) {
                target == 30 // Free package supports only one 30 min minimum interval
            } else {
                target >= 15 // PRO tier allows flexible intervals 15m+
            }
        }

        // Free tier supports only 30m interval
        assertFalse(isTargetValid(15, isPro = false))
        assertTrue(isTargetValid(30, isPro = false))
        assertFalse(isTargetValid(45, isPro = false))
        assertFalse(isTargetValid(60, isPro = false))

        // PRO tier supports 15m, 30m, 45m, 60m
        assertTrue(isTargetValid(15, isPro = true))
        assertTrue(isTargetValid(30, isPro = true))
        assertTrue(isTargetValid(45, isPro = true))
        assertTrue(isTargetValid(60, isPro = true))
        assertFalse(isTargetValid(10, isPro = true))
    }

    @Test
    fun testFreeTierAppLimitValidation() {
        // Free tier allows up to 2 productive and 2 blocked apps
        assertTrue(RuleEngine.isAppCountValid(productiveCount = 1, blockedCount = 1, isPro = false))
        assertTrue(RuleEngine.isAppCountValid(productiveCount = 2, blockedCount = 2, isPro = false))
        assertFalse(RuleEngine.isAppCountValid(productiveCount = 3, blockedCount = 1, isPro = false))
        assertFalse(RuleEngine.isAppCountValid(productiveCount = 1, blockedCount = 3, isPro = false))
        assertFalse(RuleEngine.isAppCountValid(productiveCount = 4, blockedCount = 4, isPro = false))

        // PRO tier allows unlimited apps
        assertTrue(RuleEngine.isAppCountValid(productiveCount = 3, blockedCount = 3, isPro = true))
        assertTrue(RuleEngine.isAppCountValid(productiveCount = 10, blockedCount = 10, isPro = true))
    }

    @Test
    fun testInitialTargetStudyThresholdEnforcement() {
        val targetMinutes = 30
        val targetSeconds = targetMinutes * 60L
        val rewardMinutes = 20
        val rewardSeconds = rewardMinutes * 60L

        fun evaluateReward(productiveSeconds: Long): Long {
            return if (productiveSeconds < targetSeconds) {
                0L // Below initial daily target: locked!
            } else {
                rewardSeconds // Initial minimum interval completed!
            }
        }

        // Before completing 30m target: 0s reward, social apps blocked
        assertEquals(0L, evaluateReward(0L))
        assertEquals(0L, evaluateReward(300L)) // 5m
        assertEquals(0L, evaluateReward(1799L)) // 29m 59s

        // Reaching 30m target: full 20m reward unlocked
        assertEquals(1200L, evaluateReward(1800L))
        assertEquals(1200L, evaluateReward(2000L))
    }

    @Test
    fun testPipBlockedPackageResolution() {
        val blockedPackages = setOf("com.google.android.youtube", "com.zhiliaoapp.musically", "com.instagram.android")
        val youtubePip = "com.google.android.youtube"
        val duolingoPip = "com.duolingo"

        assertTrue(blockedPackages.contains(youtubePip))
        assertFalse(blockedPackages.contains(duolingoPip))
    }

    @Test
    fun testPipWalletDeductionAndBlockingDecision() {
        fun evaluatePipAction(
            pipPackage: String,
            blockedPackages: Set<String>,
            isUnfrozen: Boolean,
            availableSeconds: Long
        ): String {
            if (!blockedPackages.contains(pipPackage)) return "IGNORE"
            if (isUnfrozen) return "ALLOW_FREE"
            return if (availableSeconds > 0) "DEDUCT_SECOND" else "BLOCK_AND_NEUTRALIZE"
        }

        val blocked = setOf("com.google.android.youtube")

        // 1. YouTube in PiP with wallet balance -> DEDUCT
        assertEquals(
            "DEDUCT_SECOND",
            evaluatePipAction("com.google.android.youtube", blocked, isUnfrozen = false, availableSeconds = 120L)
        )

        // 2. YouTube in PiP with empty wallet -> BLOCK_AND_NEUTRALIZE
        assertEquals(
            "BLOCK_AND_NEUTRALIZE",
            evaluatePipAction("com.google.android.youtube", blocked, isUnfrozen = false, availableSeconds = 0L)
        )

        // 3. YouTube in PiP with active unfreeze -> ALLOW_FREE
        assertEquals(
            "ALLOW_FREE",
            evaluatePipAction("com.google.android.youtube", blocked, isUnfrozen = true, availableSeconds = 0L)
        )

        // 4. Non-blocked app in PiP -> IGNORE
        assertEquals(
            "IGNORE",
            evaluatePipAction("com.google.android.apps.maps", blocked, isUnfrozen = false, availableSeconds = 0L)
        )
    }

    @Test
    fun testCalculateRewardSeconds_zeroOrNegativeInputs() {
        assertEquals(
            0L,
            RuleEngine.calculateRewardSeconds(
                activeProductiveSeconds = 600L,
                productiveMinutesTarget = 0,
                rewardMinutes = 20
            )
        )
        assertEquals(
            0L,
            RuleEngine.calculateRewardSeconds(
                activeProductiveSeconds = 600L,
                productiveMinutesTarget = 30,
                rewardMinutes = 0
            )
        )
        assertEquals(
            0L,
            RuleEngine.calculateRewardSeconds(
                activeProductiveSeconds = 600L,
                productiveMinutesTarget = -10,
                rewardMinutes = 20
            )
        )
    }

    @Test
    fun testIsDailyCapReached_zeroOrNegativeCap() {
        assertFalse(RuleEngine.isDailyCapReached(earnedTodaySeconds = 1000L, dailyCapMinutes = 0))
        assertFalse(RuleEngine.isDailyCapReached(earnedTodaySeconds = 1000L, dailyCapMinutes = -10))
    }

    @Test
    fun testEmergencyPasswordValidation_consecutiveDuplicateThreshold() {
        // 4 identical consecutive characters is the maximum allowed
        val fourConsecutive = "I promise to stay focused today 2026 aaaa!"
        assertTrue(RuleEngine.isEmergencyPassphraseValid(fourConsecutive))

        // 5 identical consecutive characters is rejected
        val fiveConsecutive = "I promise to stay focused today 2026 aaaaa!"
        assertFalse(RuleEngine.isEmergencyPassphraseValid(fiveConsecutive))
        assertEquals(
            "Passphrase cannot contain more than 4 identical consecutive characters.",
            RuleEngine.validateEmergencyPassphrase(fiveConsecutive).error
        )
    }

    @Test
    fun testEmergencyPasswordValidation_frequencyLimit() {
        // Passphrase of 40 chars where one char appears 17 times (17 > 40 * 0.4 = 16)
        val highCharFreq = "z 1 z 2 z 3 z 4 z 5 z 6 z 7 z 8 z 9 z 0 z a z b z"
        val validation = RuleEngine.validateEmergencyPassphrase(highCharFreq)
        assertFalse(validation.isValid)
        assertEquals(
            "Passphrase contains too many repeated instances of a single character.",
            validation.error
        )
    }
}


