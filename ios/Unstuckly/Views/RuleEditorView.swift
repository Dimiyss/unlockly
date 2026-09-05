import SwiftUI
import FamilyControls

struct RuleEditorView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var walletManager = WalletManager.shared

    @State private var productiveTarget = "30"
    @State private var rewardEarned = "20"
    @State private var dailyCap = "120"
    @State private var productiveSelection = FamilyActivitySelection()
    @State private var blockedSelection = FamilyActivitySelection()
    @State private var emergencyPassword = ""

    @State private var isProductivePickerPresented = false
    @State private var isBlockedPickerPresented = false
    @State private var isSecured = true
    @State private var targetError: String? = nil

    private let minPasswordLength = 35

    private var isPro: Bool {
        walletManager.wallet.isProActive
    }

    private var passwordLength: Int {
        emergencyPassword.trimmingCharacters(in: .whitespacesAndNewlines).count
    }

    private var passphraseValidation: PassphraseValidationResult {
        RuleEngine.validateEmergencyPassphrase(emergencyPassword)
    }

    private var isPasswordValid: Bool {
        passwordLength == 0 || passphraseValidation.isValid
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color.appBackground
                    .ignoresSafeArea()

                Form {
                    Section(
                        header: Text(isPro ? "App Selections (PRO: Unlimited)" : "App Selections (Free: Max 2 per Category)").foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
                    ) {
                        Button(action: { isProductivePickerPresented = true }) {
                            HStack {
                                Text("Target / Productive Apps")
                                Spacer()
                                let count = productiveSelection.applicationTokens.count + productiveSelection.categoryTokens.count + productiveSelection.webDomainTokens.count
                                Text(count > 0 ? "\(count) selected" + (isPro ? "" : " (\(count)/2)") : "Choose")
                                    .foregroundColor(Color(red: 16/255.0, green: 185/255.0, blue: 129/255.0))
                            }
                        }
                        .familyActivityPicker(isPresented: $isProductivePickerPresented, selection: $productiveSelection)

                        Button(action: { isBlockedPickerPresented = true }) {
                            HStack {
                                Text("Blocked / Distractor Apps")
                                Spacer()
                                let count = blockedSelection.applicationTokens.count + blockedSelection.categoryTokens.count + blockedSelection.webDomainTokens.count
                                Text(count > 0 ? "\(count) selected" + (isPro ? "" : " (\(count)/2)") : "Choose")
                                    .foregroundColor(Color(red: 244/255.0, green: 63/255.0, blue: 94/255.0))
                            }
                        }
                        .familyActivityPicker(isPresented: $isBlockedPickerPresented, selection: $blockedSelection)
                    }

                    Section(
                        header: Text("Exchange Ratio").foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0)),
                        footer: Text(targetError ?? (isPro ? "PRO: 15m, 30m, 45m, 1h target configurations available." : "Free package supports only 30m target interval and 2 apps per category. Upgrade to PRO for unlimited options."))
                            .foregroundColor(targetError != nil ? .red : .gray)
                    ) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(isPro ? "Target Study Time (PRO Tier):" : "Target Study Time (Free tier: 30m only):")
                                .font(.caption)
                                .foregroundColor(.gray)

                            HStack(spacing: 8) {
                                ForEach([15, 30, 45, 60], id: \.self) { mins in
                                    let isSelected = productiveTarget == "\(mins)"
                                    let isLocked = (mins != 30) && !isPro

                                    Button(action: {
                                        if isLocked {
                                            let label = mins == 60 ? "1h" : "\(mins)m"
                                            targetError = "\(label) target is a PRO feature. Free tier supports only 30m interval."
                                        } else {
                                            targetError = nil
                                            productiveTarget = "\(mins)"
                                        }
                                    }) {
                                        VStack(spacing: 2) {
                                            Text(mins == 60 ? "1h" : "\(mins)m")
                                                .font(.caption)
                                                .fontWeight(.bold)
                                                .foregroundColor(isSelected ? .white : (isLocked ? .gray : .primary))

                                            if mins != 30 {
                                                Text(isPro ? "PRO" : "🔒")
                                                    .font(.system(size: 8, weight: .bold))
                                                    .foregroundColor(isSelected ? .white : .orange)
                                            }
                                        }
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 6)
                                        .background(isSelected ? Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0) : Color.clear)
                                        .cornerRadius(8)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        .padding(.vertical, 4)

                        HStack {
                            Text(isPro ? "Productive Target (Minutes)" : "Productive Target (Free: 30m only)")
                            Spacer()
                            TextField("30", text: $productiveTarget)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing)
                                .onChange(of: productiveTarget) { _ in
                                    targetError = nil
                                }
                        }

                        HStack {
                            Text("Reward Earned (Minutes)")
                            Spacer()
                            TextField("20", text: $rewardEarned)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing)
                        }

                        HStack {
                            Text("Daily Cap Limit (Minutes)")
                            Spacer()
                            TextField("120", text: $dailyCap)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing)
                        }
                    }

                    Section(
                        header: Text("Emergency Passphrase (Min 35 Symbols)").foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0)),
                        footer: Text(passphraseValidation.isValid ? "✓ Valid emergency passphrase" : (passphraseValidation.error ?? "Must be at least 35 symbols (\(passwordLength)/35)"))
                            .foregroundColor(passphraseValidation.isValid ? .green : (passwordLength > 0 ? .orange : .gray))
                    ) {
                        HStack {
                            if isSecured {
                                SecureField("Min 35 characters...", text: $emergencyPassword)
                            } else {
                                TextField("Min 35 characters...", text: $emergencyPassword)
                            }

                            Button(action: { isSecured.toggle() }) {
                                Image(systemName: isSecured ? "eye.slash" : "eye")
                                    .foregroundColor(.gray)
                            }
                        }
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Edit Rule")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if let rule = walletManager.activeRule {
                    self.productiveTarget = "\(rule.productiveMinutesTarget)"
                    self.rewardEarned = "\(rule.rewardMinutes)"
                    self.dailyCap = "\(rule.dailyCapMinutes)"
                    self.productiveSelection = rule.productiveSelection
                    self.blockedSelection = rule.blockedSelection
                    self.emergencyPassword = rule.emergencyPassword
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        let target = Int(productiveTarget) ?? 30
                        if !isPro && target != 30 {
                            targetError = "Free package supports only 30m interval. Upgrade to PRO for 15m, 45m and flexible options."
                            return
                        }
                        if isPro && target < 15 {
                            targetError = "PRO package requires minimum 15m target study time."
                            return
                        }

                        let productiveCount = productiveSelection.applicationTokens.count + productiveSelection.categoryTokens.count + productiveSelection.webDomainTokens.count
                        let blockedCount = blockedSelection.applicationTokens.count + blockedSelection.categoryTokens.count + blockedSelection.webDomainTokens.count
                        if !RuleEngine.isAppCountValid(productiveCount: productiveCount, blockedCount: blockedCount, isPro: isPro) {
                            targetError = "Free package allows up to \(RuleEngine.maxFreeTierAppsPerCategory) apps per category. Upgrade to PRO for unlimited apps."
                            return
                        }

                        let reward = Int(rewardEarned) ?? 20
                        let cap = Int(dailyCap) ?? 120

                        let rule = Rule(
                            productiveSelection: productiveSelection,
                            blockedSelection: blockedSelection,
                            productiveMinutesTarget: target,
                            rewardMinutes: reward,
                            dailyCapMinutes: cap,
                            emergencyPassword: emergencyPassword.trimmingCharacters(in: .whitespacesAndNewlines)
                        )

                        walletManager.saveActiveRule(rule)
                        ScreenTimeManager.shared.startMonitoringProductiveSession(rule: rule)
                        presentationMode.wrappedValue.dismiss()
                    }
                    .disabled(!passphraseValidation.isValid)
                    .foregroundColor(passphraseValidation.isValid ? Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0) : .gray)
                }
            }
        }
    }
}
