# PennyWise Tracker Parsing and Transaction Ingestion Plan

This document explains, in detail, how `pennywiseai-tracker/` parses SMS and PDF statements, then converts parsed data into saved transactions.

It covers:
- Real-time SMS parsing and save flow
- Bulk/backfill SMS scan flow
- Bank-notification-to-transaction flow
- PDF statement parsing and import flow (Android app + shared module)
- Deduplication, rules, subscriptions, balances, and unrecognized SMS handling


## 1) System map (where logic lives)

### SMS and transaction ingestion (Android app module)
- Real-time SMS receiver: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/receiver/SmsBroadcastReceiver.kt`
- Shared processing service: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/manager/SmsTransactionProcessor.kt`
- Bulk scanner worker: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/worker/OptimizedSmsReaderWorker.kt`
- Work scheduling helper: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/manager/SmsScanManager.kt`
- Bank app notification listener: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/receiver/BankNotificationListenerService.kt`
- Retry worker for failed notifications: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/worker/BankNotificationRetryWorker.kt`

### Parser engine (shared by SMS + PDF models)
- Base parser class: `pennywiseai-tracker/parser-core/src/main/kotlin/com/pennywiseai/parser/core/bank/BankParser.kt`
- Parser selection factory: `pennywiseai-tracker/parser-core/src/main/kotlin/com/pennywiseai/parser/core/bank/BankParserFactory.kt`
- Parsed model: `pennywiseai-tracker/parser-core/src/main/kotlin/com/pennywiseai/parser/core/ParsedTransaction.kt`

### Persistence and mapping
- Parsed -> DB entity mapper: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/mapper/ParsedTransactionMapper.kt`
- Transaction repository: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/repository/TransactionRepository.kt`
- Transaction DAO: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/database/dao/TransactionDao.kt`
- Transaction entity/table: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/database/entity/TransactionEntity.kt`

### PDF import (Android app module)
- Import use case: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/statement/ImportStatementUseCase.kt`
- Text extraction: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/statement/PdfTextExtractor.kt`
- Parser factory: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/statement/PdfParserFactory.kt`
- GPay parser: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/statement/GPayPdfParser.kt`
- PhonePe parser: `pennywiseai-tracker/app/src/main/java/com/pennywiseai/tracker/data/statement/PhonePePdfParser.kt`

### PDF import (KMP shared module)
- Shared import use case: `pennywiseai-tracker/shared/src/commonMain/kotlin/com/pennywiseai/shared/domain/usecase/ImportStatementUseCase.kt`
- Shared parser factory: `pennywiseai-tracker/shared/src/commonMain/kotlin/com/pennywiseai/shared/data/statement/SharedStatementParserFactory.kt`
- Shared GPay parser: `pennywiseai-tracker/shared/src/commonMain/kotlin/com/pennywiseai/shared/data/statement/GPaySharedStatementParser.kt`
- Shared PhonePe parser: `pennywiseai-tracker/shared/src/commonMain/kotlin/com/pennywiseai/shared/data/statement/PhonePeSharedStatementParser.kt`
- Shared Android PDF extraction: `pennywiseai-tracker/shared/src/androidMain/kotlin/com/pennywiseai/shared/data/statement/SharedPdfTextExtractor.android.kt`
- Shared iOS PDF extraction placeholder: `pennywiseai-tracker/shared/src/iosMain/kotlin/com/pennywiseai/shared/data/statement/SharedPdfTextExtractor.ios.kt`


## 2) SMS parsing architecture (end-to-end)

There are three input channels for transaction-like text:
1. Real-time SMS broadcast (`SmsBroadcastReceiver`)
2. Bulk inbox scan (`OptimizedSmsReaderWorker`)
3. Bank app notifications (`BankNotificationListenerService`)

All transaction inserts eventually use the same core flow: parser -> mapped entity -> dedupe/rules/subscription -> DB insert -> balance updates.


## 3) Real-time SMS flow (`SmsBroadcastReceiver`)

### 3.1 Trigger
- Receiver listens for `Telephony.Sms.Intents.SMS_RECEIVED_ACTION`.

### 3.2 Multipart SMS merge
- Android may deliver multipart SMS in fragments.
- Receiver groups fragments by sender and concatenates message body.
- Earliest fragment timestamp is retained for the combined message.

### 3.3 Processing call
- For each combined message, receiver calls:
  - `SmsTransactionProcessor.processAndSaveTransaction(sender, body, timestamp)`

