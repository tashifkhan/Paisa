# Expo React Native Plan: SMS + PDF Transaction Ingestion System

This plan describes how to build a PennyWise-like ingestion pipeline in `app_expo/`:
- Parse bank SMS into structured transactions
- Parse transaction PDF statements (GPay/PhonePe first)
- Deduplicate and save transactions safely
- Support both bulk import and near real-time ingestion

It is tailored to the current repo structure (`app_expo/` + `server/`).


## 1) Goals and parity target

### Primary goals
1. Import historical SMS and create transactions.
2. Parse incoming SMS and auto-add new transactions.
3. Import PDF statements and create transactions.
4. Prevent duplicates across SMS/PDF/manual entry.
5. Keep user control: review, edit, and reject before save when needed.

### Parity target vs Android-native implementation
- `v1` parity: bulk SMS import + PDF import + dedupe + account/category inference.
- `v1.5` parity: near real-time auto ingestion for Android.
- `v2` parity: advanced rules, subscription detection, unrecognized SMS collection, notification-channel ingestion.


## 2) Platform constraints (Expo-specific)

### Important constraints
- Expo Go cannot access Android SMS content providers directly.
- Real-time SMS BroadcastReceiver requires native Android code.
- Background processing in pure JS is limited for always-on ingestion.

### Implication
To build a true PennyWise-like system, use **Expo prebuild/custom dev client** for Android native capabilities.


## 3) Recommended architecture

Use a hybrid architecture:

1. **Client (Expo app)**
   - On-device permissions, scan orchestration, parser execution, review UI.
   - Local ingestion queue and dedupe pre-checks.

2. **Native Android module (inside Expo prebuild)**
   - Read SMS inbox in batches.
   - Receive real-time SMS broadcasts.
   - Emit events to JS.

3. **Backend (`server/`)**
   - Final write path, server dedupe, optional parse fallback.
   - Batch import endpoint for idempotent upsert.

This keeps UX responsive while ensuring data integrity on server.


## 4) Repository fit (where code should go)

### In `app_expo/`
- `services/`
  - add `smsImportService.ts`
  - add `statementImportService.ts`
  - add `parserService.ts` (or local parser package bridge)
- `hooks/`
  - add `useSmsImport.ts`
  - add `useRealtimeSmsIngestion.ts`
  - add `useStatementImport.ts`
- `app/(modals)/`
  - add `import-sms.tsx`
  - add `import-statement.tsx`
  - add `unrecognized-messages.tsx`
- `lib/`
  - add `dedupe.ts`
  - add `hash.ts`
  - add `dateWindow.ts`
- `context/`
  - optionally add `IngestionContext.tsx` for global progress state.

### Native Android bridge (new)
- `app_expo/plugins/withSmsIngestion.ts` (config plugin)
- Prebuild-generated Android files:
  - SMS reader module (`ContentResolver` query)
  - SMS broadcast receiver
  - optional WorkManager-backed periodic scanner

### In `server/`
- `app/routes/`
  - add `ingestion.py`
- `app/services/`
  - add `ingestion_service.py`
  - add `statement_parser_service.py` (if server-side fallback enabled)
- `app/models/`
  - extend transaction schema with source metadata fields.


## 5) Data model changes

Extend transaction model (server + client types in `app_expo/services/types.ts`):

- `source_type`: `manual | sms | pdf | notification`
- `source_hash`: stable idempotency hash
- `source_sender`: SMS sender or parser source
- `source_reference`: UPI/UTR/reference value
- `source_raw_excerpt`: optional trimmed raw text for debugging
- `occurred_at`: timestamp used for dedupe windows
- `account_last4`: optional account/card last4
- `bank_name`: inferred bank/provider

### Suggested indexes (backend)
1. Unique `(user_id, source_hash)`
2. Non-unique `(user_id, source_reference)`
3. Non-unique `(user_id, amount, occurred_at)`


## 6) Parser strategy

### Option A (recommended long-term): shared parser package in TypeScript
Port parser concepts from `pennywiseai-tracker/parser-core`:
- `BankParser` interface
- parser registry/factory
- normalized `ParsedTransaction`
- sender matching + regex extraction

Benefits:
- Same parser used by scan, realtime, and PDF import (if desired)
- Faster local feedback and offline support

### Option B (fastest launch): server-side parsing
- App sends raw SMS/PDF text to backend parse endpoint.
- Backend returns parsed candidates and/or writes directly.

Benefits:
- No initial native parser complexity in app
- Easier to hot-fix parser rules

Tradeoff:
- Less privacy than fully on-device parsing


## 7) SMS ingestion flows

### 7.1 Historical bulk scan flow
1. User opens `Import SMS` modal.
2. App requests Android SMS permission.
3. Native module reads SMS in pages (with progress callbacks).
4. JS parser converts raw SMS to parsed candidates.
5. Dedupe pipeline filters candidates.
6. User sees import summary and confirms.
7. App submits batch to `/ingestion/sms/batch`.
8. Server performs idempotent upsert and returns counters.

### 7.2 Realtime SMS flow
1. Android receiver gets `SMS_RECEIVED_ACTION`.
2. Receiver forwards payload to native module queue.
3. JS listener (`useRealtimeSmsIngestion`) receives normalized event.
4. Parse + dedupe locally.
5. Fire lightweight background API call to ingest.
6. Update UI cache via React Query invalidation.

### 7.3 Unrecognized SMS flow
If sender looks financial but parser fails:
- Store in local "unrecognized" table/list with sender + body hash.
- Provide review screen and "send anonymized sample" action.
- Optionally sync anonymized patterns to backend for parser improvements.


## 8) PDF statement ingestion flows

