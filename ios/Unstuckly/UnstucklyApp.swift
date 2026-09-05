import SwiftUI

@main
struct UnstucklyApp: App {
    @StateObject private var screenTimeManager = ScreenTimeManager.shared
    @StateObject private var themeManager = ThemeManager.shared
    @StateObject private var locManager = LocalizationManager.shared
    @State private var isOnboardingCompleted: Bool = false

    var body: some Scene {
        WindowGroup {
            Group {
                if !screenTimeManager.isAuthorized || !isOnboardingCompleted {
                    OnboardingView(onFinish: {
                        isOnboardingCompleted = true
                    })
                } else {
                    HomeView()
                }
            }
            .preferredColorScheme(themeManager.mode.colorScheme)
            .environmentObject(themeManager)
            .environmentObject(locManager)
        }
    }
}
