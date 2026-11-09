# Kotlin web and cloud sync

This note answers two questions:

1. What would a Kotlin web version of Paisa look like in this repo?
2. How do Google Drive and Nextcloud connect, authenticate and sync end to end?

Short version: the hard part already exists. Paisa has an encrypted, provider-neutral sync file format, Google Drive and Nextcloud stores, and tests that prove two devices converge through a shared folder. The web version does not need a new backend or a new sync design. It needs a browser client that speaks the same file format and the same two providers.

## What already exists

| Piece | File | State |
| --- | --- | --- |
| Sync file format, last-write-wins, tombstones | `app/src/main/java/codes/tashif/paisa/sync/SyncEngine.kt` | Done, tested |
| Sync schema and SQL triggers | `sync/SyncSchema.kt` | Done |
| Pull, push, cursors, full/delta files | `sync/SyncManager.kt` | Done, tested |
| Google Drive store and sign-in | `backup/GoogleDriveStore.kt` | Done |
| Nextcloud WebDAV and Login Flow v2 | `backup/NextcloudStore.kt` | Done |
| AES-256-GCM container, PBKDF2 key | `backup/BackupCrypto.kt` | Done, tested |
| Backup archive | `backup/BackupArchive.kt` | Done, tested |
| Backup and sync UI | `ui/backup/*` | Done |
| Sync tests with a fake cloud | `SyncManagerTest.kt` | Done |

The Android flow today is two steps:

1. Connect Google Drive or Nextcloud on the Backup screen.
2. Turn on "Sync between devices" and enter a sync password.

That is deliberate. Backup and sync are separate features that share one provider. If you want one click to connect and sync, that is a small UX change, covered below.

The web client is the missing piece.

## What "Kotlin web" means here

Use Compose Multiplatform with a `wasmJs` target. Kotlin compiles to WebAssembly, Compose renders to a canvas, and the browser runs the same domain code as Android. This is the plan already written in the README. It is viable now:

- Compose Multiplatform for web runs on current Chrome, Firefox and Safari with Wasm GC support. Some text input and accessibility edges are still maturing, so test the screens you depend on.
- Room 3.0 supports JS and WasmJS, with a Web Worker based SQLite driver that stores the database in the browser's origin private file system (OPFS).
- Ktor 3.6 has a WasmJS client for Google Drive and WebDAV.
- Kotlin 2.4 and Compose Multiplatform 1.12 are the current stable line.

The repo is on Kotlin 2.1.10, Compose BOM 2025.10.00, AGP 8.9.1 and Room 2.7.0. A web target needs a toolchain upgrade. That upgrade is the largest single cost in this plan.

### What can be shared

- Domain entities and the database schema.
- The sync file format and merge rules.
- The backup container and crypto header.
- The cloud store interface.
- Most Compose UI, after Android-only APIs are put behind `expect`/`actual`.
- Formatting, charts and the app shell state.

### What cannot be shared as-is

| Android dependency | Web replacement |
| --- | --- |
| Room 2.7 with `SupportSQLiteDatabase` | Room 3 with the web SQLite driver, or SQLDelight |
| Hilt | Koin or a small manual app graph |
| WorkManager | Coroutines, app focus, online/offline events, and a visible Sync now button |
| OkHttp | Ktor client with the JS/Wasm engine |
| `EncryptedSharedPreferences` | IndexedDB plus WebCrypto |
| Play Services `AuthorizationClient` | Google Identity Services in the browser |
| `Context`, `Uri`, intents | Browser APIs behind `expect`/`actual` |
| SMS receiver, widgets, biometrics | Not on web |
| `android.util.JsonReader` / `JsonWriter` | kotlinx.serialization or a small common JSON writer |
| `javax.crypto` | WebCrypto, same PBKDF2 and AES-GCM parameters |

### What the UI looks like

Do not copy the phone layout pixel for pixel. The phone app is a bottom bar and a FAB. The web app should use the width it has.

Desktop:

