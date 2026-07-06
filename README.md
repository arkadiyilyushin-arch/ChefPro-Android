# ChefPro Android

Native Android port of [ChefPro](https://github.com/arkadiyilyushin-arch/ChefPro) — restaurant ERP for chefs and kitchen teams.

[![Android CI](https://github.com/arkadiyilyushin-arch/ChefPro-Android/actions/workflows/android.yml/badge.svg)](https://github.com/arkadiyilyushin-arch/ChefPro-Android/actions/workflows/android.yml)

## Features

- **Техкарты** — recipes, ingredients, allergens, nutrition, versions
- **Склад** — stock, expiry, barcodes, movements
- **Операции** — deliveries, write-offs, shifts, kitchen board, waiter mode
- **Аналитика** — Food Cost, ABC, P&L, menu engineering, breakeven
- **Финансы** — sales, OPEX, budgets, markup calculators
- **Персонал** — employees, schedules, checklists, temperature log
- **Гости** — reservations, loyalty, POS import, digital menu
- **Система** — JSON backup, PDF/CSV export, OCR, home widget
- **Синхронизация** — optional Firebase Firestore multi-device sync

## Download

Latest release: [GitHub Releases](https://github.com/arkadiyilyushin-arch/ChefPro-Android/releases)

| Version | Notes |
|---------|-------|
| v1.1.0 | Photos, tablet layout, Firebase sync UI, release build |
| v1.0.1 | Startup crash fix |

## Demo login

| Employee | PIN |
|----------|-----|
| Иван Петров | `1111` |
| Анна Смирнова | `2222` |
| Олег Иванов | `3333` |
| Мария Кузнецова | `4444` |

## Build

**Requirements:** JDK 17, Android SDK 36

```bash
git clone https://github.com/arkadiyilyushin-arch/ChefPro-Android.git
cd ChefPro-Android
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK (signed)
```

Debug package: `com.chefpro.debug` · Release: `com.chefpro`

## Firebase sync

See [docs/FIREBASE.md](docs/FIREBASE.md).

## Google Play

See [docs/PLAY_STORE.md](docs/PLAY_STORE.md).

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation · DataStore · WorkManager · ML Kit · Firebase Auth/Firestore

## License

Proprietary — ChefPro restaurant management suite.
