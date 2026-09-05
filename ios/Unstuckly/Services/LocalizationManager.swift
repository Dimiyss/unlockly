import SwiftUI

public enum AppLanguage: String, CaseIterable, Identifiable {
    case system = "system"
    case en = "en"
    case de = "de"
    case uk = "uk"
    case es = "es"
    case pt = "pt"

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .system: return "System Default"
        case .en: return "English"
        case .de: return "Deutsch (DE)"
        case .uk: return "Українська (UA)"
        case .es: return "Español (ESP)"
        case .pt: return "Português (POR)"
        }
    }

    public var flag: String {
        switch self {
        case .system: return "🌐"
        case .en: return "🇬🇧"
        case .de: return "🇩🇪"
        case .uk: return "🇺🇦"
        case .es: return "🇪🇸"
        case .pt: return "🇵🇹"
        }
    }
}

public class LocalizationManager: ObservableObject {
    public static let shared = LocalizationManager()

    @AppStorage("app_language") public var currentLanguage: AppLanguage = .system {
        didSet {
            objectWillChange.send()
        }
    }

    private let translations: [String: [String: String]] = [
        "app_name": [
            "en": "Unstuckly",
            "de": "Unstuckly",
            "uk": "Unstuckly",
            "es": "Unstuckly",
            "pt": "Unstuckly"
        ],
        "tagline": [
            "en": "Earn Social Time",
            "de": "Verdiene soziale Bildschirmzeit",
            "uk": "Заробляй екранний час",
            "es": "Gana tiempo de pantalla",
            "pt": "Ganhe tempo de tela"
        ],
        "preferences": [
            "en": "Preferences",
            "de": "Einstellungen",
            "uk": "Налаштування",
            "es": "Preferencias",
            "pt": "Preferências"
        ],
        "appearance": [
            "en": "Appearance",
            "de": "Erscheinungsbild",
            "uk": "Тема оформлення",
            "es": "Apariencia",
            "pt": "Aparência"
        ],
        "language": [
            "en": "Language",
            "de": "Sprache",
            "uk": "Мова",
            "es": "Idioma",
            "pt": "Idioma"
        ],
        "social_wallet": [
            "en": "Social Wallet",
            "de": "Social Wallet",
            "uk": "Соціальний гаманець",
            "es": "Billetera Social",
            "pt": "Carteira Social"
        ],
        "available_balance": [
            "en": "Available Balance",
            "de": "Verfügbares Guthaben",
            "uk": "Доступний баланс",
            "es": "Saldo disponible",
            "pt": "Saldo disponível"
        ],
        "minutes_unit": [
            "en": "min",
            "de": "Min.",
            "uk": "хв",
            "es": "min",
            "pt": "min"
        ],
        "daily_cap": [
            "en": "Daily Cap",
            "de": "Tageslimit",
            "uk": "Денний ліміт",
            "es": "Límite diario",
            "pt": "Limite diário"
        ],
        "resets_midnight": [
            "en": "Resets at midnight",
            "de": "Wird um Mitternacht zurückgesetzt",
            "uk": "Скидається опівночі",
            "es": "Se reinicia a medianoche",
            "pt": "Reinicia à meia-noite"
        ],
        "today_activity": [
            "en": "Today's Activity",
            "de": "Heutige Aktivität",
            "uk": "Сьогоднішня активність",
            "es": "Actividad de hoy",
            "pt": "Atividade de hoje"
        ],
        "productive_earned": [
            "en": "Productive Earned",
            "de": "Produktiv verdient",
            "uk": "Зароблено навчанням",
            "es": "Productivo ganado",
            "pt": "Produtivo ganho"
        ],
        "social_consumed": [
            "en": "Social Consumed",
            "de": "Social verbraucht",
            "uk": "Витрачено у соцмережах",
            "es": "Social consumido",
            "pt": "Social consumido"
        ],
        "quick_actions": [
            "en": "Quick Actions",
            "de": "Schnellaktionen",
            "uk": "Швидкі дії",
            "es": "Acciones rápidas",
            "pt": "Ações rápidas"
        ],
        "start_study": [
            "en": "Start Studying",
            "de": "Lernen starten",
            "uk": "Почати навчання",
            "es": "Comenzar a estudiar",
            "pt": "Começar a estudar"
        ],
        "open_social": [
            "en": "Open Social App",
            "de": "Social App öffnen",
            "uk": "Відкрити соцмережу",
            "es": "Abrir app social",
            "pt": "Abrir app social"
        ],
        "active_rule": [
            "en": "Active Rule",
            "de": "Aktive Regel",
            "uk": "Активне правило",
            "es": "Regla activa",
            "pt": "Regra ativa"
        ],
        "no_rule": [
            "en": "No active rule configured",
            "de": "Keine aktive Regel konfiguriert",
            "uk": "Немає налаштованого правила",
            "es": "Sin regla configurada",
            "pt": "Nenhuma regra configurada"
        ],
        "exchange_rate": [
            "en": "Exchange Rate",
            "de": "Wechselkurs",
            "uk": "Курс обміну",
            "es": "Tasa de cambio",
            "pt": "Taxa de conversão"
        ],
        "emergency_override": [
            "en": "Emergency Override",
            "de": "Notfall-Freischaltung",
            "uk": "Екстрене розблокування",
            "es": "Desbloqueo de emergencia",
            "pt": "Desbloqueio de emergência"
        ],
        "close": [
            "en": "Close",
            "de": "Schließen",
            "uk": "Закрити",
            "es": "Cerrar",
            "pt": "Fechar"
        ],
        "pro_title": [
            "en": "Unstuckly PRO",
            "de": "Unstuckly PRO",
            "uk": "Unstuckly PRO",
            "es": "Unstuckly PRO",
            "pt": "Unstuckly PRO"
        ]
    ]

    private init() {}

    public func localizedString(for key: String) -> String {
        let langKey: String
        if currentLanguage == .system {
            let preferred = Locale.preferredLanguages.first ?? "en"
            if preferred.starts(with: "de") { langKey = "de" }
            else if preferred.starts(with: "uk") { langKey = "uk" }
            else if preferred.starts(with: "es") { langKey = "es" }
            else if preferred.starts(with: "pt") { langKey = "pt" }
            else { langKey = "en" }
        } else {
            langKey = currentLanguage.rawValue
        }

        if let dict = translations[key], let val = dict[langKey] {
            return val
        }
        return translations[key]?["en"] ?? key
    }
}

public struct L10n {
    public static func tr(_ key: String) -> String {
        LocalizationManager.shared.localizedString(for: key)
    }
}
