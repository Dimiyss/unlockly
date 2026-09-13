package com.arhiplabs.unstuckly

import com.arhiplabs.unstuckly.data.model.Rule
import com.arhiplabs.unstuckly.data.model.Wallet
import com.arhiplabs.unstuckly.data.repository.RuleRepository
import com.arhiplabs.unstuckly.data.repository.WalletRepository
import com.arhiplabs.unstuckly.domain.EarnSessionManager
import com.arhiplabs.unstuckly.domain.WalletManager
import com.arhiplabs.unstuckly.fakes.FakeRuleDao
import com.arhiplabs.unstuckly.fakes.FakeWalletDao
import com.arhiplabs.unstuckly.service.InteractionTrackerService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EarnSessionManagerTest {

    private lateinit var fakeWalletDao: FakeWalletDao
    private lateinit var fakeRuleDao: FakeRuleDao
    private lateinit var walletRepository: WalletRepository
    private lateinit var ruleRepository: RuleRepository
    private lateinit var walletManager: WalletManager
    private lateinit var earnSessionManager: EarnSessionManager

    private val testProductivePackage = "com.duolingo"
    private val testBlockedPackage = "com.zhiliaoapp.musically"

    @Before
    fun setUp() {
        fakeWalletDao = FakeWalletDao(Wallet())
        fakeRuleDao = FakeRuleDao()
        walletRepository = WalletRepository(fakeWalletDao)
        ruleRepository = RuleRepository(fakeRuleDao)
        walletManager = WalletManager(walletRepository)
        earnSessionManager = EarnSessionManager(walletManager, ruleRepository)

        // Reset interaction timestamp to now
        InteractionTrackerService.updateLastInteractionTimestamp(System.currentTimeMillis())
    }

    @Test
    fun testTick_whenNoRule_doesNotAccrue() = runBlocking {
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)

        assertFalse(earnSessionManager.isAccruing.value)
        assertNull(earnSessionManager.activeProductivePackage.value)
    }

    @Test
    fun testTick_whenNonProductivePackage_doesNotAccrue() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage)
        )
        fakeRuleDao.insertRule(rule)

        earnSessionManager.tick("com.android.settings", isScreenInteractive = true)

        assertFalse(earnSessionManager.isAccruing.value)
        assertNull(earnSessionManager.activeProductivePackage.value)
    }

    @Test
    fun testTick_whenScreenNotInteractive_doesNotAccrue() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage)
        )
        fakeRuleDao.insertRule(rule)

        earnSessionManager.tick(testProductivePackage, isScreenInteractive = false)

        assertFalse(earnSessionManager.isAccruing.value)
        assertEquals(testProductivePackage, earnSessionManager.activeProductivePackage.value)
    }

    @Test
    fun testTick_whenUserIdle_doesNotAccrue() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage)
        )
        fakeRuleDao.insertRule(rule)

        // Simulate idle user: last interaction was 121 seconds ago (> 120s IDLE_THRESHOLD_MS)
        val pastTime = System.currentTimeMillis() - 121_000L
        InteractionTrackerService.updateLastInteractionTimestamp(pastTime)

        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)

        assertFalse(earnSessionManager.isAccruing.value)
        assertEquals(testProductivePackage, earnSessionManager.activeProductivePackage.value)
    }

    @Test
    fun testTick_whenDailyCapReached_doesNotAccrue() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage),
            dailyCapMinutes = 60 // 3600 seconds cap
        )
        fakeRuleDao.insertRule(rule)

        // Set wallet already reaching daily cap
        fakeWalletDao.insertOrUpdateWallet(
            Wallet(earnedTodaySeconds = 3600L)
        )

        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)

        assertFalse(earnSessionManager.isAccruing.value)
        assertEquals(testProductivePackage, earnSessionManager.activeProductivePackage.value)
    }

    @Test
    fun testTick_activeStudy_accumulatesStudySecondsAndIncrementalReward() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage),
            productiveMinutesTarget = 30, // 1800 seconds
            rewardMinutes = 20 // 1200 seconds -> ratio 2/3
        )
        fakeRuleDao.insertRule(rule)

        // 3 active study ticks: 3 * (2/3) = 2 seconds of reward
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)

        assertTrue(earnSessionManager.isAccruing.value)
        assertEquals(testProductivePackage, earnSessionManager.activeProductivePackage.value)

        val wallet = walletManager.getWallet()
        assertEquals(3L, wallet.productiveStudySecondsToday)
        assertEquals(2L, wallet.availableSeconds)
        assertEquals(2L, wallet.earnedTodaySeconds)
    }

    @Test
    fun testTick_oneMinuteLesson_accruesFortySecondsReward() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage),
            productiveMinutesTarget = 30,
            rewardMinutes = 20
        )
        fakeRuleDao.insertRule(rule)

        // Simulate a 1-minute (60 ticks) lesson in a target app like Falou
        repeat(60) {
            earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)
        }

        val wallet = walletManager.getWallet()
        assertEquals(60L, wallet.productiveStudySecondsToday)
        assertEquals(40L, wallet.availableSeconds)
        assertEquals(40L, wallet.earnedTodaySeconds)
    }

    @Test
    fun testTick_subsequentTicks_accrueIncrementalRewards() = runBlocking {
        // Target: 10m (600s), Reward: 10m (600s) -> ratio 1:1
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage),
            productiveMinutesTarget = 10,
            rewardMinutes = 10
        )
        fakeRuleDao.insertRule(rule)

        // Initial target milestone already achieved
        fakeWalletDao.insertOrUpdateWallet(
            Wallet(
                productiveStudySecondsToday = 600L,
                availableSeconds = 600L,
                earnedTodaySeconds = 600L
            )
        )

        // Subsequent tick should incrementally add 1 second (ratio 1.0)
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)

        val wallet = walletManager.getWallet()
        assertEquals(601L, wallet.productiveStudySecondsToday)
        assertEquals(601L, wallet.availableSeconds)
        assertEquals(601L, wallet.earnedTodaySeconds)
    }

    @Test
    fun testTick_withBoost_multipliesIncrementalReward() = runBlocking {
        // Target: 10m (600s), Reward: 10m (600s), Boost: 2.0x -> ratio 2.0
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage),
            productiveMinutesTarget = 10,
            rewardMinutes = 10
        )
        fakeRuleDao.insertRule(rule)

        val futureExpiry = System.currentTimeMillis() + 60_000L
        fakeWalletDao.insertOrUpdateWallet(
            Wallet(
                productiveStudySecondsToday = 600L,
                availableSeconds = 600L,
                earnedTodaySeconds = 600L,
                boostMultiplier = 2.0f,
                boostExpirationTimestamp = futureExpiry
            )
        )

        // One tick with 2.0x boost adds 2 seconds
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)

        val wallet = walletManager.getWallet()
        assertEquals(601L, wallet.productiveStudySecondsToday)
        assertEquals(602L, wallet.availableSeconds)
        assertEquals(602L, wallet.earnedTodaySeconds)
    }
}