### 3.4 Optional user notification
- If processing succeeded and app is backgrounded:
  - Re-parses SMS to build human-readable notification title/body
  - Loads saved transaction for category
  - Shows quick-action notification (suggested top categories)


## 4) Bulk/backfill SMS scan flow (`OptimizedSmsReaderWorker`)

This worker is the heavy-duty ingestion path (historical scans, periodic scans, resync).

### 4.1 Work scheduling
- `SmsScanManager.startSmsLoggingScan()` enqueues unique work named `optimized_sms_reader_work`.

### 4.2 Force resync behavior
- If `INPUT_FORCE_RESYNC` is true, worker clears:
  - all transactions
  - all balances

### 4.3 Message loading
- Reads SMS from `Telephony.Sms.CONTENT_URI` filtered by:
  - inbox message type
  - computed `scanStartTime`
- Scan window depends on user preferences:
  - last scan timestamp
  - configured scan months
  - scan-all-time flag
  - period changes and toggle transitions
- Also attempts RCS/MMS read (`content://mms`) for supported patterns.

### 4.4 Pipeline design
- Stage 1 (IO): feed messages into channel
- Stage 2 (CPU parallel): parse with N coroutines
- Stage 3 (IO single coroutine): save sequentially

Why this design:
- Parsing is CPU-bound and parallelizable.
- DB writes and balance updates are sequenced to avoid race conditions.

### 4.5 Parser caching and DB cache preloads
- Caches sender -> parser mapping (`ConcurrentHashMap`) to avoid repeated factory resolution.
- Preloads merchant mappings once.
- Preloads active rules by transaction type once.

### 4.6 Parse outcomes (sealed result)
Each SMS becomes one of:
- `Discard`: ignore noisy/non-transaction message
- `StoreUnrecognized`: sender likely financial (`-T` or `-S`) but no parser found
- `SpecialNotification`: non-transaction update (mandates/balance notices)
- `Transaction`: normal transaction for save flow

### 4.7 Unrecognized SMS handling
- Batched insertion (size 50) into `unrecognized_sms` table.
- Prevents duplicates by `(sender, sms_body)` unique index.
- Cleanup removes old entries (>30 days).

### 4.8 Special-notification handling
Worker checks parser-specific notification types before normal transaction parse:
- UPI mandates/subscription notices (e.g., SBI/PNB/Federal/HDFC/Indian Bank)
- Balance update notices (e.g., HDFC/IndusInd)

These are saved via repository callbacks in sequential stage.

### 4.9 Progress reporting
- Reports total/processed/parsed/saved/blocked.
- Calculates throughput + ETA with sliding-window estimator.
- UI reads these keys for live progress dialogs.


## 5) Parser-core mechanics (how SMS text becomes `ParsedTransaction`)

### 5.1 Parser selection
- `BankParserFactory.getParser(sender)` finds first parser whose `canHandle(sender)` is true.
- Order matters in factory list (some parsers must precede broader matchers).

### 5.2 Base parser (`BankParser`) default workflow
For most banks, parse logic follows this template:
1. Reject obvious non-transaction content (`isTransactionMessage`): OTPs, promos, reminders, collect requests, etc.
2. Extract amount (`extractAmount`)
3. Infer transaction type (`extractTransactionType`)
4. Extract optional fields:
   - merchant
   - reference/UTR
   - account last4
   - balance
   - available credit limit
   - currency
   - card-vs-account detection
5. Return `ParsedTransaction`

### 5.3 Bank-specific overrides
- Individual bank parsers override extraction methods and patterns as needed.
- This is how app supports many regions/banks while reusing shared logic.

### 5.4 `ParsedTransaction` dedupe identity helper
- `ParsedTransaction.generateTransactionId()` builds hash from:
  - sender
  - normalized amount
  - md5(smsBody) fragment
- Purpose: stable dedupe across differing timestamp sources.


## 6) Transaction save and enrichment flow (`SmsTransactionProcessor` and worker save stage)

Both real-time and worker flows converge on similar save logic.

### 6.1 Parsed -> entity mapping
- `ParsedTransactionMapper.toEntity()` maps parsed model to `TransactionEntity`:
  - timestamp -> `LocalDateTime`
  - type conversion
  - merchant normalization
  - auto-category via `SharedCategoryMapping`
  - `transactionHash` fallback to generated ID

