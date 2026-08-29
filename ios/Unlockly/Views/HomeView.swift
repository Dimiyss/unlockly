import SwiftUI

struct HomeView: View {
    @ObservedObject var walletManager = WalletManager.shared
    @ObservedObject var screenTimeManager = ScreenTimeManager.shared

    @State private var isRuleEditorPresented = false
    @State private var isHealthPresented = false
    @State private var isEmergencyUnlockPresented = false
    @State private var isPasswordGatePresented = false
    @State private var isStorePresented = false

    private func handleEditRulesRequest() {
        if let password = walletManager.activeRule?.emergencyPassword, !password.isEmpty {
            isPasswordGatePresented = true
        } else {
            isRuleEditorPresented = true
        }
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Header
                        HStack {
                            VStack(alignment: .leading) {
                                Text("Unlockly")
                                    .font(.largeTitle)
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                                Text("Earn Social Time")
                                    .font(.subheadline)
                                    .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                            }

                            Spacer()

                            Button(action: { isStorePresented = true }) {
                                Image(systemName: "crown.fill")
                                    .foregroundColor(.orange)
                                    .font(.title2)
                            }

                            Button(action: { isHealthPresented = true }) {
                                Image(systemName: screenTimeManager.isAuthorized ? "checkmark.seal.fill" : "exclamationmark.triangle.fill")
                                    .foregroundColor(screenTimeManager.isAuthorized ? .green : .orange)
                                    .font(.title2)
                            }

                            Button(action: handleEditRulesRequest) {
                                Image(systemName: "slider.horizontal.3")
                                    .foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
                                    .font(.title2)
                            }
                        }
                        .padding(.horizontal)

                        // Wallet Balance Gauge Card
                        WalletGaugeCard(wallet: walletManager.wallet)

                        // Daily Metrics Row
                        HStack(spacing: 16) {
                            MetricBox(
                                title: "Earned Today",
                                value: formatMinutes(walletManager.wallet.earnedTodaySeconds),
                                icon: "arrow.up.right.circle.fill",
                                color: Color(red: 16/255.0, green: 185/255.0, blue: 129/255.0)
                            )

                            MetricBox(
                                title: "Spent Today",
                                value: formatMinutes(walletManager.wallet.spentTodaySeconds),
                                icon: "arrow.down.right.circle.fill",
                                color: Color(red: 244/255.0, green: 63/255.0, blue: 94/255.0)
                            )
                        }
                        .padding(.horizontal)

                        // Store & Power-Ups Promo Card
                        Button(action: { isStorePresented = true }) {
                            HStack {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(LinearGradient(colors: [Color.orange, Color.yellow], startPoint: .topLeading, endPoint: .bottomTrailing))
                                        .frame(width: 44, height: 44)

                                    Image(systemName: "bolt.fill")
                                        .foregroundColor(Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0))
                                        .font(.title3)
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    HStack {
                                        Text("Store & Power-Ups")
                                            .font(.headline)
                                            .fontWeight(.bold)
                                            .foregroundColor(.white)
                                        if walletManager.wallet.isProActive {
                                            Text("PRO")
                                                .font(.system(size: 9, weight: .bold))
                                                .foregroundColor(Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0))
                                                .padding(.horizontal, 4)
                                                .padding(.vertical, 1)
                                                .background(Color.orange)
                                                .cornerRadius(4)
                                        }
                                    }
                                    Text("2.0x Mega Boosts, Unfreeze Passes & PRO")
                                        .font(.caption)
                                        .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                                }

                                Spacer()

