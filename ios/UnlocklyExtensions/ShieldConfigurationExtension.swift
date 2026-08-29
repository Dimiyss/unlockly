import ManagedSettings
import ManagedSettingsUI
import UIKit

class UnlocklyShieldConfiguration: ShieldConfigurationDataSource {
    override func configuration(shielding application: Application) -> ShieldConfiguration {
        return ShieldConfiguration(
            backgroundColor: UIColor(red: 15/255.0, green: 23/255.0, blue: 42/255.0, alpha: 1.0),
            icon: UIImage(systemName: "hourglass.bottomhalf.filled"),
            title: ShieldConfiguration.Label(
                text: "Time Limit Reached",
                color: .white
            ),
            subtitle: ShieldConfiguration.Label(
                text: "Your social wallet is empty. Earn time by studying, or enter your 35+ symbol emergency passphrase in Unlockly to override.",
                color: UIColor(red: 148/255.0, green: 163/255.0, blue: 184/255.0, alpha: 1.0)
            ),
            primaryButtonLabel: ShieldConfiguration.Label(
                text: "Open Unlockly",
                color: .white
            ),
            primaryButtonBackgroundColor: UIColor(red: 99/255.0, green: 102/255.0, blue: 241/255.0, alpha: 1.0)
        )
    }
}
