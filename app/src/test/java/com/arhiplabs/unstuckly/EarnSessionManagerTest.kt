package com.arhiplabs.unstuckly

import com.arhiplabs.unstuckly.data.model.Rule
import com.arhiplabs.unstuckly.data.model.Wallet
import com.arhiplabs.unstuckly.data.repository.RuleRepository
import com.arhiplabs.unstuckly.data.repository.WalletRepository
import com.arhiplabs.unstuckly.domain.EarnSessionManager
import com.arhiplabs.unstuckly.domain.RuleEngine
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
    fun testTick_initialStudy_accumulatesStudySecondsWithoutReleasingRewardBeforeMilestone() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage),
            productiveMinutesTarget = 30, // 1800 seconds target
            rewardMinutes = 20 // 1200 seconds reward
        )
        fakeRuleDao.insertRule(rule)

        // Simulate 60 ticks (1 minute) of study in a productive app (Falou / Duolingo)
        repeat(60) {
            earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)
        }

        assertTrue(earnSessionManager.isAccruing.value)
        assertEquals(testProductivePackage, earnSessionManager.activeProductivePackage.value)

        val wallet = walletManager.getWallet()
        // Productive study time increases, but social reward tokens remain 0 until 30m milestone
        assertEquals(60L, wallet.productiveStudySecondsToday)
        assertEquals(0L, wallet.availableSeconds)
        assertEquals(0L, wallet.earnedTodaySeconds)
        assertFalse(RuleEngine.isInitialStudyTargetMet(wallet.productiveStudySecondsToday, rule.productiveMinutesTarget))
        assertTrue(RuleEngine.shouldBlockApp(
            productiveStudySecondsToday = wallet.productiveStudySecondsToday,
            productiveMinutesTarget = rule.productiveMinutesTarget,
            availableSeconds = wallet.availableSeconds,
            emergencyUnlockUsedToday = wallet.emergencyUnlockUsedToday,
            isUnfrozen = wallet.isUnfrozen
        ))
    }

    @Test
    fun testTick_reachingInitialMilestoneThreshold_depositsFullInitialRewardAndUnlocksBlockedApps() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage),
            productiveMinutesTarget = 30, // 1800 seconds
            rewardMinutes = 20 // 1200 seconds
        )
        fakeRuleDao.insertRule(rule)

        // Set wallet to 1 second before initial target completion (1799s)
        fakeWalletDao.insertOrUpdateWallet(
            Wallet(
                productiveStudySecondsToday = 1799L,
                availableSeconds = 0L,
                earnedTodaySeconds = 0L
            )
        )

        // 1 tick hits the 1800s milestone!
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)

        val wallet = walletManager.getWallet()
        assertEquals(1800L, wallet.productiveStudySecondsToday)
        assertEquals(1200L, wallet.availableSeconds) // 20m reward deposited!
        assertEquals(1200L, wallet.earnedTodaySeconds)
        assertTrue(RuleEngine.isInitialStudyTargetMet(wallet.productiveStudySecondsToday, rule.productiveMinutesTarget))
        assertFalse(RuleEngine.shouldBlockApp(
            productiveStudySecondsToday = wallet.productiveStudySecondsToday,
            productiveMinutesTarget = rule.productiveMinutesTarget,
            availableSeconds = wallet.availableSeconds,
            emergencyUnlockUsedToday = wallet.emergencyUnlockUsedToday,
            isUnfrozen = wallet.isUnfrozen
        ))
    }

    @Test
    fun testTick_afterInitialMilestone_subsequentTicksAccrueIncrementally() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage),
            productiveMinutesTarget = 30,
            rewardMinutes = 20 // ratio 2/3
        )
        fakeRuleDao.insertRule(rule)

        // Initial target of 1800s is already achieved
        fakeWalletDao.insertOrUpdateWallet(
            Wallet(
                productiveStudySecondsToday = 1800L,
                availableSeconds = 1200L,
                earnedTodaySeconds = 1200L
            )
        )

        // 3 additional active study ticks: 3 * (2/3) = 2 seconds of incremental reward
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)
        earnSessionManager.tick(testProductivePackage, isScreenInteractive = true)

        val wallet = walletManager.getWallet()
        assertEquals(1803L, wallet.productiveStudySecondsToday)
        assertEquals(1202L, wallet.availableSeconds)
        assertEquals(1202L, wallet.earnedTodaySeconds)
    }

    @Test
    fun testTick_atStartOfDayAfterMidnightReset_locksInitialLimitAgain() = runBlocking {
        val rule = Rule(
            id = 1L,
            productivePackages = listOf(testProductivePackage),
            blockedPackages = listOf(testBlockedPackage),
            productiveMinutesTarget = 30,
            rewardMinutes = 20
        )
        fakeRuleDao.insertRule(rule)

        // User completed target yesterday
        fakeWalletDao.insertOrUpdateWallet(
            Wallet(
                productiveStudySecondsToday = 1800L,
                availableSeconds = 1200L,
                earnedTodaySeconds = 1200L
            )
        )

        // Midnight arrives & resets day
        walletRepository.performMidnightReset()

        val resetWallet = walletManager.getWallet()
        assertEquals(0L, resetWallet.productiveStudySecondsToday)
        assertEquals(0L, resetWallet.availableSeconds)
        assertEquals(0L, resetWallet.earnedTodaySeconds)

        // Social apps are strictly locked again on the new day
        assertFalse(RuleEngine.isInitialStudyTargetMet(resetWallet.productiveStudySecondsToday, rule.productiveMinutesTarget))
        assertTrue(RuleEngine.shouldBlockApp(
            productiveStudySecondsToday = resetWallet.productiveStudySecondsToday,
            productiveMinutesTarget = rule.productiveMinutesTarget,
            availableSeconds = resetWallet.availableSeconds,
            emergencyUnlockUsedToday = resetWallet.emergencyUnlockUsedToday,
            isUnfrozen = resetWallet.isUnfrozen
        ))
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
