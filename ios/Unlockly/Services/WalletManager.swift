import Foundation
import Combine
import FamilyControls
import ManagedSettings

public class WalletManager: ObservableObject {
    public static let shared = WalletManager()

    @Published public private(set) var wallet: Wallet
    @Published public private(set) var activeRule: Rule?

    private let userDefaults: UserDefaults

    public init(userDefaults: UserDefaults = UserDefaults(suiteName: "group.com.antigravity.unlockly") ?? .standard) {
        self.userDefaults = userDefaults
        if let data = userDefaults.data(forKey: "unlockly_wallet"),
           let decoded = try? JSONDecoder().decode(Wallet.self, from: data) {
            self.wallet = decoded
        } else {
            let initial = Wallet()
            self.wallet = initial
            saveWallet(initial, to: userDefaults)
        }

        if let ruleData = userDefaults.data(forKey: "unlockly_active_rule"),
           let decodedRule = try? JSONDecoder().decode(Rule.self, from: ruleData) {
            self.activeRule = decodedRule
        }

        checkAndPerformDayResetIfNeeded()
    }

    public func checkAndPerformDayResetIfNeeded() {
        let calendar = Calendar.current
        if !calendar.isDate(wallet.lastResetAt, inSameDayAs: Date()) {
            performMidnightReset()
        }
    }

    public func saveActiveRule(_ rule: Rule) {
        self.activeRule = rule
        if let encoded = try? JSONEncoder().encode(rule) {
            userDefaults.set(encoded, forKey: "unlockly_active_rule")
        }
    }

    public func addRewardSeconds(_ seconds: Int) {
        checkAndPerformDayResetIfNeeded()
        guard seconds > 0 else { return }
        var updated = wallet
        let boostedSeconds = Int(Float(seconds) * updated.effectiveBoostMultiplier)
        updated.availableSeconds += boostedSeconds
        updated.earnedTodaySeconds += boostedSeconds
        save(updated)
    }

    public func deductSpentSeconds(_ seconds: Int) {
        checkAndPerformDayResetIfNeeded()
        guard seconds > 0 else { return }
        var updated = wallet
        guard !updated.isUnfrozen else { return } // Bypass deduction and shielding if unfrozen
        updated.availableSeconds = max(0, updated.availableSeconds - seconds)
        updated.spentTodaySeconds += seconds
        save(updated)
    }

    public func setProActive(_ isPro: Bool) {
        var updated = wallet
        updated.isProActive = isPro
        save(updated)
    }

    public func activateUnfreeze(durationMinutes: Int) {
        var updated = wallet
        updated.unfreezeExpirationTimestamp = Date().addingTimeInterval(TimeInterval(durationMinutes * 60))
        save(updated)

        // Clear shields during unfreeze
        let store = ManagedSettingsStore()
        store.shield.applications = nil
        store.shield.applicationCategories = nil
    }

    public func activateBoost(multiplier: Float, durationMinutes: Int) {
        var updated = wallet
        updated.boostMultiplier = multiplier
        updated.boostExpirationTimestamp = Date().addingTimeInterval(TimeInterval(durationMinutes * 60))
        save(updated)
    }

    public func unlockWithEmergencyPassword(_ passphrase: String) -> (success: Bool, error: String?) {
        checkAndPerformDayResetIfNeeded()
        guard !wallet.emergencyUnlockUsedToday else {
            return (false, "Emergency unlock has already been used today. Resets at midnight.")
        }

        let trimmed = passphrase.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 35 else {
            return (false, "Passphrase must be at least 35 symbols.")
        }

        if let stored = activeRule?.emergencyPassword, !stored.isEmpty {
            guard stored.trimmingCharacters(in: .whitespacesAndNewlines) == trimmed else {
                return (false, "Incorrect emergency passphrase.")
            }
        }

        // Grant 15 minutes of emergency access and mark as used today
        var updated = wallet
        updated.availableSeconds += 900
        updated.earnedTodaySeconds += 900
        updated.emergencyUnlockUsedToday = true
        save(updated)

        // Clear shields
        let store = ManagedSettingsStore()
        store.shield.applications = nil
        store.shield.applicationCategories = nil

        return (true, nil)
    }

    public func performMidnightReset() {
        var updated = wallet
        updated.availableSeconds = 0 // Expire all wallet at end of day
        updated.earnedTodaySeconds = 0
        updated.spentTodaySeconds = 0
        updated.emergencyUnlockUsedToday = false
        updated.lastResetAt = Date()
        save(updated)
    }

    private func save(_ updatedWallet: Wallet) {
        self.wallet = updatedWallet
        saveWallet(updatedWallet, to: userDefaults)
    }

    private func saveWallet(_ wallet: Wallet, to defaults: UserDefaults) {
        if let encoded = try? JSONEncoder().encode(wallet) {
            defaults.set(encoded, forKey: "unlockly_wallet")
        }
    }
}
