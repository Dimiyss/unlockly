# Unstuckly — Earn Your Screen Time ⏳📱

**Unstuckly** is a dual-platform (**Android** & **iOS**) focus and digital wellbeing application built around the **"Earn Social Time"** exchange engine. Users spend active study time in educational/productive apps (e.g., Duolingo, Quizlet, Khan Academy) to earn screen time tokens in a social wallet, which is consumed when using distracting apps (e.g., Instagram, TikTok, YouTube).

When the social wallet runs out, distractor apps are automatically shielded.

---

## 🌟 Key Features

### 1. 🔄 Exchange Rate Engine
- **Study-to-Earn Ratio**: Configurable target ratio (e.g., *30 active minutes of study = 20 minutes of social screen time*).
- **Daily Cap Limits**: Set a daily limit on maximum earnable reward time.
- **Active Interaction Tracking**: Verifies actual user engagement (scrolling, tapping) to ensure study time is active rather than idling with an open screen.

### 2. 🎨 Light & Dark Themes
- Seamless support for **Light Theme**, **Dark Theme**, and **System Default**.
- Rich aesthetic design tokens with tailored contrast ratios across all screens on Android and iOS.

### 3. 🌐 Multilingual Language Options
- Full localized translation support with live in-app language switching:
  - 🇬🇧 **English (EN)**
  - 🇩🇪 **Deutsch (DE)**
  - 🇺🇦 **Українська (UA)**
  - 🇪🇸 **Español (ESP)**
  - 🇵🇹 **Português (POR)**

### 4. 📱 All Available Apps Picker (C1)
- **Device-wide App Discovery**: Automatically scans and lists all installed launcher applications on the device.
- **Target & Blocked App Selection**: Easily configure which apps count as *Productive / Target* study apps and which apps are *Blocked / Distractor* apps.
- **Interactive UI**: Search bar, app icons, select/clear shortcuts, and live selection badges.

### 5. 🔐 35+ Symbol Emergency Passphrase (C2)
- **Intentional Friction**: Users configure an emergency passphrase with a strict minimum length of **35 characters** (e.g., *"I promise to stay focused on my learning goals today 2026!"*).
- **Live Validation & Feedback**: Real-time character counters (`X / 35`), visual strength indicators, and security notes.
- **Emergency Unblocking**: If urgent access is needed when an app is blocked, typing the exact 35+ symbol passphrase grants 15 minutes of emergency access and unshields apps.

### 6. 🛡️ Settings & Rule Modification Password Gate
- **Tamper Resistance**: Accessing **Edit Rules & Settings** is protected behind the emergency password.
- Prevents impulsive rule changes or unblocking distractor apps without typing the password.

### 7. 💎 Pro, Unfreeze & Boost Multiplier Architecture
- **`is_pro_active`**: Boolean synced from StoreKit / Google Play Billing receipts.
- **`earned_wallet_seconds`**: Available focus & social time balance.
- **`unfreeze_expiration_timestamp`**: Epoch timestamp for temporary complete bypass of all blocking rules.
- **`boost_multiplier`**: Reward rate acceleration factor (Default 1.0x, Mega Pack 2.0x).
- **`boost_expiration_timestamp`**: Expiration timestamp for active boost multipliers.

### 8. 🌙 Midnight Wallet Reset
- Automated daily midnight reset worker resetting daily study progress and accrued usage metrics.

---

## 🏗️ Architecture & Technology Stack

### Android (`/app`)
- **Package & Bundle ID**: `com.arhiplabs.unstuckly`
- **UI**: 100% Jetpack Compose with Material 3 Light & Dark Themes and responsive AppPreferences.
- **Localization**: Android resources (`values/`, `values-de/`, `values-uk/`, `values-es/`, `values-pt/`).
- **Persistence**: Room Database (Entity models: `Rule`, `Wallet`, `EarnSession`, `BlockAttempt`).
- **Foreground Tracking**: `UsageStatsManager` with reconciliation fallback.
- **Activity Detection**: `AccessibilityService` (`InteractionTrackerService`) listening to touch and scroll events for idle detection.
- **Enforcement**: `BlockingOverlayManager` (`WindowManager` system overlay) + `BlockingActivity` fallback.
- **Background Tasks**: Android `WorkManager` for daily midnight reset schedules.

