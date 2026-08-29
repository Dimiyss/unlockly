import SwiftUI

struct StoreView: View {
    @ObservedObject var walletManager = WalletManager.shared
    @Environment(\.presentationMode) var presentationMode

    @State private var selectedPlan = 1 // 0: Monthly, 1: Annual

    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        // PRO Hero Card
                        ProHeroCard(
                            isPro: walletManager.wallet.isProActive,
                            selectedPlan: $selectedPlan,
                            onUpgrade: {
                                walletManager.setProActive(true)
                                walletManager.activateBoost(multiplier: 1.5, durationMinutes: 1440 * 30)
                            }
                        )

                        // Power-Ups Section
                        VStack(alignment: .leading, spacing: 14) {
                            Text("⚡ Instant Power-Ups")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                                .padding(.horizontal)

                            PowerUpRow(
                                title: "Mega Boost 2.0x",
                                description: "Double all earned screen time rewards for 24 hours.",
                                price: "$1.99",
                                icon: "bolt.fill",
                                color: .orange,
                                isActive: walletManager.wallet.isBoostActive && walletManager.wallet.boostMultiplier >= 2.0,
                                onActivate: {
                                    walletManager.activateBoost(multiplier: 2.0, durationMinutes: 1440)
                                }
                            )

                            PowerUpRow(
                                title: "1-Hour Unfreeze Pass",
                                description: "Pause all shields and limits completely for 1 hour.",
                                price: "$0.99",
                                icon: "snowflake",
                                color: Color(red: 56/255.0, green: 189/255.0, blue: 248/255.0),
                                isActive: walletManager.wallet.isUnfrozen,
                                onActivate: {
                                    walletManager.activateUnfreeze(durationMinutes: 60)
                                }
                            )

                            PowerUpRow(
                                title: "24-Hour Vacation Pass",
                                description: "Bypass all blocking rules for a full 24-hour break.",
                                price: "$2.99",
                                icon: "beach.umbrella.fill",
                                color: .purple,
                                isActive: walletManager.wallet.isUnfrozen,
                                onActivate: {
                                    walletManager.activateUnfreeze(durationMinutes: 1440)
                                }
                            )
                        }

                        Spacer(minLength: 30)
                    }
                    .padding(.top)
                }
            }
            .navigationTitle("Store & Power-Ups")
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarItems(trailing: Button("Done") {
                presentationMode.wrappedValue.dismiss()
            }.foregroundColor(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0)))
        }
    }
}

struct ProHeroCard: View {
    let isPro: Bool
    @Binding var selectedPlan: Int
    var onUpgrade: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(LinearGradient(colors: [Color.orange, Color.yellow], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 56, height: 56)

                Image(systemName: "crown.fill")
                    .foregroundColor(Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0))
                    .font(.title2)
            }

            Text(isPro ? "You Are a PRO Member!" : "Unlockly PRO")
                .font(.title)
                .fontWeight(.heavy)
                .foregroundColor(.white)

            Text("Maximize your study efficiency with unlimited rules, boosts, and emergency unfreezes.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
                .padding(.horizontal)

            VStack(alignment: .leading, spacing: 8) {
                FeatureBullet(text: "Unlimited customized app rules & schedules")
                FeatureBullet(text: "1.5x Permanent study reward boost")
                FeatureBullet(text: "Emergency unfreeze passes included")
                FeatureBullet(text: "Advanced habit analytics & streaks")
            }
            .padding(.vertical, 8)

            if !isPro {
                HStack(spacing: 12) {
                    PlanButton(
                        title: "Monthly",
                        price: "$4.99 / mo",
                        badge: nil,
                        isSelected: selectedPlan == 0,
                        onSelect: { selectedPlan = 0 }
                    )

                    PlanButton(
                        title: "Annual",
                        price: "$29.99 / yr",
                        badge: "SAVE 50%",
                        isSelected: selectedPlan == 1,
                        onSelect: { selectedPlan = 1 }
                    )
                }
                .padding(.horizontal)

                Button(action: onUpgrade) {
                    HStack {
                        Image(systemName: "star.fill")
                        Text(selectedPlan == 1 ? "Start Annual Plan ($29.99)" : "Start Monthly ($4.99)")
                            .fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0))
                    .foregroundColor(.white)
                    .cornerRadius(14)
                }
                .padding(.horizontal)
            } else {
                HStack {
                    Image(systemName: "checkmark.seal.fill")
                    Text("PRO Subscription Active")
                        .fontWeight(.bold)
                }
                .foregroundColor(.green)
                .padding()
                .frame(maxWidth: .infinity)
                .background(Color.green.opacity(0.15))
                .cornerRadius(12)
                .padding(.horizontal)
            }
        }
        .padding(20)
        .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
        .cornerRadius(24)
        .padding(.horizontal)
    }
}

struct FeatureBullet: View {
    let text: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(.green)
                .font(.subheadline)
            Text(text)
                .font(.subheadline)
                .foregroundColor(.white)
        }
    }
}

struct PlanButton: View {
    let title: String
    let price: String
    let badge: String?
    let isSelected: Bool
    var onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            VStack(spacing: 4) {
                if let badge = badge {
                    Text(badge)
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.orange)
                        .cornerRadius(4)
                } else {
                    Spacer().frame(height: 15)
                }

                Text(title)
                    .font(.caption)
                    .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))

                Text(price)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            .frame(maxWidth: .infinity)
            .padding(12)
            .background(isSelected ? Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0).opacity(0.2) : Color(red: 15/255.0, green: 23/255.0, blue: 42/255.0))
            .cornerRadius(14)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(isSelected ? Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0) : Color(red: 51/255.0, green: 65/255.0, blue: 85/255.0), lineWidth: isSelected ? 2 : 1)
            )
        }
    }
}

struct PowerUpRow: View {
    let title: String
    let description: String
    let price: String
    let icon: String
    let color: Color
    let isActive: Bool
    var onActivate: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(color.opacity(0.15))
                    .frame(width: 44, height: 44)

                Image(systemName: icon)
                    .foregroundColor(color)
                    .font(.title3)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)

                Text(description)
                    .font(.caption)
                    .foregroundColor(Color(red: 148/255.0, green: 163/255.0, blue: 184/255.0))
            }

            Spacer()

            Button(action: onActivate) {
                Text(isActive ? "Active" : price)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(isActive ? Color.green : color)
                    .cornerRadius(10)
            }
        }
        .padding(14)
        .background(Color(red: 30/255.0, green: 41/255.0, blue: 59/255.0))
        .cornerRadius(16)
        .padding(.horizontal)
    }
}
