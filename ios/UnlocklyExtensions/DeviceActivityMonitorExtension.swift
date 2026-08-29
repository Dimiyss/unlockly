import DeviceActivity
import ManagedSettings
import Foundation

class UnlocklyDeviceActivityMonitor: DeviceActivityMonitor {
    override func eventDidReachThreshold(_ event: DeviceActivityEvent.Name, activity: DeviceActivityName) {
        super.eventDidReachThreshold(event, activity: activity)

        let walletManager = WalletManager.shared
        // Accrue reward time when productive threshold is reached
        // Example: 20 minutes reward = 1200 seconds
        walletManager.addRewardSeconds(1200)

        // Clear social app shields if balance is positive
        if walletManager.wallet.availableSeconds > 0 {
            let store = ManagedSettingsStore()
            store.shield.applications = nil
            store.shield.applicationCategories = nil
        }
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
        // Perform midnight reset at day end
        WalletManager.shared.performMidnightReset()
    }
}
