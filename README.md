# Paisa

Offline-first personal finance app for Android, rewritten in **Kotlin + Jetpack Compose**.

Structure is intentionally close to [Expenso](https://github.com/darkvortex144/Expenso) so we can grow features quickly: Room data layer, bottom navigation shell, and Material 3 UI. Local clones of Expenso and [PennyWise AI](https://github.com/sarim2000/pennywiseai-tracker) live under `reference/` for study only (gitignored).

## Stack

| Layer | Choice |
| --- | --- |
| UI | Jetpack Compose, Material 3 |
| Architecture | single-module `:app`, ViewModel + Repository |
| Storage | Room (SQLite), offline-first |
| Language | Kotlin |
| Min SDK | 24 |
| Target SDK | 35 |

## Project layout

```
app/src/main/java/com/paisa/app/
├── MainActivity.kt          # edge-to-edge host + bottom nav shell
├── data/
│   ├── Entities.kt          # Account, Category, Transaction, Budget, …
│   ├── Daos.kt
│   ├── AppDatabase.kt       # seeds default accounts/categories/settings
│   ├── Repository.kt        # balance-safe transaction mutations
│   └── PaisaViewModel.kt
└── ui/
    ├── theme/               # colors, typography, spacing
    ├── components/          # EmptyState, TopBar
    └── screens/             # Home, Analytics, Budgets, Accounts, More, Add sheet
```

## Tabs (Expenso-aligned)

1. **Home** — balance card + recent transactions  
2. **Analytics** — charts placeholder  
3. **Budgets** — budget list placeholder  
4. **Accounts** — wallets / bank accounts  
5. **More** — categories, currency, theme toggle  

FAB on Home opens a simple add-transaction sheet.

## Build

```bash
# requires Android SDK (local.properties is gitignored)
./gradlew :app:assembleDebug
```

Open the project root in Android Studio and run the `app` configuration on an emulator or device.

## Reference clones

```bash
# already present locally (not committed)
reference/Expenso
reference/pennywiseai-tracker
```

Use Expenso for UX/data patterns and PennyWise for SMS parsing / on-device AI ideas later.

## Roadmap (near term)

- [ ] Full add/edit forms for accounts, categories, budgets  
- [ ] Analytics charts and date filters  
- [ ] Search + export  
- [ ] Optional SMS import (inspired by PennyWise)  
- [ ] App lock / biometrics  
