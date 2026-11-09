package codes.tashif.paisa.ai

import codes.tashif.paisa.ai.ladder.AiLadderException
import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.provider.LlmRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * Extracts structured transactions from a statement through the AI ladders:
 * "Statement photos" for images, "Statement import" for text, CSV and PDF text.
 */
class StatementExtractionService(
    private val ladderRunner: AiLadderRunner
) {
    suspend fun extractFromSource(
        source: StatementSource,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): StatementExtractionResult {
        if (StatementImageReader.isImage(source)) {
            onProgress?.invoke(0, 1)
            val image = StatementImageReader.read(source)
            val response = complete(AiService.STATEMENT_PHOTOS, IMAGE_PROMPT, image = image)
            onProgress?.invoke(1, 1)
            return StatementExtractionResult(
                transactions = StatementJsonParser.parse(response),
                rawModelOutput = response,
                chunksProcessed = 1
            )
        }

        val text = StatementTextExtractor.extract(source)
        if (text.isBlank()) {
            if (StatementTextExtractor.isPdf(source)) {
                extractFromScannedPdf(source, onProgress)?.let { return it }
            }
            throw LlmException(
                "Could not read text from this file. " +
                    "Scanned/image PDFs need a model that reads PDFs. Otherwise try a text PDF, " +
                    "CSV export, or a JPG/PNG/WebP statement image."
            )
        }
        return extractFromText(text, onProgress)
    }

    /**
     * A PDF with no text layer goes whole to a rung that reads PDFs. Returns
     * null when no rung can take it, so the caller shows the usual message.
     */
    private suspend fun extractFromScannedPdf(
        source: StatementSource,
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): StatementExtractionResult? {
        val pdf = StatementTextExtractor.readPdf(source) ?: return null
        onProgress?.invoke(0, 1)
        val response = try {
            complete(AiService.STATEMENT_IMPORT, PDF_PROMPT, pdf = pdf)
        } catch (e: AiLadderException) {
            if (e.failures.all { it.skipped }) return null
            throw e
        }
        onProgress?.invoke(1, 1)
        return StatementExtractionResult(
            transactions = StatementJsonParser.parse(response),
            rawModelOutput = response,
            chunksProcessed = 1
        )
    }

    @OptIn(ExperimentalAtomicApi::class)
    suspend fun extractFromText(
        text: String,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): StatementExtractionResult {
        val chunks = StatementTextExtractor.chunkText(text)
        onProgress?.invoke(0, chunks.size)

        // Chunks are independent: run them in parallel (bounded so we don't
        // hammer rate limits), then merge in chunk order so results stay stable.
        // Each chunk walks the ladder on its own.
        val completed = AtomicInt(0)
        val semaphore = Semaphore(MAX_PARALLEL_CHUNKS)
        val responses = coroutineScope {
            chunks.mapIndexed { index, chunk ->
                async {
                    semaphore.withPermit {
                        val response = complete(
                            AiService.STATEMENT_IMPORT,
                            buildUserPrompt(chunk, index + 1, chunks.size)
                        )
                        onProgress?.invoke(completed.incrementAndFetch(), chunks.size)
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

    private suspend fun complete(
        service: AiService,
        userPrompt: String,
        image: LlmImage? = null,
        pdf: LlmPdf? = null
    ): String = ladderRunner.run(
        service,
        LlmRequest(
            system = SYSTEM_PROMPT,
            prompt = userPrompt,
            json = true,
            image = image,
            pdf = pdf,
            feature = service.id
        )
    ).text

    private fun buildUserPrompt(chunk: String, part: Int, total: Int): String {
        val header = if (total > 1) {
            "This is part $part of $total of a bank/UPI statement.\n\n"
        } else {
            "Bank/UPI statement text:\n\n"
        }
        return header + chunk
    }

    companion object {
        fun dedupeKey(tx: ExtractedTransaction): String =
            listOf(
                tx.date,
                tx.amount.toString(),
                tx.type,
                tx.merchant.lowercase(),
                tx.reference.orEmpty()
            ).joinToString("|")

        private const val MAX_PARALLEL_CHUNKS = 3

        private const val IMAGE_PROMPT =
            "Read this bank or UPI statement image and extract every visible transaction. " +
                "Use only information visible in the image."

        private const val PDF_PROMPT =
            "Read the attached bank or UPI statement PDF and extract every transaction in it."

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