```text
┌──────────────────────────────────────────────────────────────────┐
│ Paisa          [ Search ]                         [+ Add]  [..]  │
├────────────┬──────────────────────────────────┬──────────────────┤
│ Home       │  Balance                          │  This month      │
│ Analysis   │  ₹ 1,24,500                       │  Income  ₹85,000 │
│ Budgets    │                                   │  Spent   ₹32,400 │
│ Accounts   │  Today                            │  Chart           │
│ Debts      │  Swiggy            -₹450          │                  │
│            │  Salary          +₹85,000         │  Sync: on        │
│ Settings   │  Metro              -₹60          │  Last: 2m ago    │
└────────────┴──────────────────────────────────┴──────────────────┘
```

Mobile browser: keep the bottom bar, FAB, sheets and one-column layout. Same components, different breakpoint.

Desktop-specific behaviour worth adding:

- Navigation rail instead of bottom bar above 900 px.
- A two-column Home: records on the left, balance and monthly summary on the right.
- A real table for transactions, with sticky headers and keyboard navigation.
- `/` focuses search, `n` opens Add transaction, `Esc` closes the top sheet.
- Hover states on rows and tooltips on chart segments.
- Browser back and forward wired to tabs and sheets. Compose Multiplatform 1.10 added browser history support.

Onboarding changes:

- No SMS step. The web cannot read SMS and should not pretend to.
- Offer "Restore from Google Drive", "Restore from Nextcloud" or "Start fresh".
- Then offer "Turn on sync" with the same password used on the phone.
- Manual entry and statement import stay. Widgets, app lock by biometrics and SMS do not.

## Module layout

The clean destination is one Gradle build with a shared module and two apps:

```text
Paisa/
├── app/            Android app: Compose, Hilt, Room, SMS, widgets
├── shared/         KMP: entities, sync core, crypto, cloud stores, repositories
│   ├── commonMain/
│   ├── androidMain/
│   └── wasmJsMain/
├── web/            Compose Multiplatform wasmJs app
├── parser-core/    bank SMS parsers, JVM only for now
└── proxy/          optional Nextcloud CORS proxy, deployed by the user
```

`parser-core` stays JVM-only. The web version does not parse SMS. Statement parsing can be added later with PDF.js and the AI providers.

### Path A: full KMP

Move the UI, entities, repositories and sync core into `shared`, then make `app` and `web` thin hosts. This is the clean end state. It needs:

- Kotlin 2.4.x.
- Compose Multiplatform 1.12.x.
- Room 3.0.x, which is a package rename (`androidx.room3`) and drops `SupportSQLiteDatabase`.
- AGP 8.13 or newer.
- Hilt removed from shared code. Koin is the smallest step.

The Room 3 migration is real work. Every DAO becomes coroutine-first, the raw SQL sync code moves to the new driver API, and the existing migrations need a pass. Do it on a branch with the current test suite as the safety net.

### Path B: web-first

Keep the Android app on Room 2.7 and build `web` as a separate Kotlin/Wasm app with its own data layer, sharing only the sync file format, crypto and cloud store interface. Faster to ship. The cost is two data layers and two sync implementations, which will drift.

Recommendation: do the spike first, then choose. The spike answers the risky questions before committing to a months-long migration. If the spike works, Path A is worth it. If Compose web or Room 3 blocks something important, Path B still gives a useful web app.

## Sync design for web

The web client is just another device. It uses the same cloud folder, the same `.chg` files and the same sync password.

### File format

A change file is gzip'd JSON:

```json
{
  "format": 1,
  "dbVersion": 12,
  "deviceId": "16 hex chars",
  "deviceName": "Chrome on macOS",
  "seq": 7,
  "full": false,
  "createdAt": 1760000000000,
  "rows": {
    "transactions": [
      { "id": "sync id", "s": 1760000000000, "v": { "amount": 450.0, "note": "Swiggy" } }
    ]
  },
  "deleted": [
    { "t": "transactions", "id": "sync id", "s": 1760000000001 }
  ]
}
```

Foreign keys carry the parent's `syncId`, never a local row id. The merge is last-write-wins per row on `syncStamp`, with tombstones for deletes. Applying the same file twice, or applying files out of order, gives the same result.

The web implementation should not invent a new format. Export and apply the same fields. A common `SyncCodec` in `shared` prevents drift.