### 8.1 Client-first flow
1. Use `expo-document-picker` to select PDF.
2. Extract text:
   - either native module bridge to PDF parser,
   - or upload to backend for extraction+parse.
3. Detect parser format (GPay/PhonePe first).
4. Parse into candidates.
5. Apply dedupe tiers.
6. Confirm and submit batch write.

### 8.2 Dedupe tiers (same logic as SMS import)
1. `source_hash` exact match
2. `source_reference` match (UPI/UTR)
3. amount + same-day window match

### 8.3 Initial PDF parser scope
- `v1`: Google Pay and PhonePe exports.
- `v1.1`: extend to bank statements by adding parser implementations.


## 9) Dedupe design (must be deterministic)

### Hash input recommendation
`source_hash = sha256(user_id + source_kind + sender + normalized_amount + normalized_ref + normalized_body_fragment)`

### Rules
- Normalize amount to 2 decimals before hash.
- Normalize sender casing and whitespace.
- Strip volatile tokens (timestamps that change across channels) from body fragment where possible.
- Keep final dedupe decision on backend for consistency across devices.


## 10) UI and UX plan

### New screens/modals
1. `Import SMS`
   - permission status
   - scan window selector (1m/3m/6m/all)
   - progress bar + counters
2. `Import Statement`
   - file picker
   - parser detected
   - parsed count / skipped count
3. `Unrecognized Messages`
   - grouped by sender
   - mark ignored / report sample

### Home integrations
- Add "Import from SMS" and "Import PDF statement" quick actions.
- Show post-import snackbars with imported/skipped counts.


## 11) API design in `server/`

Add ingestion endpoints:

1. `POST /ingestion/sms/batch`
   - body: list of parsed transactions + metadata
   - response: `{ imported, skipped_hash, skipped_reference, skipped_amount_day, errors[] }`

2. `POST /ingestion/pdf/parse`
   - body: extracted text or uploaded file reference
   - response: parsed candidate list + parser info

3. `POST /ingestion/pdf/batch`
   - same write contract as SMS batch

4. `POST /ingestion/unrecognized`
   - anonymized sender/body snippets for parser improvement


## 12) Security and privacy controls

1. Request SMS permission only at import setup step.
2. Explain exactly what is read and why.
3. Allow local-only parse mode (no raw SMS upload).
4. If server parse/upload is enabled, redact sensitive fields before upload when possible.
5. Encrypt auth token storage (consider `expo-secure-store` over plain AsyncStorage for tokens).


## 13) Performance guidance (Expo/React Native)

1. Process SMS in chunks (e.g., 300-500 messages per batch).
2. Avoid heavy parsing on the main JS thread for large scans; offload via chunked async pipeline.
3. Use React Query mutations for batch ingest with retry/backoff.
4. Keep list screens virtualized and memoized for parsed preview rows.
5. Defer expensive operations (formatting/grouping) until after initial render.


## 14) Testing strategy

### Unit tests
- Parser regex extraction by sender/template
- Dedupe hash generation and tier logic
- Date normalization and timezone behavior

### Integration tests
- SMS batch ingest endpoint idempotency
- PDF parse + batch import sequence
- Realtime event -> API write flow

### E2E/device tests
- Android permission flow
- historical scan with 1k+ messages
- app restart during scan and resume behavior
- duplicate prevention across SMS + PDF imports


## 15) Rollout plan

### Phase 0: foundation (1 week)
- Define schemas/types and ingestion API contracts
- Add source metadata fields and indexes
- Build dedupe utilities and tests

### Phase 1: PDF import MVP (1-2 weeks)
- Add `Import Statement` UI
- Implement parse for GPay/PhonePe (client or server)
- Add batch ingest endpoint + summary UI

### Phase 2: SMS historical import MVP (2 weeks)
- Build Android native SMS read bridge (prebuild/custom client)
- Implement chunked parser pipeline + progress UI
- Batch ingest with dedupe summary

### Phase 3: Realtime SMS auto-ingestion (1-2 weeks)
- Add broadcast receiver + event bridge
- Add background-safe ingestion queue
- Add settings toggle and diagnostics

### Phase 4: hardening (ongoing)
- Unrecognized SMS workflow
- parser coverage expansion
- analytics/observability and failure dashboards


## 16) Risk register and mitigations

1. **Expo Go limitation on SMS APIs**
   - Mitigation: prebuild + custom dev client from day one for this feature.

2. **Parser fragility across banks/templates**
   - Mitigation: test corpus, unrecognized sample collection, versioned parser rules.

3. **Duplicate inserts across channels**
   - Mitigation: deterministic hash + backend idempotent writes + multi-tier dedupe.

4. **Large scan performance issues**
   - Mitigation: chunked reading/parsing, throttled UI updates, resumable checkpoints.

5. **Privacy concerns**
   - Mitigation: on-device parse preference + explicit consent + redaction options.


## 17) Concrete first implementation checklist

1. Add backend transaction fields and indexes for source metadata.
2. Create `/ingestion/sms/batch` and `/ingestion/pdf/batch` endpoints.
3. Add `smsImportService.ts` and `statementImportService.ts` in `app_expo/services/`.
4. Add `import-statement.tsx` modal and wire to existing transaction refresh flow.
5. Add parser module (TS) for GPay/PhonePe statement text.
6. Add Android native SMS read module through Expo config plugin.
7. Add `import-sms.tsx` modal with progress and preview.
8. Add dedupe summary UI and import report persistence.


## 18) Definition of done (for v1)

The system is v1-complete when:
- User can import a PDF statement and see accurate transaction inserts with duplicate skips.
- User can run historical SMS import on Android and ingest transactions in bulk.
- Duplicate prevention works across repeated imports.
- Import summary clearly reports imported/skipped/error counts.
- Core parser and dedupe logic are covered by automated tests.
