# Paisa

an expense manager that lives on your phone and stays there. no account, no cloud you didn't pick, no "upgrade to premium" popup every third tap.

it reads your bank sms on the device, turns them into transactions, and leaves the rest of your inbox alone. around 140 indian banks are covered, thanks to the parsers from [PennyWise](https://github.com/sarim2000/pennywiseai-tracker).

android first. the same app also runs in the browser, built from the same kotlin code ([see below](#the-web-version)).

## get it

- **f-droid:** add `https://tashif.codes/fdroid/repo?fingerprint=568557c435bdf555725de95de596a90d6fa964d4bf0b71874f254cd0be7e94c9` in Settings → Repositories, refresh, install Paisa. the fingerprint lets f-droid check the repo is really mine
- **apk:** grab it from the [latest release](https://github.com/tashifkhan/Paisa/releases/latest). each apk ships with a `.sha256` next to it if you like checking things

## what it does

- **sms import.** a bulk scan of your inbox, plus a live receiver that catches new bank alerts as they land. every message is hashed, so rescans don't double count, and a deleted transaction stays deleted
- **payment notifications.** on android, a switch per bank or upi app (google pay, phonepe, hdfc, canara ai1 and more) turns the alerts those apps post into transactions. an add button picks any other installed app. a payment that also arrives as a bank sms is saved once, and the bank's account and balance fill in the row the upi app created
- **categories that learn.** your own merchant rules go first, then a keyword map, then "Others". fix a merchant once and it sticks
- **records, analytics and budgets.** search, filters, charts by date range, and monthly caps per category
- **accounts.** wallets and bank accounts you can reorder, merge, set a default on, or correct the balance of by hand when the bank and the app disagree
- **backups** to google drive, nextcloud or a `.paisa` file. set a password and the file is AES-256-GCM encrypted before it goes anywhere
- **ai statement import, if you want it.** bring your own key (openai compatible, gemini or anthropic), then feed it pdf, csv or image statements, several at once. duplicates across files get dropped
- **add with ai.** in the add sheet, switch to "With AI" and type something like "lunch 240 yesterday, uber 180 today", attach receipts, bills, screenshots or pdfs, or both. you get a list of drafts to edit, ask the ai to change ("the uber one was cash"), then save the ones you want. it uses the same models as statement import, and has its own ladder under More → AI
- **local ai, when your phone has it.** flip on **Use local AI** under More → AI and Gemini Nano runs the text and photo jobs on the device. no key, no per-call bill, no prompt leaving the phone. Pixel 9 and newer support it, as do several recent Samsung, OnePlus, Xiaomi, OPPO and vivo flagships. PDFs and voice notes still need a cloud model ([details](docs/local-ai.md))
- **app lock** with biometrics or your screen lock
- **export** to csv or pdf
- **home screen widgets** for your balance and quick actions
- **material 3 themes** with a few palettes to pick from

## the privacy bit

sms and notification text never leave the phone. the database is plain room/sqlite on the device. the only things that go anywhere are the ones you send yourself: a statement to an ai endpoint you configured, or a backup to the place you chose. with local ai, those prompts stay on the phone too. ai keys live in EncryptedSharedPreferences.

## how sms become transactions

1. **More → SMS import**, grant read and receive sms
2. the bulk scan walks the inbox through `BankParserFactory` and dedups by hash
3. the live receiver handles new alerts, and the app does a quick incremental rescan whenever you open it, in case android delivered something late
4. messages that look financial but that no parser claims go to `unrecognized_sms` instead of vanishing

## how notifications become transactions

1. **More → Payment notifications**, turn it on, then allow Paisa in the system notification access screen
2. only the apps switched on there are read; every other notification is dropped before its text is touched. **Add a bank or UPI app** opens the phone's app list and adds anything missing from the built-in 21
3. a bank app's alert goes through that bank's sms parser first. upi apps (google pay, phonepe and friends), and user-added apps, go through `notification-core`, which reads plus/minus signs, debit/credit words and offers, reminders and payment requests are ignored
4. one payment often arrives three ways: as a upi app notification, a bank sms and a bank app alert. `CrossChannelDedup` compares amount, direction, time, account and upi reference, keeps one row, and lets the bank's message add the account and balance to the row the upi app created
5. **unclear messages.** when the rules can't read an sms or alert, payment notifications → unclear messages picks who tries: off, **on-device** (Gemini Nano, nothing leaves the phone) or **cloud** (the "sms and alerts" model under ai, with a warning that the text goes to that provider, is slower and may cost money). cloud works on every phone and skips gemini nano rungs. Gemini Nano only runs while paisa is open, so on-device reads of background messages wait, and paisa posts **payment waiting for ai**. phones without aicore show on-device greyed out
6. **which account paid.** a upi app can pay from several bank accounts, so paisa picks the account in this order: a link you made for the account digits in the alert, your only account with those digits, the app's default account, then your only account at that bank (bank apps) or your only bank account (upi apps). if none fits, the payment waits under the app's name and paisa asks **which account was this?** pick one from the alert, the transaction, or the payment notifications screen. "always use" makes it the rule and moves the other waiting payments. each app's row in settings shows and changes its default account
7. **merchant renames.** rename an imported transaction ("SWIGGY LTD" to "Swiggy") and turn on **always rename**. past rows are renamed, and future sms, notifications and statements use the new name before the category rules run. renames sync and back up like category rules, and show under more → merchant rules
8. deleting a transaction keeps it deleted: the same notification or sms won't bring it back
9. android 15 and later hide a notification's text from listeners when the system thinks it holds an otp. some bank alerts with an account number trip that check and are skipped; the bank sms still imports the same payment

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
- payment notifications do nothing: More → Payment notifications, turn the master switch on, then allow Paisa under Settings → Notifications → Notification access. the app list shows which supported apps are installed; if android calls the access restricted, allow restricted settings from Paisa's App info menu

## the web version

the whole app also runs in a browser. it's the same kotlin as the phone (compose multiplatform compiled to wasm), so same screens, same database, same sync. nothing gets rewritten in typescript.

```bash
./gradlew :app:wasmJsBrowserDevelopmentRun        # dev server
./gradlew :app:wasmJsBrowserDistribution          # static files in app/build/dist/wasmJs/productionExecutable
```

open it with `?demo` on the end of the url to get three months of sample data. without it you get the first-run setup, same as a fresh install on the phone: currency, theme, then shortcuts to add something or set up backup.

the database is sqlite in a web worker, stored in the browser's origin private file system. nothing leaves the tab unless you turn on backup, sync or ai. it needs a recent chrome, edge, firefox or safari, and no special server headers.

### hosting it

the build is a folder of static files, so any static host works. on vercel:

```bash
./gradlew :app:wasmJsBrowserDistribution
npx vercel deploy app/build/dist/wasmJs/productionExecutable --prod
```

vercel's build machines have no jdk, so build on your machine or in ci and deploy the folder, rather than letting vercel run gradle. cloudflare pages, netlify and github pages work the same way. serve it over https, since the browser only gives origin private storage to secure pages.

### big screens

the layout follows the window, not the platform, so a tablet and a browser get the same thing at the same width:

- **under 600dp (phones):** bottom bar and bottom sheets
- **600 to 840dp (small tablets, phones in landscape):** a side rail
- **840 to 1200dp (tablets in landscape):** a transaction opens beside the list. debts and settings become a list on the left with the open item on the right. analytics goes to two columns, budgets and accounts to grids, and sheets open as centered dialogs
- **1200dp and up (laptops, desktops):** a drawer with every tab, and home's balances move to a pane beside the transactions

in a browser the add and rescan buttons sit at the top of the rail or drawer, near the mouse. on android tablets they stay floating in the corner, near your thumb. a phone browser with "desktop site" turned on still gets the phone layout, because the app sizes itself to the part of the page you can see.

with a keyboard: `n` adds, `/` or `ctrl k` searches, `1` to `6` switch tabs, `esc` closes things and `?` lists all of it. right click a transaction to open or delete it. charts show their tooltip on hover, lists get a scrollbar, and the browser tab title follows the screen.

### backup and sync on the web

backup and sync are the same code as on the phone, so a browser and a phone can share one nextcloud or google drive and stay in step. the browser syncs when the page opens, every 15 minutes, when it comes back online or into view, and 20 seconds after an edit. there's no background sync once the tab is closed. sync now covers the rest.

- **nextcloud:** nextcloud sends no cors headers, so browsers need a small proxy. deploy [nextcloud-proxy](nextcloud-proxy) to your own cloudflare account (one command) and paste its address into the nextcloud sheet. it only forwards to your server and stores nothing. if you run the server and add cors headers yourself, leave the proxy empty
- **google drive:** needs a "web application" oauth client id from google cloud. put it in the `paisa-google-client-id` meta tag in `app/src/wasmJsMain/resources/index.html`. browsers get no refresh token, so after about an hour you tap connect again

### ai on the web

add with ai and statement import work in the browser too, as long as the provider answers browser requests (cors). anything that speaks the openai chat completions format and allows browsers works, including a relayroom gateway added as an openai-compatible custom provider. if a provider blocks the browser, the sheet tells you so in a couple of seconds instead of spinning.

what the web can't do yet:

- read sms. browsers can't, so the sms screen says so
- pdfs. no pdf export, and statement import and add with ai can't read pdf text in the browser yet. csv, text and images work
- app lock
- a camera button. on a phone, the photos button offers the camera itself
- some currency symbols (aed, sar, bdt, npr) and the → arrow have no font in the web build. setup shows those currencies by code instead

## where stuff lives

```text
app/src/
├── commonMain/       everything both platforms share
│   ├── data/         room 3 database, daos, repositories, demo data
│   ├── sync/         change files and merge rules
│   ├── backup/       the .paisa container, its crypto, drive and nextcloud
│   ├── ai/           bring-your-own-key providers, model ladder, add with ai
│   ├── statement/    statement import: read the file, extract rows, dedupe
│   ├── sms/          merchant to category mapping, the scan service
│   ├── export/       csv and pdf export
│   ├── core/         tabs, navigation, app lock and privacy state
│   ├── platform/     dates, numbers and a calendar that work everywhere
│   ├── di/           koin modules
│   └── ui/           one folder per screen area, plus theme and components
├── androidMain/      sms, widgets, workmanager, biometrics, drive sign-in, pdf
└── wasmJsMain/       the web host, browser storage, file downloads
    └── sqlite-worker/  sqlite in a web worker, for room on the web
parser-core/          bank sms parsers, from PennyWise (AGPL)
nextcloud-proxy/      the cors proxy the web build needs for nextcloud
```

## roadmap

- [ ] recurring transactions
- [x] better material 3 charts
- [x] a web app
- [ ] ios
- [ ] use the official kotlin client, `sh.taf.relayroom:relayroom-kotlin` for relayroom

the web version is kotlin multiplatform, not a typescript rewrite. done so far:

1. **ui:** compose multiplatform with a `wasmJs` target. android-only bits like `Context` and intents sit behind `expect`/`actual`
2. **database:** room 3 with a wasm sqlite worker, stored in the browser's origin private file system
3. **exports:** downloads on the web, the system file picker on android

still to do:

- a pwa shell: a manifest so it installs to the home screen, and a service worker so it opens offline
- pdf on the web

## license

AGPL-3.0, see [LICENSE](LICENSE) and [COPYRIGHT](COPYRIGHT).

`parser-core/` comes from [PennyWise AI Tracker](https://github.com/sarim2000/pennywiseai-tracker), which is also AGPL, so the whole app is AGPL-3.0. if you modify paisa and run it as a network service, you have to give your users the source.
