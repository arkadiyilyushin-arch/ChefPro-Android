# ChefPro (Android)

ChefPro is a native Android restaurant management app for chefs and kitchen teams. It mirrors the iOS ChefPro feature set: tech cards (recipes), inventory, analytics, shift operations, guest services, and Firebase sync — with all core data stored locally and optional cloud backup.

## Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Navigation Compose** — login → onboarding → 5-tab main shell (Home, Tech Cards, Search, Inventory, More)
- **Room-style local JSON persistence** via `ChefProRepository`
- **Firebase** Auth + Firestore for multi-device sync
- **ML Kit** — text recognition (receipts, invoices, tech cards) and barcode scanning
- **WorkManager** — scheduled notifications (low stock, expiry, HACCP)

## Project structure

```
app/src/main/java/com/chefpro/
├── MainActivity.kt              # Edge-to-edge entry, theme, notification permission
├── ChefProApplication.kt        # Repository, sync, photos, notifications
├── data/                        # Local persistence, demo data, photo storage
├── domain/                      # ChefProEngine (cost, production, analytics)
├── firebase/                    # Firestore sync service
├── model/                       # Serializable domain models
├── notifications/               # NotificationHelper + workers
├── ui/
│   ├── navigation/              # Routes + AppNavigation
│   ├── screens/                 # All feature screens
│   ├── theme/                   # ChefProTheme, colors, typography
│   └── viewmodel/               # ChefProViewModel
├── util/                        # PDF/CSV export, JSON backup, OCR helpers
└── widget/                      # Home-screen widget (low stock + kitchen orders)
```

## Build requirements

- **JDK 17**
- **Android SDK** with `compileSdk 36`, `minSdk 26`
- Android Studio Ladybug or newer (or command line with `local.properties` pointing to your SDK)

## Build & run

```bash
cd ChefPro
./gradlew assembleDebug
```

Install on a device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Lint and unit tests:

```bash
./gradlew lint test
```

## Firebase setup

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an **Android app** with package name `com.chefpro`.
3. Download `google-services.json` and place it at `app/google-services.json` (a placeholder may already exist for local builds).
4. Enable **Authentication** (anonymous or email — the app registers device members via `FirebaseSyncService`).
5. Enable **Cloud Firestore** in production or test mode.
6. (Optional) Enable **Crashlytics** for crash reporting.

Firestore security rules should restrict access to authenticated restaurant members. The app works fully offline when Firebase is unavailable; changes queue until sync succeeds.

## Demo login

On first launch, demo employees are seeded. Select an employee and enter their PIN from `DemoData` (default PINs are defined in the demo employee records).

## Backup & export

- **JSON backup** — full app state via Settings → Backup or CSV export screen
- **CSV** — inventory, write-offs, sales (`util/CsvExport.kt`)
- **PDF/text reports** — summary reports (`util/PdfExport.kt`)
- Shared files use the `FileProvider` authority `${applicationId}.fileprovider`

## Widget

Add the **ChefPro** home-screen widget to see live **low stock** and **active kitchen order** counts. The widget reads from local app state and refreshes periodically.

## License

Proprietary — ChefPro restaurant management suite.
