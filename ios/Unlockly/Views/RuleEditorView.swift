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

    private var isPasswordValid: Bool {
        passwordLength >= minPasswordLength
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0)
                    .ignoresSafeArea()

                Form {
                    Section(header: Text("App Selections").foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))) {
                        Button(action: { isProductivePickerPresented = true }) {
                            HStack {
                                Text("Target / Productive Apps")
                                Spacer()
                                let count = productiveSelection.applicationTokens.count + productiveSelection.categoryTokens.count
                                Text(count > 0 ? "\(count) selected" : "Choose")
                                    .foregroundColor(Color(red: 16/255.0, green: 185/255.0, blue: 129/255.0))
                            }
                        }
                        .familyActivityPicker(isPresented: $isProductivePickerPresented, selection: $productiveSelection)

                        Button(action: { isBlockedPickerPresented = true }) {
                            HStack {
                                Text("Blocked / Distractor Apps")
                                Spacer()
                                let count = blockedSelection.applicationTokens.count + blockedSelection.categoryTokens.count
                                Text(count > 0 ? "\(count) selected" : "Choose")
                                    .foregroundColor(Color(red: 244/255.0, green: 63/255.0, blue: 94/255.0))
                            }
                        }
                        .familyActivityPicker(isPresented: $isBlockedPickerPresented, selection: $blockedSelection)
                    }

                    Section(
                        header: Text("Exchange Ratio").foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0)),
                        footer: Text(targetError ?? (isPro ? "PRO: 15m, 30m, 45m, 1h target configurations available." : "Free package requires minimum 30m study target. Upgrade to PRO for 15m option."))
                            .foregroundColor(targetError != nil ? .red : .gray)
                    ) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Target Study Time:")
                                .font(.caption)
                                .foregroundColor(.gray)

                            HStack(spacing: 8) {
                                ForEach([15, 30, 45, 60], id: \.self) { mins in
                                    let isSelected = productiveTarget == "\(mins)"
                                    let isLocked = mins == 15 && !isPro

                                    Button(action: {
                                        if isLocked {
                                            targetError = "15m target is a PRO feature. Free tier requires minimum 30m study."
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

                                            if mins == 15 {
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
                            Text("Productive Target (Minutes)")
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
                        footer: Text(isPasswordValid ? "✓ Valid emergency passphrase length" : "Must be at least 35 symbols (\(passwordLength)/35)")
                            .foregroundColor(isPasswordValid ? .green : (passwordLength > 0 ? .orange : .gray))
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
                        let minAllowed = isPro ? 15 : 30
                        if target < minAllowed {
                            targetError = "Free package requires minimum \(minAllowed)m target study time."
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
                    .disabled(!isPasswordValid)
                    .foregroundColor(isPasswordValid ? Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0) : .gray)
                }
            }
        }
    }
}
