import SwiftUI
import FamilyControls

struct OnboardingView: View {
    @ObservedObject var screenTimeManager = ScreenTimeManager.shared
    @ObservedObject var walletManager = WalletManager.shared
    var onFinish: () -> Void

    @State private var currentStep = 0
    @State private var productiveSelection = FamilyActivitySelection()
    @State private var blockedSelection = FamilyActivitySelection()
    @State private var emergencyPassword = ""

    @State private var isProductivePickerPresented = false
    @State private var isBlockedPickerPresented = false

    private let totalSteps = 5
    private let minPasswordLength = 35

    private var isCurrentStepValid: Bool {
        switch currentStep {
        case 1:
            let count = productiveSelection.applicationTokens.count + productiveSelection.categoryTokens.count + productiveSelection.webDomainTokens.count
            return count > 0 && count <= RuleEngine.maxFreeTierAppsPerCategory
        case 2:
            let count = blockedSelection.applicationTokens.count + blockedSelection.categoryTokens.count + blockedSelection.webDomainTokens.count
            return count > 0 && count <= RuleEngine.maxFreeTierAppsPerCategory
        case 3:
            return RuleEngine.isEmergencyPassphraseValid(emergencyPassword)
        default:
            return true
        }
    }

    var body: some View {
        ZStack {
            Color.appBackground
                .ignoresSafeArea()

            VStack(spacing: 24) {
                // Header Progress Dots
                HStack(spacing: 8) {
                    ForEach(0..<totalSteps, id: \.self) { index in
                        Capsule()
                            .fill(index == currentStep ? Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0) : Color.appBorder)
                            .frame(width: index == currentStep ? 24 : 8, height: 6)
                            .animation(.spring(), value: currentStep)
                    }
                }
                .padding(.top, 20)

                Spacer()

                // Step Contents
                switch currentStep {
                case 0:
                    WelcomeStepView()
                case 1:
                    AppSelectionStepView(
                        title: "Target / Productive Apps",
                        subtitle: "Select up to 2 study apps (Quizlet, Duolingo, etc.) that earn you reward time. (Free Limit: 2 apps, PRO: Unlimited)",
                        selection: $productiveSelection,
                        isPickerPresented: $isProductivePickerPresented,
                        accentColor: Color(red: 16/255.0, green: 185/255.0, blue: 129/255.0)
                    )
                case 2:
                    AppSelectionStepView(
                        title: "Blocked / Distractor Apps",
                        subtitle: "Select up to 2 distractor apps (Instagram, TikTok, YouTube, etc.) that consume your wallet. (Free Limit: 2 apps, PRO: Unlimited)",
                        selection: $blockedSelection,
                        isPickerPresented: $isBlockedPickerPresented,
                        accentColor: Color(red: 244/255.0, green: 63/255.0, blue: 94/255.0)
                    )
                case 3:
                    EmergencyPasswordStepView(
                        password: $emergencyPassword,
                        minLength: minPasswordLength
                    )
                case 4:
                    ScreenTimeSetupStepView(screenTimeManager: screenTimeManager)
                default:
                    EmptyView()
                }

                Spacer()

                // Navigation Buttons
                HStack {
                    if currentStep > 0 {
                        Button("Back") {
                            currentStep -= 1
                        }
                        .foregroundColor(Color.appTextSecondary)
                    }

                    Spacer()

                    Button(action: {
                        if currentStep < totalSteps - 1 {
                            currentStep += 1
                        } else {
                            let newRule = Rule(
                                productiveSelection: productiveSelection,
                                blockedSelection: blockedSelection,
                                productiveMinutesTarget: 30,
                                rewardMinutes: 20,
                                dailyCapMinutes: 120,
                                emergencyPassword: emergencyPassword.trimmingCharacters(in: .whitespacesAndNewlines)
                            )
                            walletManager.saveActiveRule(newRule)
                            ScreenTimeManager.shared.startMonitoringProductiveSession(rule: newRule)
                            onFinish()
                        }
                    }) {
                        Text(currentStep == totalSteps - 1 ? "Start Unstuckly" : "Continue")
                            .fontWeight(.bold)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(isCurrentStepValid ? Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0) : Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0).opacity(0.4))
                            .foregroundColor(isCurrentStepValid ? .white : Color.appTextSecondary)
                            .cornerRadius(12)
                    }
                    .disabled(!isCurrentStepValid)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
    }
}

struct WelcomeStepView: View {
    var body: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle()
                    .fill(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0).opacity(0.15))
                    .frame(width: 100, height: 100)

                Image(systemName: "hourglass")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 48, height: 48)
                    .foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
            }

            Text("Earn Your Screen Time")
                .font(.title)
                .fontWeight(.bold)
                .foregroundColor(.appTextPrimary)

            Text("Spend active minutes studying in learning apps to earn screen time for distractor apps. Turn distractor apps into rewards!")
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundColor(Color.appTextSecondary)
                .padding(.horizontal, 32)
        }
    }
}

