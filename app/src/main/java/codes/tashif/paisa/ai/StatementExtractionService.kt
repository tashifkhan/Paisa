package codes.tashif.paisa.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Uses the user's BYOK credentials to extract structured transactions from a statement.
 */
class StatementExtractionService(
    private val llmClient: LlmClient = LlmClient()
) {
    suspend fun extractFromUri(
        context: Context,
        uri: Uri,
        credentials: AiCredentials,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): StatementExtractionResult {
        if (StatementImageReader.isImage(context, uri)) {
            onProgress?.invoke(0, 1)
            val image = StatementImageReader.read(context, uri)
            val response = completeWithRetry(
                credentials = credentials,
                userPrompt = IMAGE_PROMPT,
                image = image
            )
            onProgress?.invoke(1, 1)
            return StatementExtractionResult(
                transactions = StatementJsonParser.parse(response),
                rawModelOutput = response,
                chunksProcessed = 1
            )
        }

        val text = StatementTextExtractor.extract(context, uri)
        if (text.isBlank()) {
            throw LlmException(
                "Could not read text from this file. " +
                    "Scanned/image PDFs are not supported yet — try a text PDF, CSV export, " +
                    "or a JPG/PNG/WebP statement image."
            )
        }
        return extractFromText(text, credentials, onProgress)
    }

    suspend fun extractFromText(
        text: String,
        credentials: AiCredentials,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): StatementExtractionResult {
        val chunks = StatementTextExtractor.chunkText(text)
        onProgress?.invoke(0, chunks.size)

        // Chunks are independent — run them in parallel (bounded so we don't
        // hammer rate limits), then merge in chunk order so results stay stable.
        val completed = AtomicInteger(0)
        val semaphore = Semaphore(MAX_PARALLEL_CHUNKS)
        val responses = coroutineScope {
            chunks.mapIndexed { index, chunk ->
                async {
                    semaphore.withPermit {
                        val response = completeWithRetry(
                            credentials = credentials,
                            userPrompt = buildUserPrompt(chunk, index + 1, chunks.size)
                        )
                        onProgress?.invoke(completed.incrementAndGet(), chunks.size)
                        response
                    }
                }
            }.map { it.await() }
        }

        val all = LinkedHashMap<String, ExtractedTransaction>()
        for (response in responses) {
            for (tx in StatementJsonParser.parse(response)) {
                val key = dedupeKey(tx)
                if (!all.containsKey(key)) {
                    all[key] = tx
                }
            }
        }

        return StatementExtractionResult(
            transactions = all.values.sortedByDescending { it.date },
            rawModelOutput = responses.joinToString("\n---\n"),
            chunksProcessed = chunks.size
        )
    }

    private suspend fun completeWithRetry(
        credentials: AiCredentials,
        userPrompt: String,
        image: LlmImage? = null
    ): String {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return llmClient.complete(
                    credentials = credentials,
                    systemPrompt = SYSTEM_PROMPT,
                    userPrompt = userPrompt,
                    jsonOutput = true,
                    image = image
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                lastError = e
                if (attempt < MAX_ATTEMPTS - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        throw lastError ?: LlmException("Extraction failed")
    }

    private fun dedupeKey(tx: ExtractedTransaction): String =
        listOf(
            tx.date,
            tx.amount.toString(),
            tx.type,
            tx.merchant.lowercase(),
            tx.reference.orEmpty()
        ).joinToString("|")

    private fun buildUserPrompt(chunk: String, part: Int, total: Int): String {
        val header = if (total > 1) {
            "This is part $part of $total of a bank/UPI statement.\n\n"
        } else {
            "Bank/UPI statement text:\n\n"
        }
        return header + chunk
    }

    companion object {
        private const val MAX_PARALLEL_CHUNKS = 3
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1_500L

        private const val IMAGE_PROMPT =
            "Read this bank or UPI statement image and extract every visible transaction. " +
                "Use only information visible in the image."

        val SYSTEM_PROMPT = """
            You extract financial transactions from bank or UPI statements.
            Return ONLY valid JSON (no markdown, no commentary) with this shape:
            {
              "transactions": [
                {
                  "date": "YYYY-MM-DD",
                  "amount": 123.45,
                  "type": "expense",
                  "merchant": "Merchant name",
                  "currency": "INR",
                  "reference": "optional ref",
                  "rawLine": "optional original line",
                  "category": "optional category guess"
                }
              ]
            }
            Rules:
            - type is "expense" for debits/payments/withdrawals/DR, "income" for credits/salary/refunds/CR.
            - amount is always a positive plain number: strip currency symbols and thousands
              separators ("1,23,456.78" -> 123456.78). Never confuse the running balance
              column with the transaction amount.
            - Indian statements write dates day-first: 04/07/2026 and 04-07-26 both mean 2026-07-04.
              Always output ISO YYYY-MM-DD.
            - merchant is the counterparty, not the narration. For UPI rows like
              "UPI/DR/516912345678/SWIGGY/..." the merchant is "Swiggy". Strip prefixes such as
              UPI/, NEFT/, IMPS/, POS/, VPA handles, and trailing reference numbers.
            - reference is the UPI/UTR/cheque number when present — copy it exactly.
            - Skip rows that are not real transactions: opening/closing balance, totals,
              interest summaries, page headers/footers, "brought forward" lines.
            - A single transaction may span multiple lines of text — join them into one entry.
            - If unsure of type, use "expense".
            - Do not invent, merge, or drop transactions; every real row in the text appears
              exactly once.
        """.trimIndent()
    }
}