### Local database

Use Room 3 with the web SQLite driver. The database lives in OPFS, so it survives reloads and does not leave the browser. Keep the same table and column names as Android:

- `accounts`, `categories`, `transactions`, `budgets`, `savings_goals`, `recurring_transactions`, `merchant_mappings`, `debt_people`, `debt_entries`, `settings`.
- `sync_state`, `sync_outbox`, `sync_tombstones`.
- Install the same SQL triggers that set `syncId` and `syncStamp` and queue changes.
- Omit Android-only tables such as `unrecognized_sms`. The sync file never carries them.

If Room 3 web is too rough, SQLDelight with the SQLite WASM driver is the fallback. The schema is plain SQLite, so either works. The sync triggers are the important part, not the ORM.

### Crypto parity

`BackupCrypto` uses:

- PBKDF2-HMAC-SHA256, 210,000 iterations, 256-bit key.
- AES-256-GCM, 12-byte IV, 128-bit tag.
- The header bytes as additional authenticated data.
- Header: `PAISABAK`, version 1, flags, salt, iterations, IV.

WebCrypto maps directly:

- `crypto.subtle.importKey("raw", utf8(password), "PBKDF2", ...)`
- `deriveKey` with `{ name: "PBKDF2", salt, iterations, hash: "SHA-256" }`
- `AES-GCM` with `{ name: "AES-GCM", iv, additionalData: header, tagLength: 128 }`

Test with a non-ASCII password. Java's `PBEKeySpec` and WebCrypto's `TextEncoder` both use UTF-8, but a test with a Hindi or emoji password catches encoding mistakes early.

### Sync loop

```text
on app open, on network online, 20 s after a local edit, or on "Sync now"
  -> list paisa-sync-*.chg files in the cloud
  -> for each other device, pull files newer than its cursor
  -> decrypt, apply in one transaction, advance cursor
  -> if the outbox has changes, export a delta file
  -> encrypt, upload paisa-sync-<device>-<seq>-d.chg
  -> clear the outbox
  -> every 40 deltas, upload a full file and delete older files
```

The web needs a `SyncScheduler` because WorkManager does not exist:

- `document.visibilitychange` when the app becomes visible.
- `window.addEventListener("online")`.
- A debounced call after local writes.
- A visible "Sync now" button. This is the only guarantee. Browser background sync is not reliable.

### Device identity

- Generate a 16-character hex device id once with `crypto.getRandomValues`.
- Store it in IndexedDB. Never copy an Android device id to the browser.
- Device name from `navigator.userAgent` and `navigator.platform`, shortened to something a person can read, for example "Chrome on macOS".
- The sync password is never stored in the cloud. Keep the derived key in IndexedDB as a non-extractable `CryptoKey` if the user chooses "remember on this device". Add a "Forget sync password" action that deletes it.

## Google Drive end to end

### Google Cloud setup

1. Use the same Google Cloud project as the Android app.
2. Enable the Google Drive API.
3. Create an OAuth client of type "Web application".
4. Add authorized JavaScript origins: `http://localhost:8080`, the production origin, and any preview origin.
5. Use `https://www.googleapis.com/auth/drive.appdata` plus `openid email`. The Android app currently asks for `drive.appdata` and `email`.
6. Configure the OAuth consent screen. `drive.appdata` is a sensitive scope, so a public release needs Google verification. Testing mode works for a small number of test users.

Keep the Android and web clients in the same project. App data folder access is scoped to the app, and Google's docs do not spell out how client ids in one project share it. Test this early with a real account. If the web client cannot see the files written by Android, the fallback is a visible `Paisa` folder with the `drive.file` scope, plus a migration in the Android app.

### Auth flow

```text
click Google Drive
  -> load https://accounts.google.com/gsi/client
  -> google.accounts.oauth2.initTokenClient({ client_id, scope, callback })
  -> requestAccessToken({ prompt: "consent" })
  -> account chooser and consent
  -> access token, expires_in
  -> GET https://www.googleapis.com/oauth2/v3/userinfo
  -> show the email
  -> list appDataFolder for paisa-sync-* files
     -> files exist: ask for the sync password used on the phone
     -> no files: ask for a new sync password
  -> derive key, verify by decrypting the smallest existing file
  -> enable sync, pull, push a full file
```