struct AppSelectionStepView: View {
    let title: String
    let subtitle: String
    @Binding var selection: FamilyActivitySelection
    @Binding var isPickerPresented: Bool
    let accentColor: Color

    private var selectionCount: Int {
        selection.applicationTokens.count + selection.categoryTokens.count + selection.webDomainTokens.count
    }

    var body: some View {
        VStack(spacing: 16) {
            Text(title)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.appTextPrimary)

            Text(subtitle)
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundColor(Color.appTextSecondary)
                .padding(.horizontal, 24)

            Button(action: { isPickerPresented = true }) {
                HStack {
                    Image(systemName: "square.grid.2x2.fill")
                    Text(selectionCount > 0 ? "Edit Apps (\(selectionCount) selected)" : "Choose Apps & Categories")
                        .fontWeight(.bold)
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background(accentColor.opacity(0.2))
                .foregroundColor(accentColor)
                .cornerRadius(14)
                .padding(.horizontal, 32)
            }
            .familyActivityPicker(isPresented: $isPickerPresented, selection: $selection)

            if selectionCount > 0 {
                let appsCount = selection.applicationTokens.count
                let catsCount = selection.categoryTokens.count
                let domainsCount = selection.webDomainTokens.count
                let text = [
                    appsCount > 0 ? "\(appsCount) apps" : nil,
                    catsCount > 0 ? "\(catsCount) categories" : nil,
                    domainsCount > 0 ? "\(domainsCount) web domains" : nil
                ].compactMap { $0 }.joined(separator: ", ")
                Text("\(text.isEmpty ? "\(selectionCount) items" : text) selected")
                    .font(.caption)
                    .foregroundColor(accentColor)
            }
        }
    }
}

struct EmergencyPasswordStepView: View {
    @Binding var password: String
    let minLength: Int

    @State private var isSecured = true

    private var length: Int {
        password.trimmingCharacters(in: .whitespacesAndNewlines).count
    }

    private var validation: PassphraseValidationResult {
        RuleEngine.validateEmergencyPassphrase(password)
    }

    private var isValid: Bool {
        validation.isValid
    }

    var body: some View {
        VStack(spacing: 18) {
            ZStack {
                Circle()
                    .fill(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0).opacity(0.15))
                    .frame(width: 80, height: 80)

                Image(systemName: "key.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 36, height: 36)
                    .foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
            }

            Text("Emergency Passphrase")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.appTextPrimary)

            Text("Create an emergency passphrase of at least 35 symbols. When apps are shielded, typing this phrase provides emergency unlock. Avoid repetitive duplicate symbols.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundColor(Color.appTextSecondary)
                .padding(.horizontal, 24)

            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    if isSecured {
                        SecureField("Min 35 characters...", text: $password)
                            .foregroundColor(.appTextPrimary)
                    } else {
                        TextField("Min 35 characters...", text: $password)
                            .foregroundColor(.appTextPrimary)
                    }

                    Button(action: { isSecured.toggle() }) {
                        Image(systemName: isSecured ? "eye.slash" : "eye")
                            .foregroundColor(Color.appTextSecondary)
                    }
                }
                .padding()
                .background(Color.appSurface)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(isValid ? Color.green : (length > 0 ? Color.orange : Color.appBorder), lineWidth: 1)
                )

                HStack {
                    Text(isValid ? "✓ Passphrase valid" : (validation.error ?? "Requires \(max(0, minLength - length)) more symbols"))
                        .font(.caption)
                        .foregroundColor(isValid ? .green : (length > 0 ? .orange : Color.appTextSecondary))

                    Spacer()

                    Text("\(length) / \(minLength)")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(isValid ? .green : (length > 0 ? .orange : Color.appTextSecondary))
                }
            }
            .padding(.horizontal, 24)
        }
    }
}

struct ScreenTimeSetupStepView: View {
    @ObservedObject var screenTimeManager: ScreenTimeManager

    var body: some View {
        VStack(spacing: 20) {
            Text("Screen Time Permission")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.appTextPrimary)

            Text("Unstuckly uses Apple Screen Time APIs to track study progress and shield social apps.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundColor(Color.appTextSecondary)
                .padding(.horizontal, 24)

            Button(action: {
                Task {
                    await screenTimeManager.requestAuthorization()
                }
            }) {
                HStack {
                    Image(systemName: screenTimeManager.isAuthorized ? "checkmark.circle.fill" : "lock.shield.fill")
                    Text(screenTimeManager.isAuthorized ? "Screen Time Approved" : "Enable Screen Time Access")
                        .fontWeight(.bold)
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background(screenTimeManager.isAuthorized ? Color.green.opacity(0.2) : Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
                .foregroundColor(screenTimeManager.isAuthorized ? .green : .white)
                .cornerRadius(14)
                .padding(.horizontal, 32)
            }
        }
    }
}
