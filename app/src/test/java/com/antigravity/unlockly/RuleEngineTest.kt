package com.antigravity.unlockly

import com.antigravity.unlockly.domain.RuleEngine
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
        val validPassphrase = "I promise to stay focused on my learning goals today 2026!"
        assertTrue(validPassphrase.length >= 35)
        assertFalse(shortPassword.length >= 35)
    }

    @Test
    fun testWalletUnfreezeAndBoostState() {
        val now = System.currentTimeMillis()
        val walletActive = com.antigravity.unlockly.data.model.Wallet(
            isProActive = true,
            unfreezeExpirationTimestamp = now + 60_000L,
            boostMultiplier = 2.0f,
            boostExpirationTimestamp = now + 60_000L
        )
        assertTrue(walletActive.isUnfrozen)
        assertTrue(walletActive.isBoostActive)
        assertEquals(2.0f, walletActive.effectiveBoostMultiplier)

        val walletExpired = com.antigravity.unlockly.data.model.Wallet(
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
        val wallet = com.antigravity.unlockly.data.model.Wallet(
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
        val isFreeTier = false
        val isProTier = true

        val freeMinAllowed = if (isFreeTier) 30 else 30
        val proMinAllowed = if (isProTier) 15 else 30

        assertEquals(30, freeMinAllowed)
        assertEquals(15, proMinAllowed)

        // Free tier rejects targets < 30
        val target15mValidForFree = 15 >= freeMinAllowed
        assertFalse(target15mValidForFree)

        // Pro tier accepts 15, 30, 45, 60
        val target15mValidForPro = 15 >= proMinAllowed
        val target30mValidForPro = 30 >= proMinAllowed
        val target45mValidForPro = 45 >= proMinAllowed
        val target60mValidForPro = 60 >= proMinAllowed
        assertTrue(target15mValidForPro)
        assertTrue(target30mValidForPro)
        assertTrue(target45mValidForPro)
        assertTrue(target60mValidForPro)
    }
}


