import Foundation
import FamilyControls
import ManagedSettings
import DeviceActivity
import Combine

@MainActor
public class ScreenTimeManager: ObservableObject {
    public static let shared = ScreenTimeManager()

    @Published public private(set) var isAuthorized: Bool = false
    @Published public var selection: FamilyActivitySelection = FamilyActivitySelection()

    private let store = ManagedSettingsStore()
    private let activityCenter = DeviceActivityCenter()

    public init() {
        checkAuthorizationStatus()
    }

    public func requestAuthorization() async {
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            self.isAuthorized = true
        } catch {
            print("Failed to authorize Screen Time: \(error)")
            self.isAuthorized = false
        }
    }

    public func checkAuthorizationStatus() {
        self.isAuthorized = (AuthorizationCenter.shared.authorizationStatus == .approved)
    }

    public func setShieldsEnabled(_ enabled: Bool, for selection: FamilyActivitySelection) {
        if enabled {
            store.shield.applications = selection.applicationTokens
            store.shield.applicationCategories = ShieldSettings.ActivityCategoryPolicy.specific(selection.categoryTokens)
        } else {
            store.shield.applications = nil
            store.shield.applicationCategories = nil
        }
    }

    public func startMonitoringProductiveSession(rule: Rule) {
        let schedule = DeviceActivitySchedule(
            intervalStart: DateComponents(hour: 0, minute: 0),
            intervalEnd: DateComponents(hour: 23, minute: 59),
            repeats: true
        )

        let activityName = DeviceActivityName("com.arhiplabs.unstuckly.productiveTracking")
        let eventName = DeviceActivityEvent.Name("productiveThresholdReached")

        let event = DeviceActivityEvent(
            applications: rule.productiveSelection.applicationTokens,
            categories: rule.productiveSelection.categoryTokens,
            webDomains: rule.productiveSelection.webDomainTokens,
            threshold: DateComponents(minute: rule.productiveMinutesTarget)
        )

        do {
            try activityCenter.startMonitoring(activityName, during: schedule, events: [eventName: event])
        } catch {
            print("Failed to start monitoring: \(error)")
        }
    }

    public func stopMonitoring() {
        activityCenter.stopMonitoring()
    }
}
