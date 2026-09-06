import Foundation

public struct PassphraseValidationResult {
    public let isValid: Bool
    public let error: String?

    public init(isValid: Bool, error: String?) {
        self.isValid = isValid
        self.error = error
    }
}

public enum RuleEngine {
    public static let minPassphraseLength = 35
    public static let minDistinctChars = 6
    public static let maxConsecutiveIdenticalChars = 4
    public static let maxFreeTierAppsPerCategory = 2

    /// Checks if selected app count respects Free tier limits (max 2 per category) or PRO (unlimited).
    public static func isAppCountValid(productiveCount: Int, blockedCount: Int, isPro: Bool) -> Bool {
        if isPro { return true }
        return productiveCount <= maxFreeTierAppsPerCategory && blockedCount <= maxFreeTierAppsPerCategory
    }

    /// Validates that the emergency passphrase meets length and diversity requirements
    /// and is not composed of duplicates or repetitive symbols (e.g., 35 of the same character).
    public static func validateEmergencyPassphrase(_ passphrase: String) -> PassphraseValidationResult {
        let trimmed = passphrase.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= minPassphraseLength else {
            return PassphraseValidationResult(
                isValid: false,
                error: "Passphrase must be at least \(minPassphraseLength) characters (\(trimmed.count)/\(minPassphraseLength))."
            )
        }

        // Check for duplicate symbols / lack of character diversity (e.g. 35 identical characters)
        let distinctCount = Set(trimmed).count
        guard distinctCount >= minDistinctChars else {
            return PassphraseValidationResult(
                isValid: false,
                error: "Passphrase cannot be composed of repetitive or duplicate characters. Please enter a real sentence with diverse characters."
            )
        }

        // Check max consecutive identical characters (e.g. "aaaaa")
        var consecutiveCount = 1
        var previousChar: Character? = nil
        for char in trimmed {
            if let prev = previousChar, prev == char {
                consecutiveCount += 1
                if consecutiveCount > maxConsecutiveIdenticalChars {
                    return PassphraseValidationResult(
                        isValid: false,
                        error: "Passphrase cannot contain more than \(maxConsecutiveIdenticalChars) identical consecutive characters."
                    )
                }
            } else {
                consecutiveCount = 1
            }
            previousChar = char
        }

        // Check single character frequency (no single character should make up > 40% of the passphrase)
        var charCounts: [Character: Int] = [:]
        for char in trimmed {
            charCounts[char, default: 0] += 1
        }
        let maxCount = charCounts.values.max() ?? 0
        if maxCount > Int(Double(trimmed.count) * 0.4) {
            return PassphraseValidationResult(
                isValid: false,
                error: "Passphrase contains too many repeated instances of a single character."
            )
        }

        return PassphraseValidationResult(isValid = true, error: nil)
    }

    public static func isEmergencyPassphraseValid(_ passphrase: String) -> Bool {
        return validateEmergencyPassphrase(passphrase).isValid
    }

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
