# Paisa

an expense manager that lives on your phone and stays there. no account, no cloud you didn't pick, no "upgrade to premium" popup every third tap.

it reads your bank sms on the device, turns them into transactions, and leaves the rest of your inbox alone. around 140 indian banks are covered, thanks to the parsers from [PennyWise](https://github.com/sarim2000/pennywiseai-tracker).

android first. the same app also runs in the browser, built from the same kotlin code ([see below](#the-web-version)).

## get it

- **f-droid:** add `https://tashif.codes/fdroid/repo?fingerprint=568557c435bdf555725de95de596a90d6fa964d4bf0b71874f254cd0be7e94c9` in Settings → Repositories, refresh, install Paisa. the fingerprint lets f-droid check the repo is really mine
- **apk:** grab it from the [latest release](https://github.com/tashifkhan/Paisa/releases/latest). each apk ships with a `.sha256` next to it if you like checking things

## what it does

- **sms import.** a bulk scan of your inbox, plus a live receiver that catches new bank alerts as they land. every message is hashed, so rescans don't double count, and a deleted transaction stays deleted
- **categories that learn.** your own merchant rules go first, then a keyword map, then "Others". fix a merchant once and it sticks
- **records, analytics and budgets.** search, filters, charts by date range, and monthly caps per category
- **accounts.** wallets and bank accounts you can reorder, merge, set a default on, or correct the balance of by hand when the bank and the app disagree
- **backups** to google drive, nextcloud or a `.paisa` file. set a password and the file is AES-256-GCM encrypted before it goes anywhere
- **ai statement import, if you want it.** bring your own key (openai compatible, gemini or anthropic), then feed it pdf, csv or image statements, several at once. duplicates across files get dropped
- **app lock** with biometrics or your screen lock
- **export** to csv or pdf
- **home screen widgets** for your balance and quick actions
- **material 3 themes** with a few palettes to pick from

## the privacy bit

sms never leave the phone. the database is plain room/sqlite on the device. the only things that go anywhere are the ones you send yourself: a statement to an ai endpoint you configured, or a backup to the place you chose. ai keys live in EncryptedSharedPreferences.

## how sms become transactions

1. **More → SMS import**, grant read and receive sms
2. the bulk scan walks the inbox through `BankParserFactory` and dedups by hash
3. the live receiver handles new alerts, and the app does a quick incremental rescan whenever you open it, in case android delivered something late
4. messages that look financial but that no parser claims go to `unrecognized_sms` instead of vanishing

## run it

you need jdk 17, the android sdk, and a real phone if you want to test sms (emulators have very boring inboxes).

```bash
git clone https://github.com/tashifkhan/Paisa
cd Paisa

# android studio writes this for you when you open the project
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # macOS
# echo "sdk.dir=$HOME/Android/Sdk" > local.properties         # Linux

./gradlew :app:installDebug
```

or open the repo root in android studio and run `app`.

when it breaks:

- `SDK location not found`: you skipped the `local.properties` step
- gradle hates your jdk: set the gradle jdk to 17 in studio
- signature clash on install: `adb uninstall codes.tashif.paisa`
- sms import does nothing: check Permissions → SMS, and use a phone with actual bank messages

## the web version

```bash
./gradlew :app:wasmJsBrowserDevelopmentRun        # dev server
./gradlew :app:wasmJsBrowserDistribution          # static files in app/build/dist/wasmJs/productionExecutable
```

open it with `?demo` on the end of the url to get three months of sample data.

it's the same screens, viewmodels and database as the phone. the database lives in the browser's origin private file system, so nothing leaves the tab. what the web can't do yet:

- read sms. browsers can't, so the sms screen says so
- google drive, nextcloud and sync. backup and restore work with a `.paisa` file you download or pick
- pdf export and pdf statement import. csv export works
- app lock
- most ai providers block browser requests (cors), so statement import with ai may fail until something proxies it
- the aed and sar currency symbols show as boxes, because the web build has no arabic font

## where stuff lives

```text
app/src/
├── commonMain/       everything both platforms share
│   ├── data/         room 3 database, daos, repositories, demo data
│   ├── sync/         change files and merge rules
│   ├── backup/       the .paisa format and its crypto
│   ├── ai/           bring-your-own-key statement import
│   ├── platform/     dates, numbers and a calendar that work everywhere
│   └── ui/           theme, components, every screen and viewmodel
├── androidMain/      sms, widgets, workmanager, biometrics, drive sign-in, pdf
└── wasmJsMain/       the web host, browser storage, file downloads
app/sqlite-worker/    sqlite in a web worker, for room on the web
shared/               the .paisa container, tested on jvm and in node
parser-core/          bank sms parsers, from PennyWise (AGPL)
tools/PORTING.md      how to move android code into commonMain
```

## roadmap

- [ ] recurring transactions
- [ ] better material 3 charts. the current ones work, they're just not pretty
- [x] a web app, from the same compose multiplatform code. see [the web version](#the-web-version) for what's missing
- [ ] ios, which the shared code now makes much shorter work
- [ ] swap paisa's own relayroom code (`ai/provider/RelayRoomStyle.kt` and the relayroom bits in `ai/catalog/`) for the official kotlin client, `sh.taf.relayroom:relayroom-kotlin`

the web version is kotlin multiplatform, not a typescript rewrite. done so far:

1. **ui:** compose multiplatform with a `wasmJs` target. android-only bits like `Context` and intents sit behind `expect`/`actual`
2. **database:** room 3 with a wasm sqlite worker, stored in the browser's origin private file system
3. **exports:** downloads on the web, the system file picker on android

still to do:

- a pwa shell: a manifest so it installs to the home screen, and a service worker so it opens offline
- google drive and nextcloud on the web, then sync
- pdf on the web

## license

AGPL-3.0, see [LICENSE](LICENSE) and [COPYRIGHT](COPYRIGHT).

`parser-core/` comes from [PennyWise AI Tracker](https://github.com/sarim2000/pennywiseai-tracker), which is also AGPL, so the whole app is AGPL-3.0. if you modify paisa and run it as a network service, you have to give your users the source.
