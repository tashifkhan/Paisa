# Porting a package to commonMain

Paisa's `:app` is a Kotlin Multiplatform module with an Android target and a
wasmJs (web) target. Code moves from `app/src/androidMain/kotlin/codes/tashif/paisa/...`
to `app/src/commonMain/kotlin/codes/tashif/paisa/...` so both platforms share it.

The rule: Android must look and behave exactly as before. The web gets the
closest browser equivalent, or a clear "not available on the web" state.

## Steps

1. `git mv` the package's files into commonMain, keeping paths.
2. Run `python3 tools/to_common.py <files>`. It rewrites the recurring JVM idioms:
   - `Calendar` to `PaisaCalendar`
   - `"%.Nf".format(x)` to `formatFixed(x, N)`
   - `SimpleDateFormat(p, Locale…).format(Date(x))` to `formatDate(x, p)`
   - `BackHandler` to `PlatformBackHandler`
   - `Color.parseColor` to `parseColor`
   - `lowercase(Locale…)` to `lowercase()`
   - `System.currentTimeMillis()` to `nowMillis()`
3. Compile and fix what's left by hand.

## Common helpers that already exist

`codes.tashif.paisa.platform`:

- `nowMillis()`, `randomHexId()`, `radiansToDegrees()`
- `formatDate(millis, pattern, utc = false, machine = false)`. On Android this is `SimpleDateFormat(pattern, Locale.getDefault()/US)`. `machine = true` means `Locale.US`.
- `parseDate(text, pattern, utc, machine): Long?`
- `formatMediumDateTime(millis)`, which is `DateFormat.getDateTimeInstance(MEDIUM, SHORT)`
- `formatFixed(value, decimals, machine = false)`, which is `"%.Nf"`
- `formatGrouped(value, decimals)`, which is `"%,.Nf"`
- `formatInteger(value)`, which is `NumberFormat.getIntegerInstance()`
- `localeFirstDayOfWeek()`
- `PaisaCalendar` with `getInstance()`, `getUtcInstance()`, `get`, `set` (lenient), `add`, `getActualMaximum`, `timeInMillis`, `firstDayOfWeek`, `clone()`, `before`, `after`, and the Calendar field constants. It has no `time`/`Date`, so use `timeInMillis`.

`codes.tashif.paisa.ui.platform`:

- `PlatformBackHandler(enabled, onBack)`
- `isImeVisible()`
- `dynamicColorScheme(dark)`, `dynamicColorSupported`
- `parseColor(text): Color`

These also work in common code:

- `androidx.lifecycle.ViewModel`, `viewModelScope`, `collectAsStateWithLifecycle`
- `org.koin.compose.viewmodel.koinViewModel`
- `androidx.compose.material.icons.*`, `MaterialShapes`, `LoadingIndicator`, `MotionScheme`
- `kotlin.concurrent.Volatile`, `kotlinx.io.IOException`
- Room 3 DAOs and repositories under `data/`
- `Sql` and `sqlTransaction` in `data/sql`

Note that `kotlinx-datetime` is 0.6.2. It uses `kotlinx.datetime.Instant`, `dayOfMonth` and `monthNumber`.

## Platform-only things

Context, permissions, intents, activity result launchers, WorkManager, the SMS
worker, biometrics, Google sign-in, file pickers, toasts, SharedPreferences:

- **Composable or UI-level pieces.** Use an `expect fun` in commonMain with
  `actual`s in androidMain and wasmJsMain. Make the Android actual the existing
  code, moved as-is.
- **Services used by viewmodels.** Define an interface in commonMain,
  implement it in androidMain by wrapping the existing class, implement it in
  wasmJsMain for the browser, and inject it with Koin.

Put new expect, actual and interface files inside your own packages, with
unique file names (for example `ui/settings/SettingsPlatform.kt`). Other people
port other packages at the same time, and the work gets merged.

Don't edit `app/src/androidMain/.../di/AppModules.kt`. If you need Koin
bindings for new interfaces, add a new file `di/<Area>Module.kt` in androidMain
(and `di/<Area>WebModule.kt` in wasmJsMain), each declaring one `val …Module`.
Report the names.

## Verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_HOME=~/Library/Android/sdk
./gradlew :app:compileKotlinWasmJs :app:compileDebugKotlinAndroid :app:testDebugUnitTest -q
```

All three must pass. 173 unit tests pass today.

## Commits

Use a plain, concise message. No co-author trailer lines. Don't push.
Comments and messages: no em dashes, say what the code does.
