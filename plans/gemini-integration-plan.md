# Gemini Integration Plan for Expo App (`app_expo`)

This plan explains how to add Gemini-powered AI features using `google/genai` for:
- transaction analysis insights,
- menu/bill/photo -> transaction extraction,
- strict "Local Only" mode (no Gemini calls at all).

It is designed for the current Expo + FastAPI setup in this repo.


## 1) Product requirements (from your request)

1. Use Gemini via `google/genai` package.
2. API key is user-provided inside the app (not hardcoded).
3. If user selects "Local Only", Gemini must not be used.
4. Add AI services for:
   - analyzing transactions,
   - converting menu/bill/photo into transaction drafts.


## 2) High-level architecture

### Modes
- `local_only`: no network AI calls, local heuristics only.
- `gemini_opt_in`: use Gemini when user has enabled it and key is valid.

### Routing rule (single source of truth)
All AI features must go through one gate:
- `AiModeRouter.canUseGemini(feature)`

If false:
- return local analysis/parser result,
- never instantiate Gemini client,
- never upload text/image to Gemini.


## 3) Dependencies to add in `app_expo`

Core:
- `@google/genai` (official JS SDK for Gemini)

Media handling for photo extraction:
- `expo-image-picker`
- `expo-file-system`
- `expo-image-manipulator`

Secure key storage:
- `expo-secure-store`

Optional (for PDF/bill files):
- `expo-document-picker`

Share sheet support (PDF/photo into app):
- `expo-share-intent` (recommended) or a custom native share-extension plugin


## 4) Settings and key management

### New user-facing settings (Profile)
Add an "AI & Privacy" section in `app_expo/app/(tabs)/profile.tsx`:

1. `AI Mode`
   - Local Only
   - Gemini (Bring your own key)

2. `Gemini API Key`
   - masked input
   - save/update/delete
   - "Test key" button

3. Feature toggles
   - `Use AI for transaction analysis`
   - `Use AI for photo/bill/menu extraction`

### Storage strategy
- Store API key in `SecureStore`, not AsyncStorage.
- Store non-secret toggles in AsyncStorage or user profile API.
- Never log key in console.

### Suggested keys
- `ai_mode`
- `ai_use_gemini_analysis`
- `ai_use_gemini_extraction`
- `gemini_api_key` (SecureStore)


## 5) Service layer additions

Add these services under `app_expo/services/`.

### 5.1 `aiSettingsService.ts`
Responsibilities:
- read/write AI mode and toggles,
- read/write/delete Gemini key (SecureStore),
- expose `getEffectiveAiConfig()`.

### 5.2 `geminiClientService.ts`
Responsibilities:
- lazily create Gemini client using user key,
- expose methods:
  - `generateText(prompt, options)`
  - `analyzeTransactionBundle(input)`
  - `extractTransactionFromImage(base64Image, hint)`
- central timeout/retry handling.

### 5.3 `aiModeRouterService.ts`
Responsibilities:
- determine if Gemini is allowed for a feature,
- enforce local-only hard stop.

### 5.4 `transactionAnalysisService.ts`
Responsibilities:
- analyze recent transactions and return:
  - spending insights,
  - anomaly flags,
  - category suggestions,
  - budget suggestions.
- Uses local analyzer if Gemini not allowed.

### 5.5 `photoToTransactionService.ts`
Responsibilities:
- handle menu/bill/photo parsing pipeline,
- preprocess image (resize/compress),
- call Gemini vision extraction if allowed,
- map extracted data -> app transaction draft.

### 5.6 `localAnalysisService.ts`
Responsibilities:
- deterministic local fallback:
  - top-category spend,
  - unusual amount heuristics,
  - merchant-based category suggestion,
  - confidence scores.


## 6) Data contracts

### 6.1 AI config type
```ts
type AiMode = 'local_only' | 'gemini_opt_in';

interface AiConfig {
  mode: AiMode;
  useGeminiForAnalysis: boolean;
  useGeminiForExtraction: boolean;
  hasGeminiKey: boolean;
}
```