                                Text("Buy")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 6)
                                    .background(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
                                    .cornerRadius(10)
                            }
                            .padding(16)
                            .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
                            .cornerRadius(20)
                            .padding(.horizontal)
                        }

                        // Active Rule Summary Card
                        ActiveRuleBox(rule: walletManager.activeRule, onEdit: handleEditRulesRequest)

                        // Emergency Unlock Card
                        Button(action: {
                            isEmergencyUnlockPresented = true
                        }) {
                            HStack {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(walletManager.wallet.emergencyUnlockUsedToday ? Color.gray.opacity(0.15) : Color.orange.opacity(0.15))
                                        .frame(width: 36, height: 36)

                                    Image(systemName: "key.fill")
                                        .foregroundColor(walletManager.wallet.emergencyUnlockUsedToday ? .gray : .orange)
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Emergency Unlock")
                                        .font(.subheadline)
                                        .fontWeight(.bold)
                                        .foregroundColor(walletManager.wallet.emergencyUnlockUsedToday ? .gray : .white)
                                    Text(walletManager.wallet.emergencyUnlockUsedToday ? "Used today (resets at midnight)" : "Override shields with 35+ symbol passphrase (1x / day)")
                                        .font(.caption)
                                        .foregroundColor(walletManager.wallet.emergencyUnlockUsedToday ? .orange : Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                                }

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                            }
                            .padding()
                            .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
                            .cornerRadius(16)
                            .padding(.horizontal)
                        }

                        Spacer()
                    }
                    .padding(.top)
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $isRuleEditorPresented) {
                RuleEditorView()
            }
            .sheet(isPresented: $isHealthPresented) {
                HealthView()
            }
            .sheet(isPresented: $isStorePresented) {
                StoreView()
            }
            .sheet(isPresented: $isEmergencyUnlockPresented) {
                EmergencyUnlockSheet(walletManager: walletManager)
            }
            .sheet(isPresented: $isPasswordGatePresented) {
                PasswordGateSheet(
                    correctPassword: walletManager.activeRule?.emergencyPassword ?? "",
                    onSuccess: {
                        isPasswordGatePresented = false
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                            isRuleEditorPresented = true
                        }
                    }
                )
            }
        }
    }

    private func formatMinutes(_ seconds: Int) -> String {
        let mins = seconds / 60
        return "\(mins)m"
    }
}

struct WalletGaugeCard: View {
    let wallet: Wallet

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "creditcard.fill")
                .font(.title)
                .foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))

            Text("Social Wallet Balance")
                .font(.subheadline)
                .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))

            Text(formatTime(wallet.availableSeconds))
                .font(.system(size: 36, weight: .bold, design: .monospaced))
                .foregroundColor(.white)

            let subtitle = wallet.isUnfrozen ? "❄️ Unfreeze pass active — shields paused" : (wallet.availableSeconds > 0 ? "Social apps unlocked" : "Wallet empty - study to unlock")

            Text(subtitle)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(wallet.isUnfrozen ? Color(red: 56/255.0, green: 189/255.0, blue: 248/255.0) : (wallet.availableSeconds > 0 ? .green : Color(red: 244/255.0, green: 63/255.0, blue: 94/255.0)))
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
        .cornerRadius(24)
        .padding(.horizontal)
    }

    private func formatTime(_ totalSeconds: Int) -> String {
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        return String(format: "%02dh %02dm %02ds", hours, minutes, seconds)
    }
}

struct MetricBox: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                Text(title)
                    .font(.caption)
                    .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
            }

            Text(value)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
        .cornerRadius(16)
    }
}

struct ActiveRuleBox: View {
    let rule: Rule?
    let onEdit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Active Rule & Limits")
                    .font(.headline)
                    .foregroundColor(.white)
                Spacer()
                Button(action: onEdit) {
                    Image(systemName: "pencil")
                        .foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
                }
            }

            HStack {
                Text("Exchange Rate")
                    .font(.subheadline)
                    .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                Spacer()
                Text("\(rule?.productiveMinutesTarget ?? 30)m study ➔ \(rule?.rewardMinutes ?? 20)m social")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
            }

            HStack {
                Text("Productive Apps")
                    .font(.subheadline)
                    .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                Spacer()
                let productiveCount = (rule?.productiveSelection.applicationTokens.count ?? 0) + (rule?.productiveSelection.categoryTokens.count ?? 0)
                Text("\(productiveCount) configured")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
            }

            HStack {
                Text("Blocked Apps")
                    .font(.subheadline)
                    .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                Spacer()
                let blockedCount = (rule?.blockedSelection.applicationTokens.count ?? 0) + (rule?.blockedSelection.categoryTokens.count ?? 0)
                Text("\(blockedCount) shielded")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
            }

            HStack {
                Text("Settings Protection")
                    .font(.subheadline)
                    .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                Spacer()
                let hasPass = !(rule?.emergencyPassword.isEmpty ?? true)
                Text(hasPass ? "Password Protected" : "Unlocked")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(hasPass ? .green : .gray)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
        .cornerRadius(20)
        .padding(.horizontal)
    }
}

struct PasswordGateSheet: View {
    let correctPassword: String
    var onSuccess: () -> Void
    @Environment(\.presentationMode) var presentationMode

