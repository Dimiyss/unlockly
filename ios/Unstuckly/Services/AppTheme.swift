import SwiftUI

public enum AppThemeMode: String, CaseIterable, Identifiable {
    case system = "system"
    case light = "light"
    case dark = "dark"

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }

    public var iconName: String {
        switch self {
        case .system: return "gearshape"
        case .light: return "sun.max.fill"
        case .dark: return "moon.stars.fill"
        }
    }

    public var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

public class ThemeManager: ObservableObject {
    public static let shared = ThemeManager()

    @AppStorage("app_theme_mode") public var mode: AppThemeMode = .system {
        didSet {
            objectWillChange.send()
        }
    }

    private init() {}
}

public extension Color {
    // Dynamic adaptive colors based on user interface style
    static var appBackground: Color {
        Color(UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 15/255.0, green: 23/255.0, blue: 42/255.0, alpha: 1.0) // Slate 900
                : UIColor(red: 248/255.0, green: 250/255.0, blue: 252/255.0, alpha: 1.0) // Slate 50
        })
    }

    static var appSurface: Color {
        Color(UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 30/255.0, green: 41/255.0, blue: 59/255.0, alpha: 1.0) // Slate 800
                : UIColor(red: 255/255.0, green: 255/255.0, blue: 255/255.0, alpha: 1.0) // Pure White
        })
    }

    static var appSurfaceVariant: Color {
        Color(UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 51/255.0, green: 65/255.0, blue: 85/255.0, alpha: 1.0) // Slate 700
                : UIColor(red: 241/255.0, green: 245/255.0, blue: 249/255.0, alpha: 1.0) // Slate 100
        })
    }

    static var appBorder: Color {
        Color(UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 51/255.0, green: 65/255.0, blue: 85/255.0, alpha: 1.0)
                : UIColor(red: 226/255.0, green: 232/255.0, blue: 240/255.0, alpha: 1.0)
        })
    }

    static var appTextPrimary: Color {
        Color(UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 248/255.0, green: 250/255.0, blue: 252/255.0, alpha: 1.0)
                : UIColor(red: 15/255.0, green: 23/255.0, blue: 42/255.0, alpha: 1.0)
        })
    }

    static var appTextSecondary: Color {
        Color(UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 148/255.0, green: 163/255.0, blue: 184/255.0, alpha: 1.0)
                : UIColor(red: 71/255.0, green: 85/255.0, blue: 105/255.0, alpha: 1.0)
        })
    }

    static var appTextMuted: Color {
        Color(UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 100/255.0, green: 116/255.0, blue: 139/255.0, alpha: 1.0)
                : UIColor(red: 148/255.0, green: 163/255.0, blue: 184/255.0, alpha: 1.0)
        })
    }

    // Brand accents
    static let primaryIndigo = Color(red: 99/255.0, green: 102/255.0, blue: 241/255.0)
    static let secondaryPurple = Color(red: 168/255.0, green: 85/255.0, blue: 247/255.0)
    static let accentPink = Color(red: 236/255.0, green: 72/255.0, blue: 153/255.0)
    static let successGreen = Color(red: 16/255.0, green: 185/255.0, blue: 129/255.0)
    static let warningAmber = Color(red: 245/255.0, green: 158/255.0, blue: 11/255.0)
    static let errorRose = Color(red: 244/255.0, green: 63/255.0, blue: 94/255.0)
}