Use the browser token model. There is no refresh token in the browser, and the access token lasts about an hour.

That has one consequence: a sync that runs long after the token expired may need a user gesture. Design for it:

- Keep the token in memory and refresh it proactively while the app is open.
- On a 401, show "Reconnect Google Drive" instead of failing silently.
- Make the first connect and the main "Sync now" click-driven.
- Do not promise hourly background sync on the web. The phone app can do that. The web app syncs when open, when clicked, and when the token is still valid.

A backend that holds the client secret and refresh tokens would remove this limit, but it adds a server, an account and a privacy cost. The app's pitch is no server. Stay with the token model unless the UX demands otherwise.

### Drive calls

```text
list:     GET  /drive/v3/files?spaces=appDataFolder
               &q=name contains 'paisa-sync-' and trashed = false
               &fields=nextPageToken,files(id,name,size)
               &pageSize=1000
download: GET  /drive/v3/files/{id}?alt=media
upload:   POST /upload/drive/v3/files?uploadType=multipart
               multipart/related: JSON metadata + bytes
delete:   DELETE /drive/v3/files/{id}
```

Google APIs support CORS. The browser can call them directly with a bearer token. No proxy is needed.

Keep the same backup list and restore path. A new browser can restore the latest `.paisa` file before sync is on. That is the easiest way to bootstrap a new device.

## Nextcloud end to end

### Login Flow v2

```text
POST {server}/index.php/login/v2
  <- { poll: { token, endpoint }, login }
open {login} in a new tab
user logs in and grants access
POST {endpoint} with token every 2 seconds
  <- { server, loginName, appPassword }
GET {server}/ocs/v2.php/cloud/user?format=json
  <- ocs.data.id
```

The app password is long-lived and does not need a refresh flow. This is the better web story of the two providers, if the browser can reach the server.

### The CORS problem

Nextcloud does not send CORS headers for WebDAV or Login Flow v2 by default. A browser on `https://paisa.example` cannot call `https://cloud.example` directly. Native Android does not care because OkHttp is not a browser.

Three ways out.

#### Option A: a small proxy

Ship a stateless proxy that forwards only the calls Paisa needs:

- `POST /login/v2`
- `POST /login/v2/poll`
- WebDAV `PROPFIND`, `GET`, `PUT`, `DELETE`, `MKCOL`

The proxy does not store credentials or files. It adds CORS headers and forwards the body. A Cloudflare Worker is enough. The user can deploy it under their own Cloudflare account with one command, so their traffic goes through their own worker and the project does not run a central service.

If the project hosts one shared worker, protect it:

- HTTPS targets only.
- Block private and link-local addresses to prevent SSRF.
- Allow only WebDAV methods.
- Rate limit by IP.
- Do not log credentials.

Even then, a public proxy can be abused. A deploy-your-own model is safer and fits the self-hosted Nextcloud audience.

#### Option B: the WebAppPassword app

`WebAppPassword` is a Nextcloud app that injects CORS headers and issues temporary app passwords for WebDAV, CalDAV and related APIs. If the user can install it, the web app can talk to Nextcloud directly after a popup approval. This avoids a proxy but adds an app install and a less common flow. Support it as an option, not the default.

#### Option C: manual app password plus a proxy

The user creates an app password in Nextcloud settings, enters server, username and password in Paisa, and the proxy handles WebDAV. No Login Flow v2 needed. This works on any Nextcloud and is the best fallback when the login flow is blocked. The Android app already offers a manual path, so the copy can match.

Recommendation: implement the proxy path first, keep the manual app password as a fallback, and document WebAppPassword for admins who prefer no proxy.

### WebDAV calls

Same paths as Android:

```text
backups: /remote.php/dav/files/{userId}/Paisa
sync:    /remote.php/dav/files/{userId}/Paisa/sync
```