    @State private var input = ""
    @State private var isSecured = true
    @State private var errorMessage: String? = nil

    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0)
                    .ignoresSafeArea()

                VStack(spacing: 20) {
                    Image(systemName: "lock.shield.fill")
                        .font(.system(size: 44))
                        .foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
                        .padding(.top)

                    Text("Password Required")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    Text("Enter your emergency passphrase to unlock and edit app settings & rules:")
                        .font(.subheadline)
                        .multilineTextAlignment(.center)
                        .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                        .padding(.horizontal)

                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            if isSecured {
                                SecureField("Enter password...", text: $input)
                                    .foregroundColor(.white)
                            } else {
                                TextField("Enter password...", text: $input)
                                    .foregroundColor(.white)
                            }

                            Button(action: { isSecured.toggle() }) {
                                Image(systemName: isSecured ? "eye.slash" : "eye")
                                    .foregroundColor(.gray)
                            }
                        }
                        .padding()
                        .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
                        .cornerRadius(12)

                        if let err = errorMessage {
                            Text(err)
                                .font(.caption)
                                .foregroundColor(Color(red: 244/255.0, green: 63/255.0, blue: 94/255.0))
                        }
                    }
                    .padding(.horizontal)

                    Button(action: {
                        let entered = input.trimmingCharacters(in: .whitespacesAndNewlines)
                        let stored = correctPassword.trimmingCharacters(in: .whitespacesAndNewlines)
                        if entered == stored {
                            onSuccess()
                        } else {
                            errorMessage = "Incorrect password. Access denied."
                        }
                    }) {
                        Text("Unlock Settings")
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                    .padding(.horizontal)

                    Spacer()
                }
                .padding()
            }
            .navigationBarItems(trailing: Button("Cancel") {
                presentationMode.wrappedValue.dismiss()
            }.foregroundColor(.gray))
        }
    }
}

struct EmergencyUnlockSheet: View {
    @ObservedObject var walletManager: WalletManager
    @Environment(\.presentationMode) var presentationMode

    @State private var input = ""
    @State private var isSecured = true
    @State private var errorMessage: String? = nil
    @State private var isSuccess = false

    private let minLength = 35

    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0)
                    .ignoresSafeArea()

                VStack(spacing: 20) {
                    Image(systemName: "key.fill")
                        .font(.system(size: 44))
                        .foregroundColor(.orange)
                        .padding(.top)

                    Text("Emergency Unlock")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    Text("Type your 35+ symbol emergency passphrase below to unshield apps and receive 15 minutes of emergency access.")
                        .font(.subheadline)
                        .multilineTextAlignment(.center)
                        .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                        .padding(.horizontal)

                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            if isSecured {
                                SecureField("Type 35+ symbol passphrase...", text: $input)
                                    .foregroundColor(.white)
                            } else {
                                TextField("Type 35+ symbol passphrase...", text: $input)
                                    .foregroundColor(.white)
                            }

                            Button(action: { isSecured.toggle() }) {
                                Image(systemName: isSecured ? "eye.slash" : "eye")
                                    .foregroundColor(.gray)
                            }
                        }
                        .padding()
                        .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
                        .cornerRadius(12)

                        HStack {
                            Text("Length: \(input.trimmingCharacters(in: .whitespacesAndNewlines).count)/\(minLength)")
                                .font(.caption)
                                .foregroundColor(input.trimmingCharacters(in: .whitespacesAndNewlines).count >= minLength ? .green : .gray)
                            Spacer()
                        }

                        if let err = errorMessage {
                            Text(err)
                                .font(.caption)
                                .foregroundColor(Color(red: 244/255.0, green: 63/255.0, blue: 94/255.0))
                        }
                    }
                    .padding(.horizontal)

                    if isSuccess {
                        Text("✓ Emergency access granted (+15m)")
                            .foregroundColor(.green)
                            .fontWeight(.bold)
                    }

                    Button(action: {
                        let result = walletManager.unlockWithEmergencyPassword(input)
                        if result.success {
                            isSuccess = true
                            errorMessage = nil
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                                presentationMode.wrappedValue.dismiss()
                            }
                        } else {
                            errorMessage = result.error ?? "Incorrect emergency passphrase."
                        }
                    }) {
                        Text("Confirm Emergency Unlock")
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(input.trimmingCharacters(in: .whitespacesAndNewlines).count >= minLength ? Color.green : Color.gray.opacity(0.3))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                    .disabled(input.trimmingCharacters(in: .whitespacesAndNewlines).count < minLength)
                    .padding(.horizontal)

                    Spacer()
                }
                .padding()
            }
            .navigationBarItems(trailing: Button("Cancel") {
                presentationMode.wrappedValue.dismiss()
            }.foregroundColor(.gray))
        }
    }
}
