package com.arhiplabs.unstuckly

import com.arhiplabs.unstuckly.data.model.Wallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletModelTest {

    @Test
    fun testDefaultWalletValues() {
        val wallet = Wallet()
        assertEquals(1, wallet.id)
        assertEquals(0L, wallet.availableSeconds)
        assertEquals(0L, wallet.earnedTodaySeconds)
        assertEquals(0L, wallet.spentTodaySeconds)
        assertEquals(0L, wallet.productiveStudySecondsToday)
        assertFalse(wallet.isProActive)
        assertEquals(0L, wallet.unfreezeExpirationTimestamp)
        assertEquals(1.0f, wallet.boostMultiplier, 0.001f)
        assertEquals(0L, wallet.boostExpirationTimestamp)
        assertFalse(wallet.emergencyUnlockUsedToday)
        assertFalse(wallet.isUnfrozen)
        assertFalse(wallet.isBoostActive)
        assertEquals(1.0f, wallet.effectiveBoostMultiplier, 0.001f)
    }

    @Test
    fun testIsUnfrozen_activeWhenTimestampInFuture() {
        val futureTime = System.currentTimeMillis() + 60_000L
        val wallet = Wallet(unfreezeExpirationTimestamp = futureTime)
        assertTrue(wallet.isUnfrozen)
    }

    @Test
    fun testIsUnfrozen_inactiveWhenTimestampInPast() {
        val pastTime = System.currentTimeMillis() - 1_000L
        val wallet = Wallet(unfreezeExpirationTimestamp = pastTime)
        assertFalse(wallet.isUnfrozen)
    }

    @Test
    fun testIsBoostActive_activeWhenMultiplierGreaterThanOneAndFuture() {
        val futureTime = System.currentTimeMillis() + 60_000L
        val wallet = Wallet(
            boostMultiplier = 2.0f,
            boostExpirationTimestamp = futureTime
        )
        assertTrue(wallet.isBoostActive)
        assertEquals(2.0f, wallet.effectiveBoostMultiplier, 0.001f)
    }

    @Test
    fun testIsBoostActive_inactiveWhenTimestampInPast() {
        val pastTime = System.currentTimeMillis() - 1_000L
        val wallet = Wallet(
            boostMultiplier = 2.0f,
            boostExpirationTimestamp = pastTime
        )
        assertFalse(wallet.isBoostActive)
        assertEquals(1.0f, wallet.effectiveBoostMultiplier, 0.001f)
    }

    @Test
    fun testIsBoostActive_inactiveWhenMultiplierIsOneOrLessEvenIfFuture() {
        val futureTime = System.currentTimeMillis() + 60_000L
        val wallet = Wallet(
            boostMultiplier = 1.0f,
            boostExpirationTimestamp = futureTime
        )
        assertFalse(wallet.isBoostActive)
        assertEquals(1.0f, wallet.effectiveBoostMultiplier, 0.001f)

        val negativeBoost = Wallet(
            boostMultiplier = 0.5f,
            boostExpirationTimestamp = futureTime
        )
        assertFalse(negativeBoost.isBoostActive)
        assertEquals(1.0f, negativeBoost.effectiveBoostMultiplier, 0.001f)
    }

    @Test
    fun testCopyAndFieldModifications() {
        val base = Wallet()
        val modified = base.copy(
            availableSeconds = 600L,
            earnedTodaySeconds = 1200L,
            spentTodaySeconds = 600L,
            isProActive = true
        )
        assertEquals(600L, modified.availableSeconds)
        assertEquals(1200L, modified.earnedTodaySeconds)
        assertEquals(600L, modified.spentTodaySeconds)
        assertTrue(modified.isProActive)
    }
}
