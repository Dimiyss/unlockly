import SwiftUI

public struct PreferencesView: View {
    @ObservedObject var themeManager = ThemeManager.shared
    @ObservedObject var locManager = LocalizationManager.shared
    @Environment(\.dismiss) private var dismiss

    public init() {}

    public var body: some View {
        NavigationView {
            ZStack {
                Color.appBackground
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        // Section 1: Appearance / Theme
                        VStack(alignment: .leading, spacing: 14) {
                            HStack(spacing: 8) {
                                Image(systemName: "sun.max.fill")
                                    .foregroundColor(.primaryIndigo)
                                Text(L10n.tr("appearance"))
                                    .font(.headline)
                                    .foregroundColor(.appTextPrimary)
                            }

                            HStack(spacing: 12) {
                                ForEach(AppThemeMode.allCases) { mode in
                                    let isSelected = themeManager.mode == mode
                                    Button(action: {
                                        themeManager.mode = mode
                                    }) {
                                        VStack(spacing: 8) {
                                            Image(systemName: mode.iconName)
                                                .font(.title3)
                                                .foregroundColor(isSelected ? .primaryIndigo : .appTextSecondary)
                                            Text(mode.title)
                                                .font(.footnote)
                                                .fontWeight(isSelected ? .bold : .medium)
                                                .foregroundColor(isSelected ? .primaryIndigo : .appTextPrimary)
                                        }
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 14)
                                        .background(isSelected ? Color.primaryIndigo.opacity(0.15) : Color.appSurface)
                                        .cornerRadius(14)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 14)
                                                .stroke(isSelected ? Color.primaryIndigo : Color.appBorder, lineWidth: isSelected ? 2 : 1)
                                        )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        .padding(18)
                        .background(Color.appSurface)
                        .cornerRadius(20)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(Color.appBorder, lineWidth: 1)
                        )

                        // Section 2: Language Options
                        VStack(alignment: .leading, spacing: 14) {
                            HStack(spacing: 8) {
                                Image(systemName: "globe")
                                    .foregroundColor(.primaryIndigo)
                                Text(L10n.tr("language"))
                                    .font(.headline)
                                    .foregroundColor(.appTextPrimary)
                            }

                            VStack(spacing: 8) {
                                ForEach(AppLanguage.allCases) { lang in
                                    let isSelected = locManager.currentLanguage == lang
                                    Button(action: {
                                        locManager.currentLanguage = lang
                                    }) {
                                        HStack {
                                            Text(lang.flag)
                                                .font(.title3)
                                            Text(lang.displayName)
                                                .font(.subheadline)
                                                .fontWeight(isSelected ? .bold : .medium)
                                                .foregroundColor(isSelected ? .primaryIndigo : .appTextPrimary)
                                            Spacer()
                                            if isSelected {
                                                Image(systemName: "checkmark")
                                                    .font(.footnote.weight(.bold))
                                                    .foregroundColor(.primaryIndigo)
                                            }
                                        }
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 12)
                                        .background(isSelected ? Color.primaryIndigo.opacity(0.12) : Color.appSurfaceVariant.opacity(0.5))
                                        .cornerRadius(12)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 12)
                                                .stroke(isSelected ? Color.primaryIndigo : Color.clear, lineWidth: 1.5)
                                        )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        .padding(18)
                        .background(Color.appSurface)
                        .cornerRadius(20)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(Color.appBorder, lineWidth: 1)
                        )
                    }
                    .padding(20)
                }
            }
            .navigationTitle(L10n.tr("preferences"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(L10n.tr("close")) {
                        dismiss()
                    }
                    .foregroundColor(.primaryIndigo)
                    .fontWeight(.semibold)
                }
            }
        }
    }
}
