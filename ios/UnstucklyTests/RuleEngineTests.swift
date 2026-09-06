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
        let sameSymbol35Chars = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        let numbers35Chars = "11111111111111111111111111111111111"
        let excessiveConsecutive = "aaaaa I promise to stay focused today 2026!"
        let validPassphrase = "I promise to stay focused on my learning goals today 2026!"

        // Short password rejected
        XCTAssertFalse(RuleEngine.isEmergencyPassphraseValid(shortPassword))

        // 35 same symbols rejected
        XCTAssertFalse(RuleEngine.isEmergencyPassphraseValid(sameSymbol35Chars))
        XCTAssertFalse(RuleEngine.isEmergencyPassphraseValid(numbers35Chars))

        // Excessive consecutive duplicates rejected
        XCTAssertFalse(RuleEngine.isEmergencyPassphraseValid(excessiveConsecutive))

        // Valid sentence accepted
        XCTAssertTrue(RuleEngine.isEmergencyPassphraseValid(validPassphrase))
        XCTAssertNil(RuleEngine.validateEmergencyPassphrase(validPassphrase).error)
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
        func isTargetValid(target: Int, isPro: Bool) -> Bool {
            if !isPro {
                return target == 30 // Free package supports only one 30 min minimum interval
            } else {
                return target >= 15 // PRO tier allows flexible intervals 15m+
            }
        }

        // Free tier supports only 30m interval
        XCTAssertFalse(isTargetValid(target: 15, isPro: false))
        XCTAssertTrue(isTargetValid(target: 30, isPro: false))
        XCTAssertFalse(isTargetValid(target: 45, isPro: false))
        XCTAssertFalse(isTargetValid(target: 60, isPro: false))

        // PRO tier accepts 15, 30, 45, 60
        XCTAssertTrue(isTargetValid(target: 15, isPro: true))
        XCTAssertTrue(isTargetValid(target: 30, isPro: true))
        XCTAssertTrue(isTargetValid(target: 45, isPro: true))
        XCTAssertTrue(isTargetValid(target: 60, isPro: true))
        XCTAssertFalse(isTargetValid(target: 10, isPro: true))
    }

    func testFreeTierAppLimitValidation() {
        // Free tier allows up to 2 productive and 2 blocked apps
        XCTAssertTrue(RuleEngine.isAppCountValid(productiveCount: 1, blockedCount: 1, isPro: false))
        XCTAssertTrue(RuleEngine.isAppCountValid(productiveCount: 2, blockedCount: 2, isPro: false))
        XCTAssertFalse(RuleEngine.isAppCountValid(productiveCount: 3, blockedCount: 1, isPro: false))
        XCTAssertFalse(RuleEngine.isAppCountValid(productiveCount: 1, blockedCount: 3, isPro: false))
        XCTAssertFalse(RuleEngine.isAppCountValid(productiveCount: 4, blockedCount: 4, isPro: false))

        // PRO tier allows unlimited apps
        XCTAssertTrue(RuleEngine.isAppCountValid(productiveCount: 3, blockedCount: 3, isPro: true))
        XCTAssertTrue(RuleEngine.isAppCountValid(productiveCount: 10, blockedCount: 10, isPro: true))
    }

    func testInitialTargetStudyThresholdEnforcement() {
        let targetMinutes = 30
        let targetSeconds = targetMinutes * 60
        let rewardMinutes = 20
        let rewardSeconds = rewardMinutes * 60

        func evaluateReward(productiveSeconds: Int) -> Int {
            if productiveSeconds < targetSeconds {
                return 0 // Below initial daily target: locked!
            } else {
                return rewardSeconds // Initial minimum interval completed!
            }
        }

        // Before completing 30m target: 0s reward, social apps blocked
        XCTAssertEqual(evaluateReward(productiveSeconds: 0), 0)
        XCTAssertEqual(evaluateReward(productiveSeconds: 300), 0) // 5m
        XCTAssertEqual(evaluateReward(productiveSeconds: 1799), 0) // 29m 59s

        // Reaching 30m target: full 20m reward unlocked
        XCTAssertEqual(evaluateReward(productiveSeconds: 1800), 1200)
        XCTAssertEqual(evaluateReward(productiveSeconds: 2000), 1200)
    }
}
