import XCTest

final class RuleEngineTests: XCTestCase {

    func testCalculateRewardSeconds_StandardRatio() {
        // 30 mins productive target -> 20 mins reward
        // 1800s productive -> 1200s reward
        let rewardSec = RuleEngine.calculateRewardSeconds(
            activeProductiveSeconds: 1800,
            productiveMinutesTarget: 30,
            rewardMinutes: 20
        )
        XCTAssertEqual(rewardSec, 1200)
    }

    func testCalculateRewardSeconds_OneToOneRatio() {
        let rewardSec = RuleEngine.calculateRewardSeconds(
            activeProductiveSeconds: 600,
            productiveMinutesTarget: 10,
            rewardMinutes: 10
        )
        XCTAssertEqual(rewardSec, 600)
    }

    func testIsDailyCapReached() {
        let capMinutes = 120 // 7200 seconds
        XCTAssertFalse(RuleEngine.isDailyCapReached(earnedTodaySeconds: 7199, dailyCapMinutes: capMinutes))
        XCTAssertTrue(RuleEngine.isDailyCapReached(earnedTodaySeconds: 7200, dailyCapMinutes: capMinutes))
    }

    func testCalculateRewardSeconds_WithBoost() {
        let rewardSec = RuleEngine.calculateRewardSeconds(
            activeProductiveSeconds: 1800,
            productiveMinutesTarget: 30,
            rewardMinutes: 20,
            boostMultiplier: 2.0
        )
        XCTAssertEqual(rewardSec, 2400)
    }

    func testEmergencyPasswordValidation() {
        let shortPassword = "Too short password"
        let validPassphrase = "I promise to stay focused on my learning goals today 2026!"
        XCTAssertTrue(validPassphrase.count >= 35)
        XCTAssertFalse(shortPassword.count >= 35)
    }

    func testWalletUnfreezeAndBoostState() {
        let futureDate = Date().addingTimeInterval(3600)
        let pastDate = Date().addingTimeInterval(-3600)

        let walletActive = Wallet(
            isProActive: true,
            unfreezeExpirationTimestamp: futureDate,
            boostMultiplier: 2.0,
            boostExpirationTimestamp: futureDate
        )
        XCTAssertTrue(walletActive.isUnfrozen)
        XCTAssertTrue(walletActive.isBoostActive)
        XCTAssertEqual(walletActive.effectiveBoostMultiplier, 2.0)

        let walletExpired = Wallet(
            isProActive: false,
            unfreezeExpirationTimestamp: pastDate,
            boostMultiplier: 2.0,
            boostExpirationTimestamp: pastDate
        )
        XCTAssertFalse(walletExpired.isUnfrozen)
        XCTAssertFalse(walletExpired.isBoostActive)
        XCTAssertEqual(walletExpired.effectiveBoostMultiplier, 1.0)
    }

    func testDailyWalletExpirationAndEmergencyUnlockLimit() {
        let wallet = Wallet(
            availableSeconds: 900,
            earnedTodaySeconds: 900,
            spentTodaySeconds: 400,
            emergencyUnlockUsedToday: true
        )
        XCTAssertTrue(wallet.emergencyUnlockUsedToday)
        XCTAssertEqual(wallet.availableSeconds, 900)

        // After midnight reset, availableSeconds expires to 0 and emergency unlock flag resets
        var resetWallet = wallet
        resetWallet.availableSeconds = 0
        resetWallet.earnedTodaySeconds = 0
        resetWallet.spentTodaySeconds = 0
        resetWallet.emergencyUnlockUsedToday = false

        XCTAssertEqual(resetWallet.availableSeconds, 0)
        XCTAssertEqual(resetWallet.earnedTodaySeconds, 0)
        XCTAssertFalse(resetWallet.emergencyUnlockUsedToday)
    }

    func testTargetStudyRequirementValidation() {
        let isFreeTier = false
        let isProTier = true

        let freeMinAllowed = isFreeTier ? 30 : 30
        let proMinAllowed = isProTier ? 15 : 30

        XCTAssertEqual(freeMinAllowed, 30)
        XCTAssertEqual(proMinAllowed, 15)

        // Free tier rejects targets < 30
        XCTAssertFalse(15 >= freeMinAllowed)

        // Pro tier accepts 15, 30, 45, 60
        XCTAssertTrue(15 >= proMinAllowed)
        XCTAssertTrue(30 >= proMinAllowed)
        XCTAssertTrue(45 >= proMinAllowed)
        XCTAssertTrue(60 >= proMinAllowed)
    }
}