Use Basic auth with the app password, `PROPFIND` with `Depth: 1` to list, `PUT` to upload, `GET` to download, `DELETE` to remove, `MKCOL` to create the folders. Keep the `User-Agent` honest: `Paisa (Web)`.

## One-click connect and sync

The current Android flow asks for the provider, then separately asks to turn on sync. The user asked for "click it and they sync". The clean flow is one combined action.

### First device

```text
Settings -> Backup and sync
  -> Google Drive or Nextcloud
  -> authenticate
  -> "This is the first device. Choose a sync password."
  -> derive key, upload a full file
  -> "Sync is on. Connect your phone with the same password."
```

### Joining device

```text
Settings -> Backup and sync
  -> Google Drive or Nextcloud
  -> authenticate
  -> Paisa finds existing paisa-sync-* files
  -> "Your phone already syncs here. Enter its sync password."
  -> decrypt the smallest file to verify
  -> pull everything, upload a full file
  -> "Synced 214 changes from Tashif's phone."
```

### Bootstrapping a new browser

If backups exist, offer restore before sync:

```text
Install PWA
  -> Restore from Google Drive / Nextcloud
  -> pick the newest .paisa backup
  -> unlock with the backup password
  -> data is local
  -> optionally turn on sync with the same password
```

This is faster than replaying every sync file and gives the user a working app in one step.

### Android one-click change

If you also want this on Android, the change is contained:

- In `BackupViewModel.onGoogleAuthorized` and `finishNextcloud`, after the provider is saved, check `config.sync.enabled`.
- If sync is off, open the sync password sheet automatically with the title "Turn on sync".
- In `BackupViewModel`, before showing the sheet, call `store.listSync()` and tell the user whether they are starting fresh or joining an existing set of devices.
- Keep the separate "Back up only" path for users who do not want multi-device sync.

Do not enable sync silently with a generated password. The same password is needed on every device, and a password the user never sees is a support problem.

## Toolchain and migration

Current and needed versions:

| Tool | Now | Needed for web |
| --- | --- | --- |
| Kotlin | 2.1.10 | 2.4.x |
| Compose | Android BOM 2025.10.00 | Compose Multiplatform 1.12.x |
| AGP | 8.9.1 | 8.13.x or newer |
| Room | 2.7.0 | 3.0.x |
| KSP | 2.1.10-1.0.31 | current KSP |
| DI | Hilt | Koin or manual graph for shared code |
| HTTP | OkHttp | Ktor client |
| JSON | android.util | kotlinx.serialization |
| Crypto | JCE | JCE on Android, WebCrypto on wasm |

The upgrade touches almost every build file and the Room layer. Do it in stages:

1. Upgrade Kotlin, AGP, Compose and KSP on a branch. Keep the Android app green.
2. Move to Room 3 and port the DAOs, migrations and sync SQL. Keep the tests green.
3. Extract `shared` and move the sync core, crypto and cloud stores.
4. Add the `web` target and the shell.

If that is too much at once, use Path B for the first web release and schedule the unification later.

## Phased plan

### Phase 0: risk spike, 3 to 5 days

One branch, one screen, no product UI:

- Kotlin 2.4, Compose Multiplatform 1.12, wasmJs.
- Open a Room 3 database in OPFS.
- Load Google Identity Services and get a token for `drive.appdata`.
- List the Android app's `paisa-sync-*` files in the same Google account.
- Download the smallest file, derive the key from a password, decrypt it with WebCrypto.
- Print the header and the first rows.

This proves the four assumptions that matter: appDataFolder sharing, Drive CORS, crypto parity and OPFS.

Also test the hosting headers. If Room 3 needs cross-origin isolation, check whether Google sign-in still works in popup mode. If it does not, use the redirect mode or a different storage driver.

### Phase 1: shared sync core, 1 to 2 weeks

- Define `SyncCodec`, `SyncCrypto`, `CloudStore` and `SyncDatabase` in `commonMain`.
- Port `SyncEngine` and `SyncSchema` behind those interfaces.
- Keep the Android actuals on the existing code first, then switch to Room 3.
- Add golden-file tests: an Android-generated `.chg` applies on the web and the reverse.
- Run the existing two-device convergence test against the shared core.

