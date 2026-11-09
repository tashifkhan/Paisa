# Paisa web

Phase 0 of `docs/kotlin-web-and-sync.md`. It's a spike, not the product: one page that answers the risky questions before the real web app gets built.

## Run it

```sh
./gradlew :web:wasmJsBrowserDevelopmentRun   # dev server on http://localhost:8080
./gradlew :web:wasmJsBrowserDistribution     # static files in web/build/dist/wasmJs/productionExecutable
```

The production output is plain static files. Any static host works, and no special headers are needed.

## What it proves

- Compose Multiplatform on Kotlin/Wasm renders and takes text input, including Hindi and emoji, in Chromium, Firefox and WebKit.
- Room 3 opens a database in the origin private file system and keeps it across reloads. The launch counter on the page goes up each time.
- The browser decrypts a `.paisa` backup or a `paisa-sync-*.chg` file that the phone wrote, using WebCrypto with the same PBKDF2 and AES-GCM parameters. A wrong password shows an error, not a crash.

## How the database works

`sqlite-worker/worker.js` runs SQLite Wasm in a web worker and speaks the message protocol that `androidx.sqlite`'s `WebWorkerSQLiteDriver` expects. Room 3 talks to that driver.

The worker uses the `opfs-sahpool` VFS. The plain `opfs` VFS needs `SharedArrayBuffer`, which means cross-origin isolation headers, and those break the Google sign-in popup. `opfs-sahpool` doesn't, so the Drive phase can use the popup.

`webpack.config.d/sqlite-worker.js` points webpack at the worker source. Don't turn the worker into an npm `file:` dependency. Yarn copies those once and keeps serving the stale copy.

## Not in the spike

- No Google Drive or Nextcloud calls. There's no web OAuth client yet.
- The database has two spike tables, not Paisa's schema. The real schema comes when the Android entities move to `:shared` on Room 3.
- No sync loop, PWA manifest or service worker.
