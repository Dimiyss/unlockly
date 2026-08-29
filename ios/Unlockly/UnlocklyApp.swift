import SwiftUI

@main
struct UnlocklyApp: App {
    @StateObject private var screenTimeManager = ScreenTimeManager.shared
    @State private var isOnboardingCompleted: Bool = false

    var body: some Scene {
        WindowGroup {
            if !screenTimeManager.isAuthorized || !isOnboardingCompleted {
                OnboardingView(onFinish: {
                    isOnboardingCompleted = true
                })
            } else {
                HomeView()
            }
        }
    }
}
