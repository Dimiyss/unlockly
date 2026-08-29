import Foundation

public enum RuleEngine {
    /// Calculates earned reward seconds given active productive seconds,
    /// target productive minutes, and reward minutes.
    public static func calculateRewardSeconds(
        activeProductiveSeconds: Int,
        productiveMinutesTarget: Int,
        rewardMinutes: Int,
        boostMultiplier: Float = 1.0
    ) -> Int {
        guard productiveMinutesTarget > 0, rewardMinutes > 0 else { return 0 }
        let ratio = (Double(rewardMinutes) / Double(productiveMinutesTarget)) * Double(boostMultiplier)
        return Int(Double(activeProductiveSeconds) * ratio)
    }

    /// Checks if accrued earned time today exceeds daily cap limit.
    public static func isDailyCapReached(earnedTodaySeconds: Int, dailyCapMinutes: Int) -> Bool {
        guard dailyCapMinutes > 0 else { return false }
        let dailyCapSeconds = dailyCapMinutes * 60
        return earnedTodaySeconds >= dailyCapSeconds
    }
}
