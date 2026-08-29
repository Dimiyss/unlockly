import Foundation

public struct Wallet: Codable {
    public var availableSeconds: Int
    public var earnedTodaySeconds: Int
    public var spentTodaySeconds: Int
    public var isProActive: Bool
    public var unfreezeExpirationTimestamp: Date?
    public var boostMultiplier: Float
    public var boostExpirationTimestamp: Date?
    public var emergencyUnlockUsedToday: Bool
    public var lastResetAt: Date

    public init(
        availableSeconds: Int = 0,
        earnedTodaySeconds: Int = 0,
        spentTodaySeconds: Int = 0,
        isProActive: Bool = false,
        unfreezeExpirationTimestamp: Date? = nil,
        boostMultiplier: Float = 1.0,
        boostExpirationTimestamp: Date? = nil,
        emergencyUnlockUsedToday: Bool = false,
        lastResetAt: Date = Date()
    ) {
        self.availableSeconds = availableSeconds
        self.earnedTodaySeconds = earnedTodaySeconds
        self.spentTodaySeconds = spentTodaySeconds
        self.isProActive = isProActive
        self.unfreezeExpirationTimestamp = unfreezeExpirationTimestamp
        self.boostMultiplier = boostMultiplier
        self.boostExpirationTimestamp = boostExpirationTimestamp
        self.emergencyUnlockUsedToday = emergencyUnlockUsedToday
        self.lastResetAt = lastResetAt
    }

    public var isUnfrozen: Bool {
        if let timestamp = unfreezeExpirationTimestamp {
            return Date() < timestamp
        }
        return false
    }

    public var isBoostActive: Bool {
        if boostMultiplier > 1.0, let timestamp = boostExpirationTimestamp {
            return Date() < timestamp
        }
        return false
    }

    public var effectiveBoostMultiplier: Float {
        isBoostActive ? boostMultiplier : 1.0
    }
}
