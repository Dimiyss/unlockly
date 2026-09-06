package com.arhiplabs.unstuckly

import com.arhiplabs.unstuckly.data.model.Wallet
import com.arhiplabs.unstuckly.data.repository.WalletRepository
import com.arhiplabs.unstuckly.fakes.FakeWalletDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class WalletRepositoryTest {

    private lateinit var fakeDao: FakeWalletDao
    private lateinit var repository: WalletRepository

    @Before
    fun setUp() {
        fakeDao = FakeWalletDao()
        repository = WalletRepository(fakeDao)
    }

    @Test
    fun testGetWallet_createsInitialWalletWhenNull() = runBlocking {
        val wallet = repository.getWallet()
        assertEquals(1, wallet.id)
        assertEquals(0L, wallet.availableSeconds)
        assertEquals(0L, wallet.earnedTodaySeconds)
        assertEquals(0L, wallet.spentTodaySeconds)
    }

    @Test
    fun testGetWallet_returnsExistingWhenSameDay() = runBlocking {
        val initial = Wallet(
            availableSeconds = 500L,
            earnedTodaySeconds = 1000L,
            spentTodaySeconds = 500L,
            lastResetAt = System.currentTimeMillis()
        )
        fakeDao.insertOrUpdateWallet(initial)

        val retrieved = repository.getWallet()
        assertEquals(500L, retrieved.availableSeconds)
        assertEquals(1000L, retrieved.earnedTodaySeconds)
        assertEquals(500L, retrieved.spentTodaySeconds)
    }

    @Test
    fun testGetWallet_autoResetsWhenDayChanges() = runBlocking {
        // Create wallet with timestamp from yesterday
        val yesterdayCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val oldWallet = Wallet(
            availableSeconds = 600L,
            earnedTodaySeconds = 1200L,
            spentTodaySeconds = 600L,
            productiveStudySecondsToday = 1800L,
            emergencyUnlockUsedToday = true,
            lastResetAt = yesterdayCal.timeInMillis
        )
        fakeDao.insertOrUpdateWallet(oldWallet)

        val retrieved = repository.getWallet()
        assertEquals(0L, retrieved.availableSeconds)
        assertEquals(0L, retrieved.earnedTodaySeconds)
        assertEquals(0L, retrieved.spentTodaySeconds)
        assertEquals(0L, retrieved.productiveStudySecondsToday)
        assertFalse(retrieved.emergencyUnlockUsedToday)
    }

    @Test
    fun testAddProductiveStudySeconds() = runBlocking {
        repository.addProductiveStudySeconds(300L)
        assertEquals(300L, repository.getWallet().productiveStudySecondsToday)

        repository.addProductiveStudySeconds(200L)
        assertEquals(500L, repository.getWallet().productiveStudySecondsToday)
    }

    @Test
    fun testAddEarnedSeconds() = runBlocking {
        repository.addEarnedSeconds(600L)
        val wallet = repository.getWallet()
        assertEquals(600L, wallet.availableSeconds)
        assertEquals(600L, wallet.earnedTodaySeconds)

        repository.addEarnedSeconds(400L)
        val updated = repository.getWallet()
        assertEquals(1000L, updated.availableSeconds)
        assertEquals(1000L, updated.earnedTodaySeconds)
    }

    @Test
    fun testDeductSpentSeconds_normalDeduction() = runBlocking {
        repository.addEarnedSeconds(1000L)
        repository.deductSpentSeconds(300L)

        val wallet = repository.getWallet()
        assertEquals(700L, wallet.availableSeconds)
        assertEquals(300L, wallet.spentTodaySeconds)
    }

    @Test
    fun testDeductSpentSeconds_cannotDropBelowZero() = runBlocking {
        repository.addEarnedSeconds(200L)
        repository.deductSpentSeconds(500L)

        val wallet = repository.getWallet()
        assertEquals(0L, wallet.availableSeconds)
        assertEquals(500L, wallet.spentTodaySeconds)
    }

    @Test
    fun testRecordEmergencyUnlockUsed() = runBlocking {
        repository.recordEmergencyUnlockUsed(900L)

        val wallet = repository.getWallet()
        assertEquals(900L, wallet.availableSeconds)
        assertEquals(900L, wallet.earnedTodaySeconds)
        assertTrue(wallet.emergencyUnlockUsedToday)
    }

    @Test
    fun testSetProActive() = runBlocking {
        assertFalse(repository.getWallet().isProActive)

        repository.setProActive(true)
        assertTrue(repository.getWallet().isProActive)

        repository.setProActive(false)
        assertFalse(repository.getWallet().isProActive)
    }

    @Test
    fun testSetUnfreezeExpiration() = runBlocking {
        val expiry = System.currentTimeMillis() + 300_000L
        repository.setUnfreezeExpiration(expiry)

        val wallet = repository.getWallet()
        assertEquals(expiry, wallet.unfreezeExpirationTimestamp)
        assertTrue(wallet.isUnfrozen)
    }

    @Test
    fun testSetBoost() = runBlocking {
        val expiry = System.currentTimeMillis() + 300_000L
        repository.setBoost(2.5f, expiry)

        val wallet = repository.getWallet()
        assertEquals(2.5f, wallet.boostMultiplier, 0.001f)
        assertEquals(expiry, wallet.boostExpirationTimestamp)
        assertTrue(wallet.isBoostActive)
    }

    @Test
    fun testPerformMidnightReset() = runBlocking {
        repository.addEarnedSeconds(1200L)
        repository.addProductiveStudySeconds(1800L)
        repository.deductSpentSeconds(400L)
        repository.recordEmergencyUnlockUsed()

        repository.performMidnightReset()

        val resetWallet = repository.getWallet()
        assertEquals(0L, resetWallet.availableSeconds)
        assertEquals(0L, resetWallet.earnedTodaySeconds)
        assertEquals(0L, resetWallet.spentTodaySeconds)
        assertEquals(0L, resetWallet.productiveStudySecondsToday)
        assertFalse(resetWallet.emergencyUnlockUsedToday)
    }
}