### 6.2 Transaction analysis output
```ts
interface TransactionInsight {
  title: string;
  detail: string;
  severity: 'info' | 'warning' | 'critical';
  relatedTransactionIds?: string[];
}

interface TransactionAnalysisResult {
  provider: 'local' | 'gemini';
  insights: TransactionInsight[];
  suggestedActions: string[];
}
```

### 6.3 Photo extraction output
```ts
interface ExtractedTransactionDraft {
  amount?: number;
  currency?: string;
  merchantName?: string;
  occurredAtIso?: string;
  categoryHint?: string;
  note?: string;
  lineItems?: Array<{ name: string; qty?: number; unitPrice?: number; total?: number }>;
  taxAmount?: number;
  totalAmount?: number;
  confidence: number;
  sourceType: 'menu' | 'bill' | 'photo';
  provider: 'local' | 'gemini';
}
```


## 7) Feature flows

### 7.1 Analyze transactions
1. User taps "Analyze" in stats/home.
2. App fetches user transactions using existing services.
3. `transactionAnalysisService` checks `AiModeRouter`.
4. If local-only: run local analysis only.
5. If Gemini enabled: send summarized transaction bundle to Gemini.
6. Show insight cards with provider badge (`Local` or `Gemini`).

### 7.2 Menu/Bill/Photo -> transaction
1. User picks source type: Menu / Bill / Photo.
2. Capture/select image.
3. Preprocess image (resize, compress, base64).
4. `photoToTransactionService` checks `AiModeRouter`.
5. If Gemini allowed:
   - call vision extraction prompt,
   - parse structured JSON result,
   - return draft with confidence.
6. If local-only:
   - run limited local OCR/parser if available,
   - otherwise show "AI disabled in Local Only" helper and allow manual entry.
7. Show prefilled Add Expense form for user confirmation.

### 7.3 Share sheet -> import flow (PDF/photo)
Goal: show this app in system share sheet when user shares a transaction PDF or image.

1. User taps Share from another app (gallery/files/bank app).
2. `Paisa` appears in share sheet for:
   - `application/pdf`
   - `image/*`
3. Shared file opens app into a dedicated ingest route (for example: `/(modals)/import-from-share`).
4. App determines content type:
   - PDF -> statement parse flow
   - image -> bill/menu/photo extraction flow
5. AI routing is enforced:
   - `local_only`: no Gemini calls, local/manual flow only
   - `gemini_opt_in`: Gemini extraction allowed if key + toggle enabled
6. User reviews extracted draft and confirms save.

Implementation notes:
- Android: configure share intent filters for `SEND`/`SEND_MULTIPLE` with MIME types above.
- iOS: use a share extension (plugin-based) to receive files from share sheet.
- In Expo, use `expo-share-intent` (recommended) or a custom config plugin to wire native pieces.


## 8) Prompting strategy for Gemini

Use strict JSON-only responses.

### 8.1 Transaction analysis prompt (text)
Input:
- compact transaction list (amount/type/date/category/merchant)
- user currency
- analysis goals (anomalies, trend, suggestions)

Output schema:
- `insights[]`, `suggestedActions[]`, `riskFlags[]`

### 8.2 Bill/menu/photo extraction prompt (multimodal)
Input:
- image bytes/base64
- source hint (`menu` | `bill` | `photo`)
- target JSON schema

Output schema:
- merchant/date/currency/amounts/items/confidence

Guardrails:
- "If uncertain, set null and lower confidence."
- "Do not fabricate unreadable values."


## 9) Local Only enforcement details

This is critical.

### Hard rules
1. `local_only` means no Gemini SDK calls.
2. No upload of images/text to external AI endpoints.
3. UI must clearly show "Local Only active".
4. All AI buttons still work with local fallback where possible.

### Code pattern
- Every public method in AI services starts with:
  - read config,
  - `if (!canUseGemini(feature)) return localFallback(...)`.


