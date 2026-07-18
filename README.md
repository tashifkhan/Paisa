# Paisa

Offline-first personal finance app for Android, written in **Kotlin + Jetpack Compose**.

Structure: Room data layer, bottom navigation shell, and Material 3 UI. Bank SMS parsing is powered by a vendored copy of [PennyWise AI](https://github.com/sarim2000/pennywiseai-tracker)’s `parser-core` (~140 banks), all on-device.

---

## How to run

### Prerequisites

| Tool | Notes |
| --- | --- |
| **JDK 17** | Temurin / Oracle / Android Studio bundled JBR all work |
| **Android Studio** | Ladybug / Meerkat or newer recommended (AGP 8.9) |
| **Android SDK** | API **35** platform + build-tools (Studio installs these on first open) |
| **Device or emulator** | Min API **24**. For SMS import you need a **physical phone** with real bank SMS |

Confirm tools:

```bash
java -version          # should report 17+
echo $ANDROID_HOME     # or ANDROID_SDK_ROOT
```

### 1. Clone the repo

```bash
git clone <your-repo-url> Paisa
cd Paisa
```

### 2. Point Gradle at your Android SDK

`local.properties` is gitignored. Create it once:

```bash
# macOS default SDK path
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# Linux example
# echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

Android Studio also writes this file automatically when you open the project.

### 3. Run from Android Studio (recommended)

1. **File → Open** the project root (`Paisa/`, not a subfolder).
2. Wait for Gradle sync to finish.
3. Select the **`app`** run configuration and a connected device/emulator.
4. Click **Run** (▶) or press `Ctrl+R` / `⌃R`.

First sync downloads the Gradle wrapper distribution and dependencies; that can take a few minutes.

### 4. Run from the command line

```bash
# Debug APK
./gradlew :app:assembleDebug

# Install on the only connected device/emulator
./gradlew :app:installDebug

# Or build + install in one step
./gradlew :app:installDebug && adb shell am start -n codes.tashif.paisa/.MainActivity
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install manually:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 5. SMS import (optional, physical device)

SMS features need runtime permissions and an inbox with bank messages:

1. Open the app → **More → SMS import**.
2. Tap **Grant SMS permission & scan**.
3. Allow **Read SMS** (and **Receive SMS** for live alerts).
4. Run **Full rescan** once, then **Scan new** for incremental imports.

Notes:

- Parsing is **100% on-device** — no SMS leaves the phone.
- Emulators usually have no bank SMS; use a real device for this flow.
- Live capture uses `SmsBroadcastReceiver` when a new bank SMS arrives.

### Troubleshooting

| Problem | Fix |
| --- | --- |
| `SDK location not found` | Create `local.properties` with `sdk.dir=...` |
| Gradle sync fails on JDK | Use JDK 17 (Android Studio → Settings → Build → Gradle JDK) |
| `BUILD FAILED` after pull | `./gradlew clean :app:assembleDebug` |
| SMS scan does nothing | Check Settings → Apps → Paisa → Permissions → SMS |
| App already installed with different signature | `adb uninstall codes.tashif.paisa` then reinstall |

---

## Stack

| Layer | Choice |
| --- | --- |
| UI | Jetpack Compose, Material 3 |
| Architecture | `:app` + `:parser-core`, ViewModel + Repository |
| Storage | Room (SQLite), offline-first |
| SMS | `READ_SMS` / `RECEIVE_SMS`, WorkManager bulk scan, live broadcast receiver |
| Parsers | Vendored PennyWise `parser-core` (~140 banks, on-device only) |
| Language | Kotlin |
| Min / Target SDK | 24 / 35 |

---

## Project layout

```text
app/src/main/java/codes/tashif/paisa/
├── MainActivity.kt
├── data/                    # Room entities, DAOs, repository, ViewModel
├── sms/
│   ├── SmsTransactionProcessor.kt  # parse + store + dedup + categorize
│   ├── SmsReaderWorker.kt          # bulk inbox scan
│   ├── SmsBroadcastReceiver.kt     # real-time SMS
│   └── CategoryMapping.kt          # keyword merchant → category
├── ai/
│   ├── AiCredentialsStore.kt       # encrypted BYOK keys
│   ├── LlmClient.kt                # OpenAI-compat / Gemini / Anthropic
│   ├── StatementExtractionService.kt
│   └── StatementImportUseCase.kt
├── security/                # biometric / credential app lock
└── ui/                      # theme, components, screens
parser-core/                 # bank SMS parsers (AGPL, from PennyWise)
```

### SMS pipeline

1. User grants `READ_SMS` / `RECEIVE_SMS` (**More → SMS import**).
2. **Bulk scan** (`SmsReaderWorker`) reads the inbox, runs `BankParserFactory`, inserts with hash dedup.
3. **Live** (`SmsBroadcastReceiver`) handles new bank alerts as they arrive.
4. Unknown financial-looking SMS land in `unrecognized_sms` for review.
5. Soft-delete preserves hash so rescans never re-import removed transactions.

---

## Tabs

1. **Home** — balance card, filters, and recent transactions  
2. **Analytics** — spend charts and period breakdowns  
3. **Budgets** — monthly budgets by category  
4. **Accounts** — wallets / bank accounts (reorder, default, merge)  
5. **More** — SMS import, AI statements, appearance, categories, lock  

FAB on Home opens the add-transaction sheet.

---

## Useful Gradle tasks

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # release APK (unsigned unless you configure signing)
./gradlew :app:installDebug       # install debug build
./gradlew :parser-core:compileKotlin
./gradlew clean
```

---

## Reference clones (local only)

```bash
mkdir -p reference
git clone --depth 1 https://github.com/sarim2000/pennywiseai-tracker.git reference/pennywiseai-tracker
```

`reference/` is listed in `.gitignore` and must not be committed.

---

## Auto-categorization

SMS (and statement) imports assign categories in this order:

1. **Merchant rules** — learned when you change a category and keep “Always use for this merchant”
2. **Keyword map** — built-in merchant dictionary (`CategoryMapping`, PennyWise-style)
3. **Others** — fallback

Manage rules under **More → Merchant rules**. Edit any transaction from Home to recategorize.

## AI statement import (optional, BYOK)

SMS never leaves the device. For bank PDF/CSV/image statements you can optionally:

1. **More → AI provider** — paste an API key for OpenAI-compatible, Gemini, or Anthropic  
2. **More → Import statement** — pick a file, review extracted rows, import  

Keys are stored in EncryptedSharedPreferences. Extracted file text, or the selected image, is sent
only to the endpoint you configure.

---

## Roadmap (near term)

- [x] SMS import (bulk scan + live receiver + dedup)  
- [x] Auto-categorization + merchant learning  
- [x] BYOK AI statement import (OpenAI-compatible / Gemini / Anthropic)  
- [x] Accounts / categories / budgets CRUD  
- [x] Analytics charts and date filters  
- [x] Search + transaction filters  
- [x] App lock / biometrics  
- [ ] Export (CSV / OFX)  
- [ ] Recurring transactions UI  


---

## License

**Paisa** is free software licensed under the  
[GNU Affero General Public License v3.0](LICENSE) (**AGPL-3.0**).

```text
Copyright (c) 2026 Tashif Ahmad Khan
```

You may redistribute and/or modify it under the AGPL-3.0. See [LICENSE](LICENSE) for the full text and [COPYRIGHT](COPYRIGHT) for the short notice.

### Third-party

| Component | Source | License |
| --- | --- | --- |
| `parser-core/` (bank SMS parsers) | [PennyWise AI Tracker](https://github.com/sarim2000/pennywiseai-tracker) | AGPL-3.0 — see [parser-core/NOTICE](parser-core/NOTICE) |

Because Paisa includes AGPL-licensed parser code, the combined work is distributed under AGPL-3.0. If you modify and offer the app as a network service, AGPL requires you to make the corresponding source available to users.
