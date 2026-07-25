# Paisa

Your money, on your phone. Offline-first personal finance for Android.

Bank SMS? Parsed on-device (~140 banks via a vendored [PennyWise](https://github.com/sarim2000/pennywiseai-tracker) `parser-core`).

**Download**

| | |
| --- | --- |
| **GitHub Release** | [Paisa v1.0.0](https://github.com/tashifkhan/Paisa/releases/tag/v1.0.0) · [APK](https://github.com/tashifkhan/Paisa/releases/download/v1.0.0/Paisa-v1.0.0.apk) |
| **F-Droid repo** | add `https://tashif.codes/fdroid/repo` → install **Paisa** |
| **All releases** | https://github.com/tashifkhan/Paisa/releases |

---

## Stack

| | |
| --- | --- |
| UI | Jetpack Compose, Material 3 |
| Shape | `:app` + `:parser-core`, ViewModel + Repository |
| Storage | Room (SQLite), stays on the device |
| SMS | bulk scan + live receiver, hash dedup |
| Optional AI | BYOK statement import (OpenAI-compat / Gemini / Anthropic) |
| Language | Kotlin · min SDK 24 · target 35 |

## What’s in the box

- **Home** — balance, filters, recent txs  
- **Analytics** — charts that actually chart something  
- **Budgets** — monthly caps by category  
- **Accounts** — wallets / banks (reorder, default, merge)  
- **More** — SMS import, AI statements, themes, merchant rules, app lock  

Plus onboarding, transaction detail, and a FAB that means “I spent money, log it.”

### How SMS lands in the ledger

1. **More → SMS import** → grant Read/Receive SMS
2. Bulk scan walks the inbox through `BankParserFactory`, dedups by hash  
3. Live receiver catches new bank alerts as they arrive  
4. Weird financial-looking SMS that no parser claims go to `unrecognized_sms`  
5. Soft-delete keeps the hash so a rescan won’t resurrect deleted rows  

Categories on import: learned **merchant rules** first → keyword map → **Others**.

### Optional: AI statement import

SMS never leaves the phone. For PDF/CSV/image statements you *can* paste your own API key (**More → AI provider**) and import via **More → Import statement**. Keys live in EncryptedSharedPreferences; only what you send to *your* endpoint leaves the device.

---

## How to run

Need **JDK 17**, Android Studio (or CLI + SDK), and for SMS a **physical phone**.

```bash
git clone <your-repo-url> Paisa
cd Paisa

# once — Studio also writes this when you open the project
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # macOS
# echo "sdk.dir=$HOME/Android/Sdk" > local.properties         # Linux

./gradlew :app:installDebug
# adb shell am start -n codes.tashif.paisa/.MainActivity
```

Or open the **repo root** in Android Studio → run **`app`**.

Stuck?

- `SDK location not found` → `local.properties` with `sdk.dir=...`  
- Gradle hates your JDK → set Gradle JDK to 17 in Studio  
- Signature clash → `adb uninstall codes.tashif.paisa`  
- SMS does nothing → Permissions → SMS, and use a real inbox  

```bash
./gradlew :app:assembleDebug
./gradlew :parser-core:compileKotlin
./gradlew clean
```

---

## Release & F-Droid

Releases are cut from **master** via GitHub Actions → **Release**.

That workflow:

1. Bumps `versionName` / `versionCode` in `app/build.gradle.kts`
2. Builds a **signed** release APK
3. Tags `vX.Y.Z`, publishes a GitHub Release with `Paisa-vX.Y.Z.apk` + SHA-256
4. Optionally dispatches the F-Droid publisher on [`tashif.codes`](https://github.com/tashifkhan/tashif.codes) so the APK lands in:

   ```text
   https://tashif.codes/fdroid/repo
   ```

### Secrets (`tashifkhan/Paisa`)

| Secret | Purpose |
| --- | --- |
| `KEYSTORE_BASE64` | base64 of the APK upload keystore (`.jks` / `.p12`) |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |
| `TASHIF_CODES_WORKFLOW_TOKEN` | fine-grained PAT with **Actions** write on `tashifkhan/tashif.codes` |

Create an upload keystore once and **back it up** — lost keys mean users cannot update:

```bash
keytool -genkeypair \
  -v \
  -keystore paisa-upload.jks \
  -storetype JKS \
  -alias paisa \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

base64 -i paisa-upload.jks | pbcopy   # → KEYSTORE_BASE64
```

### Secrets (`tashifkhan/tashif.codes`)

Repo-signing key for the F-Droid **index** (separate from the APK keystore):

- `FDROID_KEYSTORE_BASE64`, `FDROID_KEYSTORE_PASS`, `FDROID_KEY_ALIAS`, `FDROID_KEY_PASS`

See [public/fdroid/README.md](https://github.com/tashifkhan/tashif.codes/blob/master/public/fdroid/README.md) on the site repo.

### Manual F-Droid re-publish

Actions → **Publish to F-Droid repo** (or run `publish-fdroid.yml` on `tashif.codes` with `source_repository=tashifkhan/Paisa`).

---

## Layout

```
app/src/main/java/codes/tashif/paisa/
├── MainActivity.kt
├── data/          # Room, repo, ViewModel
├── sms/           # scan, live receiver, category map
├── ai/            # BYOK statement import
├── security/      # biometric / credential lock
└── ui/            # theme, components, screens
parser-core/       # bank SMS parsers (AGPL, from PennyWise)
```

---

## Roadmap

### Done / nearly done

- [x] SMS import (bulk + live + dedup)  
- [x] BYOK AI statement import  (Your AI)
- [x] Accounts / categories / budgets CRUD  
- [x] Analytics charts + date filters  
     - [ ] Umm better MD3 charts tho
- [x] Search + transaction filters  
- [x] App lock / biometrics  
- [x] Export (CSV / PDF)  
- [ ] Recurring transactions UI  
- [ ] MAIN THING: web PWA without rewriting in JS (iOS support thus also there - but no SMS parsing coz web & also iOS is a bitch doesn't let you read SMS)

### MAIN THING's plan -

Good news: we don’t need to throw Kotlin away and rebuild Paisa in TypeScript (coZ been there don't wanna go back). **Kotlin Multiplatform + Compose Multiplatform** can compile a lot of this straight to **WebAssembly** and ship it as a PWA.

#### 1. Port the UI — Compose → Compose Multiplatform

Same declarative UI, different Gradle target.

- Swap Android-only `androidx.compose` bits for `org.jetbrains.compose` where needed  
- Add a KMP `wasmJs` target so the UI becomes a Wasm binary in the browser  
- Anything Android-shaped (`Context`, Intents, Android `ViewModel` lifecycle) goes behind `expect` / `actual`  

#### 2. Port the DB — Room Multiplatform + OPFS

Offline-first only works if the web still has a real local store.

- Room KMP (2.6+) instead of Android-only Room  
- Web: Wasm SQLite driver, not the Android factory  
- Persist under the browser’s **Origin Private File System (OPFS)** so SQLite stays fast and local, closer to “file on disk” than “hope localStorage is fine”  

#### 3. PWA shell — installable + offline boot

Once Wasm runs in a tab, wrap it like any web app:

- `index.html` loads the compiled JS + Wasm  
- `manifest.json` — name, icons, theme, `display: standalone` (home-screen install)  
- Service worker (e.g. Workbox) caches shell + Wasm + fonts so cold start works with airplane mode on  

#### 4. Exports without SAF / Intents

Android file pickers don’t exist in the browser. For backups and CSV/JSON dumps, use the **File System Access API** so the user still picks where the file lands — same local-first vibe, different plumbing.

*(SMS import stays Android-only, obviously. Web gets manual entry + statement import + export.)* _so does iOS_

---

## License

**AGPL-3.0** — see [LICENSE](LICENSE) and [COPYRIGHT](COPYRIGHT).

```
Copyright (c) 2026 Tashif Ahmad Khan
```

`parser-core/` comes from [PennyWise AI Tracker](https://github.com/sarim2000/pennywiseai-tracker) (also AGPL). Because that code ships with Paisa, the combined work is AGPL-3.0. If you modify and offer this as a network service, you need to give users the corresponding source.