### 6.2 Deduplication (primary)
- Lookup by `transaction_hash` before insert.
- `transactions` table enforces unique index on `transaction_hash`.

### 6.3 Soft-delete behavior
- Soft-deleting a transaction mutates hash to `DELETED_<id>_<oldHash>`.
- This intentionally allows re-import/new insert of equivalent future transactions.
- In processor path, if hash exists and row is already deleted, message is skipped as "previously deleted".

### 6.4 Merchant mapping override
- If user created merchant -> category mapping, mapped category replaces inferred one.

### 6.5 Rules engine
- Loads active rules by transaction type.
- Checks blocking rules first (`shouldBlockTransaction`).
- Applies non-blocking transformations (`evaluateRules`).
- Persists rule applications when rules fired.

### 6.6 Subscription matching
- Attempts `matchTransactionToSubscription(merchant, amount)`.
- If matched:
  - updates next payment date
  - marks transaction as recurring

### 6.7 Insert
- DAO insert uses `OnConflictStrategy.IGNORE`.
- Insert success is rowId != -1.

### 6.8 Balance and card/account updates
- If account last4 exists, updates card/account balance history.
- Handles:
  - card creation/find for card-like sources
  - credit-card outstanding logic
  - explicit balance from SMS when available
  - fallback computed balance using transaction type

### 6.9 Post-insert side effects
- Triggers recent-transactions widget refresh.
- Worker completion may also refresh LLM system prompt when new transactions were added.


## 7) Bank app notification ingestion flow

### 7.1 Entry
- `BankNotificationListenerService` receives posted notifications.
- Only allowed package names are processed (`BankNotificationConfig`).

### 7.2 Preprocessing
- Skips group summary notifications.
- Extracts body text from notification payload.
- Maps package -> sender alias for parser compatibility.

### 7.3 Cross-source dedupe against SMS
- Service parses notification body first.
- Checks for existing transaction with same amount in +-2 minute window.
- If same bank already exists in that window, skip to avoid duplicate SMS+notification inserts.

### 7.4 Processing and retry
- Calls `SmsTransactionProcessor.processAndSaveTransaction(...)`.
- On failures, enqueues `BankNotificationRetryWorker` (15-minute delayed one-shot).


## 8) PDF statement import flow (Android app module)

### 8.1 Entry use case
- `ImportStatementUseCase.import(uri)`.

### 8.2 Text extraction
- `PdfTextExtractor.extractText(context, uri)`:
  - initializes PDFBox resources once
  - reads PDF via content resolver
  - extracts text with `PDFTextStripper`

### 8.3 Parser selection
- `PdfParserFactory.getParser(text)` returns first parser that can handle text.
- Current support: Google Pay and PhonePe.

### 8.4 Parser-specific logic

#### GPay parser (`GPayPdfParser`)
- Detects by `google pay`/`gpay` + `upi transaction id` marker.
- Splits statement text into transaction blocks using anchors:
  - `Paid to ...` (expense)
  - `Received from ...` (income)
- Avoids misclassifying account lines as transaction anchors.
- Extracts:
  - merchant
  - amount
  - UPI transaction ID
  - account/bank line (`Paid by/to ... Bank/Card/A/c 1234`)
  - timestamp from triplet lines (date + year + time)

#### PhonePe parser (`PhonePePdfParser`)
- Detects by PhonePe keywords.
- Splits blocks by debit/credit and paid/received anchors.
- Extracts:
  - amount
  - type (debit/credit or semantic anchor)
  - merchant
  - transaction ID/UTR
  - timestamp (multiple date formats)

### 8.5 PDF import dedupe tiers
Inside `ImportStatementUseCase`:
1. Tier 0: hash dedupe (`transaction_hash`)
2. Tier 1: reference dedupe (`reference` / UPI/UTR)
3. Tier 2: amount + same calendar day dedupe

### 8.6 Insert and result
- Converts parsed records to `TransactionEntity` via `toEntity()`.
- Bulk inserts only non-duplicates.
- Returns result with counts:
  - imported
  - skipped duplicates (with breakdown)
  - total parsed


## 9) PDF import flow (KMP shared module)

There is a parallel statement-import implementation in shared code.

### 9.1 Entry
- `shared ... ImportStatementUseCase.importFromPdfPath(filePath)`
- or `importFromText(statementText)`

