# On-device AI

Billi can run some AI work on the phone itself. Android uses Gemini Nano through
AICore and ML Kit's Prompt API. There is no API key, no request body leaving the
device, and no per-call bill. The tradeoff is a small model with a short context
window, and Android only allows it while the app is in front.

This is an addition, not a replacement. Cloud providers still handle PDFs, voice
notes and anything that needs a larger model. The ladder decides the order.

## What Pixel 11 Pro can do

Google lists the Pixel 11, 11 Pro, 11 Pro XL and 11 Pro Fold under Gemini Nano 4
(`nano-v4`). The same model version runs on Pixel 10 and several Samsung, Honor,
iQOO, OnePlus, OPPO, vivo and Xiaomi flagships. Pixel 9, 9 Pro, 9 Pro XL and
9 Pro Fold use Gemini Nano 3.

For an app, the useful pieces are:

| Capability | API | Billi uses it |
| --- | --- | --- |
| Custom text prompts, JSON-ish output | Prompt API (`GenerativeModel`) | Yes |
| Image plus text input, up to 10 images | Prompt API | Yes, one image per call |
| Multi-turn chat | Prompt API contents/session | No, each Billi job is one call |
| Structured output with a schema | Prompt API typed output | No, Billi validates JSON itself |
| Tool calling | Prompt API | No |
| Thinking mode | Prompt API on Nano 4 | Not exposed yet |
| Summarize, proofread, rewrite | Task APIs | No, not needed for ledgers |
| Image description | Task API | No, the Prompt API reads receipts |
| Speech recognition, translation, TTS | Task APIs | No, Billi records audio and sends it to cloud models |
| Embeddings | Embeddings API | No |

AICore shares one model across apps, so Billi does not add a 2 to 4 GB download
to its APK. The model lives in the phone's AICore install. Billi only asks
whether it is available and, when it is not, asks AICore to download it.

### Limits that matter

- Input is capped around 4,000 tokens, about 3,000 English words.
- Output is capped around 4,000 tokens.
- AICore enforces a per-app inference quota and a longer battery quota.
- Inference is blocked when Billi is not the top foreground app.
- PDFs and audio are not Prompt API inputs on Android. Text and images are.
- Unlocked bootloaders are not supported.
- The Android emulator has no AICore and no Gemini Nano. Local AI cannot run
  there, even on a Play Store image.

## Compatible phones

Google's [GenAI device list](https://developers.google.com/ml-kit/genai) is the
source of truth. The short version as of November 2026:

| Nano version | Phones |
| --- | --- |
| Nano 4 | Pixel 11, 11 Pro, 11 Pro XL, 11 Pro Fold, Pixel 10 series; Galaxy S25/S26 series, Z Flip 7/8, Z Fold 7/8; OnePlus 13/13s/15/15R; OPPO Find N5/N6/X8/X9/X10; vivo X200/X300/X500; Xiaomi 15/17, 15T Pro, 17T Pro; Honor Magic 7/8, Magic V5/V6; iQOO 13/15; Motorola Edge 70 Pro, Razr 60 Ultra; Sony Xperia 1 VIII; Sharp AQUOS R11 |
| Nano 3 | Pixel 9, 9 Pro, 9 Pro XL, 9 Pro Fold; Samsung Galaxy S26 series (task APIs); OPPO Reno 14/15/16 Pro; realme GT 7T; Googlebooks |
| Nano 2 | OnePlus 13, Xiaomi 15/17, Galaxy Z Fold7/Z TriFold, Honor Magic 7/V5, realme GT 7 Pro, POCO F7/F8/X7/X8, vivo X200 FE/T4 Ultra |

The task-specific APIs (summarize, proofread, rewrite, image description) have a
slightly wider list than the Prompt API. Since Billi needs free-form prompts and
JSON, the Prompt API list is the one that applies.

Apple Intelligence is a separate stack. It has no bearing on the Android build.
The Foundation Models framework is available on iOS, iPadOS, macOS and visionOS
26 and newer, on Apple Intelligence capable hardware, with the model enabled and
ready. It gained direct image input in iOS 27. Billi has no iOS target today, so
there is nothing to wire up. If one is added, it can implement the same
`LocalAiEngine` interface with `SystemLanguageModel.availability` and a
`LanguageModelSession`.

Chrome has its own local model. The Prompt API is on by default in desktop Chrome
148 and later, with text, image and audio input and a text reply. It is not
available in Chrome on Android or iOS. Billi's web build has no local engine yet,
so the Local AI screen explains that Android is where it runs. A later web
engine could use `LanguageModel.availability()` and `LanguageModel.create()`.

## What Billi sends to the local model

