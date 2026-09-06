package com.arhiplabs.unstuckly

import com.arhiplabs.unstuckly.data.model.Wallet
import com.arhiplabs.unstuckly.data.repository.WalletRepository
import com.arhiplabs.unstuckly.domain.WalletManager
import com.arhiplabs.unstuckly.fakes.FakeWalletDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WalletManagerTest {

    private lateinit var fakeDao: FakeWalletDao
    private lateinit var repository: WalletRepository
    private lateinit var walletManager: WalletManager

    @Before
    fun setUp() {
        fakeDao = FakeWalletDao()
        repository = WalletRepository(fakeDao)
        walletManager = WalletManager(repository)
    }

    @Test
    fun testDeductSpentSeconds_deductsWhenNotUnfrozen() = runBlocking {
        walletManager.addRewardSeconds(500L)
        walletManager.deductSpentSeconds(100L)

        val wallet = walletManager.getWallet()
        assertEquals(400L, wallet.availableSeconds)
        assertEquals(100L, wallet.spentTodaySeconds)
    }

    @Test
    fun testDeductSpentSeconds_bypassesDeductionWhenUnfrozen() = runBlocking {
        walletManager.addRewardSeconds(500L)
        // Activate unfreeze for 15 minutes
        walletManager.activateUnfreeze(15)

        assertTrue(walletManager.getWallet().isUnfrozen)

        // Attempt deduction
        walletManager.deductSpentSeconds(100L)

        val wallet = walletManager.getWallet()
        // Available seconds should remain untouched because unfreeze is active
        assertEquals(500L, wallet.availableSeconds)
        assertEquals(0L, wallet.spentTodaySeconds)
    }

    @Test
    fun testZeroOrNegativeSecondsIgnored() = runBlocking {
        walletManager.addRewardSeconds(100L)

        // Attempt adding 0 or negative
        walletManager.addRewardSeconds(0L)
        walletManager.addRewardSeconds(-50L)
        assertEquals(100L, walletManager.getWallet().availableSeconds)

        walletManager.addProductiveStudySeconds(0L)
        walletManager.addProductiveStudySeconds(-10L)
        assertEquals(0L, walletManager.getWallet().productiveStudySecondsToday)

        walletManager.deductSpentSeconds(0L)
        walletManager.deductSpentSeconds(-20L)
        assertEquals(100L, walletManager.getWallet().availableSeconds)
    }

    @Test
    fun testActivateBoostCalculatesFutureExpiration() = runBlocking {
        val now = System.currentTimeMillis()
        walletManager.activateBoost(2.0f, 30)

        val wallet = walletManager.getWallet()
        assertEquals(2.0f, wallet.boostMultiplier, 0.001f)
        assertTrue(wallet.boostExpirationTimestamp >= now + (30 * 60 * 1000L) - 1000L)
        assertTrue(wallet.isBoostActive)
        assertEquals(2.0f, wallet.effectiveBoostMultiplier, 0.001f)
    }

    @Test
    fun testEmergencyUnlockDelegation() = runBlocking {
        walletManager.recordEmergencyUnlockUsed(900L)

        val wallet = walletManager.getWallet()
        assertEquals(900L, wallet.availableSeconds)
        assertTrue(wallet.emergencyUnlockUsedToday)
    }

    @Test
    fun testProActivationDelegation() = runBlocking {
        assertFalse(walletManager.getWallet().isProActive)
        walletManager.setProActive(true)
        assertTrue(walletManager.getWallet().isProActive)
    }
}