### 9.2 Parser selection and parse
- `SharedStatementParserFactory` picks parser (GPay/PhonePe shared variants).
- Shared parsers output `SharedParsedStatementTransaction` with:
  - amount in minor units
  - transaction type
  - merchant
  - reference
  - account/bank
  - timestamp
  - raw text

### 9.3 Deduplication
- Hash built from raw text + amount minor + timestamp.
- Reference dedupe.
- Amount + day-window dedupe.

### 9.4 Insert and account bootstrap
- Inserts unique transactions into shared repository.
- Auto-creates account balance stubs for new `(bankName, accountLast4)` pairs.

### 9.5 Platform extraction differences
- Android shared implementation can extract PDF text directly via PDFBox.
- iOS shared implementation currently throws unsupported operation and expects text extraction in Swift/PDFKit then `importFromText(...)`.


## 10) Important dedupe and data-integrity rules (quick reference)

1. Primary uniqueness key is `transaction_hash` (DB unique index).
2. PDF imports add secondary dedupe by `reference` and amount+date.
3. Notification ingestion adds cross-source dedupe window against SMS.
4. Soft delete changes hash, so future equivalent records can be inserted.
5. Worker serializes save stage to prevent balance race conditions.


## 11) Current format support and limitations

### SMS
- Very broad bank coverage via parser-core factory list.
- Coverage quality depends on parser-specific patterns and sender matching.

### PDF
- Explicitly supported statement formats: Google Pay and PhonePe.
- Other PDF layouts return unsupported-format error.

### iOS shared PDF extraction
- Not wired in Kotlin shared layer yet; requires host-side extraction.


## 12) End-to-end sequence examples

### A) New SMS arrives (real-time)
1. `SmsBroadcastReceiver` receives and merges message parts.
2. Calls `SmsTransactionProcessor.processAndSaveTransaction`.
3. Processor resolves parser from sender.
4. Parser extracts transaction fields.
5. Processor maps, dedupes, applies merchant mapping/rules/subscription.
6. Transaction inserted.
7. Balance update stored.
8. Optional user notification shown if app in background.

### B) User runs full SMS scan
1. `SmsScanManager` enqueues `OptimizedSmsReaderWorker`.
2. Worker reads SMS/RCS history by configured window.
3. Parallel parse workers classify each message.
4. Single saver coroutine writes valid transactions in order.
5. Unrecognized messages are batched to `unrecognized_sms`.
6. Progress is continuously reported to UI.

### C) User imports PDF statement
1. `ImportStatementUseCase.import(uri)` extracts PDF text.
2. Parser factory selects GPay/PhonePe parser.
3. Parser emits transaction candidates.
4. 3-tier dedupe filters existing matches.
5. Unique transactions inserted.
6. UI receives import summary.


## 13) If you want to extend this system

### Add a new SMS bank parser
1. Create parser in `parser-core/.../bank/`.
2. Implement `canHandle(sender)` and parsing overrides.
3. Register parser in `BankParserFactory.parsers` in correct order.
4. Add parser tests under `parser-core/src/test/...`.

### Add a new PDF format
1. Implement `PdfStatementParser` (app) and/or shared parser equivalent.
2. Add robust `canHandle(text)` markers.
3. Register in parser factory.
4. Validate dedupe interactions (hash/reference/amount-day).


## 14) Practical debugging checklist

When a transaction is missing, inspect in this order:
1. Did sender resolve to parser in `BankParserFactory`?
2. Did parser reject as non-transaction (`isTransactionMessage`) or fail amount/type extraction?
3. Did dedupe skip it (hash/reference/amount-day)?
4. Was it blocked by rules?
5. Was it intentionally skipped due to prior deletion?
6. If unknown sender with `-T`/`-S`, did it land in `unrecognized_sms`?

When PDF import fails:
1. Confirm extractor produced text (not empty/noisy glyphs).
2. Confirm parser factory recognized the format.
3. Confirm parser split blocks correctly.
4. Confirm duplicates were not filtered by the 3 dedupe tiers.


## 15) Key takeaway

`pennywiseai-tracker` uses a layered ingestion design:
- parser-core for bank-specific text understanding,
- shared processor/save rules for data integrity,
- worker pipeline for scalable history import,
- and strict dedupe paths across SMS, notifications, and PDFs.

That combination is what allows both real-time and historical transaction ingestion while minimizing duplicates and preserving user customizations (rules/categories/subscriptions).