| Billi feature | Local support | Notes |
| --- | --- | --- |
| Add with AI, typed note | Yes | Same system prompt and JSON parser as cloud |
| Add with AI, receipt or screenshot | Yes | One image per call, scaled to 1568 px |
| Add with AI, voice note | No | Prompt API on Android takes no audio |
| Add with AI, PDF | No | Prompt API takes no PDF and the context is too small |
| Statement import, text/CSV | Yes | Statements are chunked; very long files need several calls |
| Statement photos | Yes | One image per call |
| Scanned PDF | No | No PDF input |
| Bill scan | Yes for photos and text | Same Quick add ladder |
| Refine drafts | Yes | Short JSON in, short JSON out |

Local is a rung in the existing ladder. If it fails, is skipped or hits a quota,
the next rung runs. That is the same failure path as a cloud model.

## How it is wired

- `ai/local/LocalAi.kt` defines `LocalAiEngine`, its status, the catalogue entry
  and the pure ladder edits.
- `androidMain/.../MlKitLocalAiEngine.kt` talks to AICore with ML Kit's
  `genai-prompt` beta. It checks status, downloads, reads the model name and
  token limit, and runs text or image prompts.
- `ProviderClient` sends `ApiStyle.LOCAL` calls to that engine instead of Ktor.
  No key check, no URL check, no redirect check, no retry loop.
- `AiRepository` merges the local status into the provider list and catalogue and
  stores the ladder change.
- The AI overview has a "Use local AI" switch. Turning it on puts
  `Rung(local, gemini-nano)` at the top of the Default ladder. Turning it off
  removes local rungs from every ladder.
- `Local AI` screen shows availability, download progress, the active Nano
  version, what it can and cannot read, and a live test prompt.

The ML Kit AAR declares `minSdk 26`. Billi keeps `minSdk 24` and uses
`tools:overrideLibrary`, then only loads the AICore engine on API 26 and newer.
The unit test manifest has the same override so Robolectric can run the graph.
No ML Kit class is touched on API 24 or 25.

`genai-prompt:1.0.0-beta4` is used because it ships with Kotlin 2.3.21, the
project's compiler version. `beta5` exists but depends on Kotlin 2.4.0. Moving to
it should be a separate upgrade with the Kotlin and Compose versions.

## Testing

What runs in CI and locally:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:compileKotlinWasmJs
```

New tests cover ladder edits, catalogue capability, no-key/no-URL calls, JSON
parsing from the demo engine, and local style rejecting PDF and audio input.

On an emulator with the debug APK:

```bash
# Real AICore path: shows "Not available on the emulator"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n sh.taf.billi/.MainActivity

# Demo local engine, for the full UI path without AICore
adb shell am force-stop sh.taf.billi
adb shell am start -n sh.taf.billi/.MainActivity --ez local_ai_demo true
```

The demo engine is only compiled into debug builds, only switches on when that
extra is passed, and labels its output "Made by the local demo engine". Release
builds always use AICore.

End-to-end check on Pixel 8 API 35 emulator:

1. AI screen shows "Use local AI", disabled, with "Not available on the
   emulator". Tapping opens the Local AI screen with the same explanation.
2. With the demo engine, the switch turns on and the Default ladder becomes
   `Gemini Nano · nano-v4 (demo) · On-device`.
3. Add with AI, "Lunch 240 at Chaayos yesterday" produces one draft for 240,
   expense, today, Food & Dining, HDFC Savings.
4. Saving changes the balance by 240 and stores the note "Made by the local demo
   engine" in the transaction row.

The one thing an emulator cannot prove is real AICore inference. Before shipping,
run the following on a supported phone:

1. Settings > Google > AICore is installed and up to date.
2. Local AI shows "Available to download" or "Ready" and the model name starts
   with `nano-`.
3. Download completes, then "Test on-device model" replies OK.
4. A typed quick add produces a sensible draft.
5. A receipt photo produces a draft on a Nano 4 phone.
6. A long statement is split and imported, or fails with the 4,000 token message.
7. Backgrounding the app during a local import gives the foreground-only error,
   and the ladder moves to the next model if one is configured.

## Rollout plan

1. Ship the engine behind the existing "Use AI" switch. Local AI is opt-in and
   never adds a rung on its own.
2. Keep local first only when the user turns it on. If a phone is slow or the
   reply quality is poor, they can drag it down in the Default ladder.
3. Watch three failure classes: `NOT_AVAILABLE` (AICore still setting up),
   `BUSY`/quota (temporary), and `REQUEST_TOO_LARGE` (needs smaller chunks).
4. Do not move statement import to a worker while local is the only rung. AICore
   blocks background inference.
5. Consider typed output and thinking mode after the first release. Both need
   more prompt tests across Nano 2, 3 and 4, since output differs by version.
6. A web engine and an iOS engine can implement the same interface later.