### Phase 2: web shell, 1 to 2 weeks

- PWA host page, manifest and service worker.
- Theme, app shell, responsive nav.
- Home, Add transaction, Accounts, Settings.
- OPFS persistence and a local settings store.
- No sync yet. Use fake data and a fake cloud store.

### Phase 3: Google Drive, 3 to 5 days

- GIS sign-in and token refresh.
- Drive list, download, upload and delete.
- Backup restore from `.paisa`.
- Connect and sync UI, including first device and joining device.
- Test with a real Google account that already has Android data.

### Phase 4: Nextcloud, 3 to 5 days

- Deploy-your-own proxy template.
- Login Flow v2 and manual app password.
- WebDAV store and folder creation.
- Same connect and sync UI.
- Test against a real Nextcloud instance with 2FA and an app password.

### Phase 5: finish the web app, 4 to 6 weeks

- Analysis, budgets, debts, categories, merchant rules.
- Transaction detail and edit.
- Statement import and AI settings.
- CSV and PDF export through the File System Access API, with a download fallback.
- Keyboard shortcuts and browser history.
- PWA install and offline states.

### Phase 6: hardening, 1 week

- Cross-browser testing: Chrome, Firefox, Safari.
- Offline changes queue and sync when online.
- Large data sets, broken files, wrong passwords, revoked tokens.
- Accessibility pass.
- Bundle size and cold start.

### Rough total

2 to 3 months for one experienced developer, assuming the spike goes well and the UI scope stays close to the Android screens.

## Testing

- `commonTest` for the sync codec, tombstones, foreign key mapping and conflict resolution.
- A golden file from `SyncEngineTest` that the web must decrypt and apply.
- A web test with Playwright that reloads the page and checks the OPFS database survived.
- A fake Google Drive and fake WebDAV server that both speak CORS, so browser tests do not need real accounts.
- One manual test with a real Android phone and a real browser on the same Google account.
- One manual test with a real Nextcloud instance, including 2FA, an app password and a revoked session.

## Risks

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| appDataFolder is not shared across client ids | The web cannot see Android's files | Test in the spike. Keep both clients in one project. Fall back to a visible folder with `drive.file`. |
| Google verification | Public release blocked until approved | Start the consent screen review early. Use testing mode for the first users. |
| Google token expiry | Background sync stops after an hour | Sync on open and on click. Refresh while open. Show a reconnect action. |
| Nextcloud CORS | Direct browser access fails | Proxy, WebAppPassword app, or manual app password plus proxy. |
| Cross-origin isolation vs Google popup | Sign-in may break if COOP blocks the opener | Test popup and redirect modes. Use a storage driver that does not need isolation if possible. |
| Compose web still has rough edges | Text input, IME and accessibility gaps | Keep the web UI to the screens that matter first. Test on real browsers. |
| Room 3 migration | Large change to a working app | Branch, keep the test suite, migrate in steps. |
| Wasm bundle size | Slow first load on mobile data | Brotli, service worker cache, split non-critical screens. |
| Two data layers (Path B) | Schema drift and double sync bugs | Prefer Path A once the spike passes. |

## Decisions needed

1. Full KMP migration now, or web-first with a separate data layer? Recommendation: spike first, then decide.
2. Room 3 or SQLDelight for the web database? Recommendation: Room 3 if the spike passes, because the entities already exist.
3. Nextcloud: proxy, WebAppPassword, or manual only? Recommendation: proxy first, manual as fallback, WebAppPassword as an admin option.
4. Google: one-hour browser tokens, or a small backend for refresh tokens? Recommendation: browser tokens, no server.
5. Who hosts the PWA, owns the Google OAuth client, and runs the optional Nextcloud proxy? These are product decisions, not code decisions.

## First step

Build the Phase 0 spike. It is small, it does not touch the Android app, and it answers the questions that would otherwise surface after weeks of UI work. If the spike works, the rest is planned work. If it does not, the fallback is a web-first client with SQLDelight and a simpler data model, or a server-rendered Kotlin web app with a hosted sync service.

The existing sync protocol is the asset. Protect it. Everything else is a client.