## 10) Error handling and resilience

Cases:
- missing API key,
- invalid key,
- quota exceeded,
- network timeout,
- malformed JSON response.

Behavior:
1. show friendly toast/snackbar,
2. auto-fallback to local analysis where possible,
3. preserve user input and image so they can retry.


## 11) Security and privacy

1. API key in `SecureStore` only.
2. Never send key to your backend unless explicitly adding server-proxy mode.
3. Redact obvious sensitive text before analysis prompts (account numbers, full UPI IDs, emails).
4. Show disclosure before first Gemini use:
   - "Your selected content may be processed by Gemini."
5. Add per-feature consent toggle (analysis vs extraction).


## 12) Backend touchpoints (optional but recommended)

Not required for direct Gemini use, but recommended:

1. Save AI metadata with transaction creation:
   - `ai_provider`, `ai_confidence`, `ai_source_type`.
2. Endpoint to receive user feedback on AI extraction quality:
   - `POST /ai/feedback`.
3. Optional audit endpoint for local-only compliance diagnostics.


## 13) UI additions summary

### Profile (`app_expo/app/(tabs)/profile.tsx`)
- Add AI & Privacy section.
- Mode selector + key input + feature toggles.

### New modal screens
- `app_expo/app/(modals)/analyze-transactions.tsx`
- `app_expo/app/(modals)/photo-to-transaction.tsx`
- `app_expo/app/(modals)/import-from-share.tsx`

### Add Expense integration
- prefill form from extracted draft,
- show confidence + editable fields,
- require user confirmation before save.

### Share sheet UX
- If app opens from share action, land directly in import preview (not home tab).
- Show source badge: `Shared PDF` or `Shared Image`.
- Preserve shared file temp URI until user confirms/cancels.


## 14) Suggested implementation order

### Phase 1 (core settings + Gemini client)
1. Add dependencies.
2. Implement `aiSettingsService`.
3. Implement `geminiClientService` with key validation.
4. Add profile UI for mode/toggles/key.

### Phase 2 (analysis)
1. Implement `localAnalysisService`.
2. Implement `transactionAnalysisService` (Gemini + local fallback).
3. Add analysis screen and wire to existing transaction data.

### Phase 3 (photo extraction)
1. Implement image picker + preprocessing utility.
2. Implement `photoToTransactionService`.
3. Add modal flow and prefill into add-transaction form.

### Phase 3.5 (share sheet integration)
1. Add share-target plugin/config (`expo-share-intent` recommended).
2. Register Android MIME intent filters and iOS share extension.
3. Add `import-from-share` route and parser dispatch.
4. Enforce local-only gate for share-triggered imports.

### Phase 4 (hardening)
1. Add JSON schema validation for Gemini outputs.
2. Add retries/backoff and graceful errors.
3. Add analytics + user feedback loop.


## 15) Acceptance criteria

1. User can set Gemini key in app; key persists securely.
2. Local Only mode prevents all Gemini calls (verified by logs/tests).
3. Transaction analysis works in both modes:
   - Local mode -> local insights
   - Gemini mode -> Gemini insights
4. Menu/bill/photo extraction returns editable transaction drafts.
5. Failed Gemini calls do not block manual transaction entry.
6. App appears in share sheet for PDF and image sharing.
7. Share-triggered imports obey Local Only mode (no Gemini calls).


## 16) Example service map (final)

- `services/aiSettingsService.ts`
- `services/geminiClientService.ts`
- `services/aiModeRouterService.ts`
- `services/transactionAnalysisService.ts`
- `services/photoToTransactionService.ts`
- `services/shareIntentIngestionService.ts`
- `services/localAnalysisService.ts`
- `services/aiPromptTemplates.ts`
- `services/aiSchemaValidator.ts`


## 17) Notes on package naming

In JS/TS, Gemini SDK is typically installed as `@google/genai`.
If you specifically use a different `google/genai` wrapper in your environment, keep the same architecture and replace only the adapter in `geminiClientService.ts`.