### iOS (`/ios`)
- **UI**: 100% SwiftUI with custom adaptive theme matching Light and Dark modes.
- **Localization**: Live `LocalizationManager` and standard `Localizable.strings` (`en`, `de`, `uk`, `es`, `pt`).
- **Screen Time Frameworks**:
  - `FamilyControls`: Individual Screen Time authorization and `FamilyActivityPicker` app selection.
  - `ManagedSettings`: System-level app shielding (`ManagedSettingsStore`).
  - `DeviceActivity`: Productive threshold detection (`DeviceActivityMonitorExtension`).
  - `ManagedSettingsUI`: Custom branded shield configuration (`ShieldConfigurationExtension`).
- **State & Persistence**: App Group `UserDefaults` (`group.com.arhiplabs.unstuckly`) shared across main app and extensions.

---

## 🚀 Getting Started & Installation

### Android Installation

#### Requirements
- Android Studio Jellyfish / Koala or newer
- Android Device or Emulator running Android 10 (API 29) or higher

#### Steps
1. Open the project root in **Android Studio**:
   ```bash
   File > Open... > /path/to/unstuckly
   ```
2. Enable **Developer Options** & **USB Debugging** on your Android phone.
3. *(Xiaomi / MIUI / HyperOS Users)*:
   - Go to **Settings > Additional settings > Developer options**.
   - Turn ON **`USB debugging`**.
   - Turn ON **`Install via USB`** *(requires Mi Account login)*.
4. Select your connected device and click **Run (▶)** (`Shift + F10`).

#### Permissions Setup on First Launch
1. **Usage Access**: Allows detecting active productive/social apps.
2. **Accessibility Service**: Accurately measures user interaction signals.
3. **Display over other apps**: Allows displaying the blocking overlay when the wallet expires.

---

### iOS Installation

#### Requirements
- macOS with Xcode 15+ installed
- Apple Developer Account with Family Controls entitlement

#### Steps
1. Open the Xcode project under `ios/`.
2. Select your development team under **Signing & Capabilities** for both the main target and extensions (`UnstucklyExtensions`).
3. Build and run on an iOS 16+ physical device.

---

## 🧪 Running Tests

### Android Tests
```bash
./gradlew test
```
- Includes [`RuleEngineTest.kt`](app/src/test/java/com/arhiplabs/unstuckly/RuleEngineTest.kt) testing reward ratios, daily cap enforcement, and emergency passphrase validation.

### iOS Tests
- Run tests in Xcode (`Cmd + U`) or using:
```bash
xcodebuild test -project ios/Unstuckly.xcodeproj -scheme UnstucklyTests
```
- Includes [`RuleEngineTests.swift`](ios/UnstucklyTests/RuleEngineTests.swift).

---

## 📁 Repository Structure

```
unstuckly/
├── app/                                  # Android Native Application
│   ├── src/main/java/com/arhiplabs/unstuckly/
│   │   ├── data/                         # Room Entities, DAOs, Repositories, InstalledAppHelper
│   │   ├── domain/                       # RuleEngine, WalletManager, BlockingCoordinator, HealthMonitor
│   │   ├── service/                      # Accessibility & Foreground Tracking Services
│   │   ├── ui/                           # Jetpack Compose Screens, Preferences, Light & Dark Theme
│   │   └── worker/                       # WorkManager Midnight Reset Worker
│   └── src/main/res/                     # Resources & Translations (values, values-de, values-uk, values-es, values-pt)
├── ios/                                  # iOS Native Application
│   ├── Unstuckly/                        # SwiftUI Views, ThemeManager, LocalizationManager, Models, Services
│   ├── UnstucklyExtensions/              # DeviceActivityMonitor & ShieldConfiguration Extensions
│   └── UnstucklyTests/                   # Unit Tests
└── .doc/                                 # MVP Product & Technical Specification Documentation
```

---

## 📄 License
Unstuckly is distributed under the Apache 2.0 License.
