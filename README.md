# Paisa

Offline-first personal finance app for Android, rewritten in **Kotlin + Jetpack Compose**.

Structure is intentionally close to [Expenso](https://github.com/darkvortex144/Expenso) so we can grow features quickly: Room data layer, bottom navigation shell, and Material 3 UI. Local clones of Expenso and [PennyWise AI](https://github.com/sarim2000/pennywiseai-tracker) live under `reference/` for study only (gitignored).

## Stack

| Layer | Choice |
| --- | --- |
| UI | Jetpack Compose, Material 3 |
| Architecture | `:app` + `:parser-core`, ViewModel + Repository |
| Storage | Room (SQLite), offline-first |
| SMS | READ/RECEIVE_SMS, bulk scan via WorkManager, live `SmsBroadcastReceiver` |
| Parsers | Vendored PennyWise `parser-core` (~140 banks, on-device only) |
| Language | Kotlin |
| Min SDK | 24 |
| Target SDK | 35 |

## Project layout

```
app/src/main/java/com/paisa/app/
├── MainActivity.kt
├── data/                    # Room entities/DAOs/repo/ViewModel
├── sms/
│   ├── SmsTransactionProcessor.kt  # parse + store + dedup
│   ├── SmsReaderWorker.kt          # bulk inbox scan
│   ├── SmsBroadcastReceiver.kt     # real-time SMS
│   └── CategoryMapping.kt          # merchant → category
└── ui/
parser-core/                 # bank SMS parsers (AGPL, from PennyWise)
```

### SMS pipeline

1. User grants `READ_SMS` / `RECEIVE_SMS` (More → SMS import)
2. **Bulk scan** (`SmsReaderWorker`) reads inbox, runs `BankParserFactory`, inserts with hash dedup
3. **Live** (`SmsBroadcastReceiver`) handles new bank alerts as they arrive
4. Unknown financial-looking SMS land in `unrecognized_sms` for review
5. Soft-delete preserves hash so rescans never re-import removed txs

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
