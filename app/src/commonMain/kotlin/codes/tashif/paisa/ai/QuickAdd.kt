package codes.tashif.paisa.ai

import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject
import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.provider.LlmRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** A transaction the model drafted, before the user reviews it. */
data class TransactionDraft(
    val amount: Double,
    val type: String,
    val title: String,
    val note: String = "",
    /** yyyy-MM-dd */
    val date: String,
    /** HH:mm, or null when the input didn't say. */
    val time: String? = null,
    val categoryName: String? = null,
    val accountName: String? = null,
)

/** What the model needs to know about the ledger to fill in names it can match. */
data class QuickAddContext(
    /** yyyy-MM-dd */
    val today: String,
    /** Weekday name for today, so "last friday" resolves. */
    val weekday: String,
    val currency: String,
    val expenseCategories: List<String>,
    val incomeCategories: List<String>,
    /** "HDFC Savings (Bank Account, HDFC Bank, ••1234)" style lines. */
    val accounts: List<String>,
)

/** One draft and where it came from: the note, or a file's name. */
data class SourcedDraft(val draft: TransactionDraft, val origin: String)

/** A file or the note that gave nothing back or failed. */
data class QuickAddFailure(val origin: String, val message: String)

data class QuickAddResult(
    val drafts: List<SourcedDraft>,
    val failures: List<QuickAddFailure>,
)

/**
 * Turns a free-text note, receipts, bills, screenshots and PDFs into draft
 * transactions through the "Quick add" ladder. Each file is one call, with
 * the note sent along as context.
 */
class QuickAddService(private val ladderRunner: AiLadderRunner) {

    fun canRun(): Boolean = ladderRunner.canRun(AiService.QUICK_ADD)

    suspend fun draft(
        note: String,
        files: List<StatementSource>,
        context: QuickAddContext,
    ): QuickAddResult {
        val cleanNote = note.trim()
        if (files.isEmpty()) {
            if (cleanNote.isEmpty()) return QuickAddResult(emptyList(), emptyList())
            return try {
                val response = complete(
                    buildString {
                        append(contextBlock(context))
                        append("\n\nWhat the user wrote:\n")
                        append(cleanNote)
                    }
                )
                QuickAddResult(
                    TransactionDraftParser.parse(response, context.today).map { SourcedDraft(it, NOTE_ORIGIN) },
                    emptyList()
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                QuickAddResult(emptyList(), listOf(QuickAddFailure(NOTE_ORIGIN, e.message ?: "The AI call failed")))
            }
        }

        val names = files.mapIndexed { i, file -> runCatching { file.displayName() }.getOrNull() ?: file.name ?: "File ${i + 1}" }
        val semaphore = Semaphore(MAX_PARALLEL)
        val results = coroutineScope {
            files.mapIndexed { index, file ->
                async {
                    semaphore.withPermit {
                        try {
                            val response = draftFromFile(file, index, names, cleanNote, context)
                            Result.success(response)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                }
            }.map { it.await() }
        }

        val drafts = mutableListOf<SourcedDraft>()
        val failures = mutableListOf<QuickAddFailure>()
        results.forEachIndexed { index, result ->
            result.fold(
                onSuccess = { response ->
                    val parsed = TransactionDraftParser.parse(response, context.today)
                    if (parsed.isEmpty()) {
                        failures += QuickAddFailure(names[index], "No transactions found in it")
                    }
                    parsed.forEach { drafts += SourcedDraft(it, names[index]) }
                },
                onFailure = { failures += QuickAddFailure(names[index], it.message ?: "Couldn't read it") }
            )
        }
        return QuickAddResult(drafts, failures)
    }

    /**
     * Applies [instruction] to the current drafts, like "make the uber one
     * cash" or "split the dinner into two". Returns the whole new list.
     */
    suspend fun refine(
        instruction: String,
        drafts: List<TransactionDraft>,
        context: QuickAddContext,
    ): List<TransactionDraft> {
        val response = complete(
            buildString {
                append(contextBlock(context))
                append("\n\nCurrent drafts:\n")
                append(TransactionDraftParser.toJson(drafts))
                append("\n\nChange them like this, then return the full updated list:\n")
                append(instruction.trim())
            }
        )
        return TransactionDraftParser.parse(response, context.today)
    }

    private suspend fun draftFromFile(
        file: StatementSource,
        index: Int,
        names: List<String>,
        note: String,
        context: QuickAddContext,
    ): String {
        val header = buildString {
            append(contextBlock(context))
            append("\n\n")
            if (names.size > 1) {
                append("The user attached ${names.size} files. This is file ${index + 1}, \"${names[index]}\".\n")
            } else {
                append("The user attached \"${names[index]}\".\n")
            }
            if (note.isNotEmpty()) {
                append("Their note, which applies to all the files:\n")
                append(note)
                append("\n")
                append(
                    if (index == 0) {
                        "Use the note to fill in or correct details. If it also describes payments that " +
                            "clearly aren't in any file, include those too."
                    } else {
                        "Use the note only to fill in or correct details of what's in this file. " +
                            "Don't add payments from the note alone."
                    }
                )
                append("\n")
            }
        }

        if (StatementImageReader.isImage(file)) {
            val image = StatementImageReader.read(file)
            return complete("$header\nRead the attached image.", image = image)
        }

        val text = StatementTextExtractor.extract(file)
        if (text.isNotBlank()) {
            return complete("$header\nThe file's text:\n\n${text.take(MAX_TEXT_CHARS)}")
        }
        if (StatementTextExtractor.isPdf(file)) {
            val pdf = StatementTextExtractor.readPdf(file)
                ?: throw LlmException("Couldn't open this PDF")
            return complete("$header\nRead the attached PDF.", pdf = pdf)
        }
        throw LlmException("Couldn't read this file. Try a photo, a PDF or a CSV.")
    }

    private suspend fun complete(prompt: String, image: LlmImage? = null, pdf: LlmPdf? = null): String =
        ladderRunner.run(
            AiService.QUICK_ADD,
            LlmRequest(
                system = SYSTEM_PROMPT,
                prompt = prompt,
                json = true,
                image = image,
                pdf = pdf,
                feature = AiService.QUICK_ADD.id
            )
        ).text

    private fun contextBlock(context: QuickAddContext): String = buildString {
        append("Today is ${context.weekday}, ${context.today}. The user's currency is ${context.currency}.\n")
        append("Expense categories: ${context.expenseCategories.joinToString(", ").ifEmpty { "none" }}\n")
        append("Income categories: ${context.incomeCategories.joinToString(", ").ifEmpty { "none" }}\n")
        append("Accounts:\n")
        if (context.accounts.isEmpty()) append("- none\n")
        context.accounts.forEach { append("- ").append(it).append('\n') }
    }

    companion object {
        const val NOTE_ORIGIN = "your note"
        private const val MAX_PARALLEL = 3
        private const val MAX_TEXT_CHARS = 24_000

        val SYSTEM_PROMPT = """
            You turn what someone tells you about their money into transactions for a personal
            expense tracker. The input can be a plain description, a receipt or bill photo, a
            payment screenshot, a PDF invoice or statement, or a mix of these.
            Return ONLY valid JSON (no markdown, no commentary) with this shape:
            {
              "transactions": [
                {
                  "amount": 240,
                  "type": "expense",
                  "title": "Chaayos",
                  "note": "",
                  "date": "YYYY-MM-DD",
                  "time": "HH:mm",
                  "category": "Food",
                  "account": "HDFC Savings"
                }
              ]
            }
            Rules:
            - One entry per separate payment. A receipt or bill is one transaction for its final
              total (after tax and tip), not one per line item, unless the user asks otherwise.
            - amount is a positive plain number: no currency symbols or thousands separators.
            - type is "expense" for money going out and "income" for money coming in (salary,
              refunds, someone paying the user back).
            - title is short: the merchant or payee ("Swiggy", "Ravi"), or what it was for
              ("Rent", "Petrol") when there's no merchant.
            - note holds a useful extra detail from the input (items bought, who it was with),
              or "" when there's nothing worth keeping. Keep it under 80 characters.
            - date: resolve words like "yesterday" or "last friday" against today's date. Receipts
              and screenshots use their printed date. With no date at all, use today.
              Day-first dates such as 04/07/2026 mean 4 July 2026.
            - time is 24-hour HH:mm when the input gives one. Leave it out otherwise.
            - category must be copied exactly from the list for that type. Leave it out if
              nothing fits.
            - account must be copied exactly from the account list, and only when the input
              says what paid: a card's last digits, a bank, UPI app, "cash". Leave it out otherwise.
            - Never invent payments. If there are none, return {"transactions": []}.
        """.trimIndent()
    }
}

/** Reads [TransactionDraft]s out of model output. Tolerant of fences and loose types. */
object TransactionDraftParser {
    fun parse(modelOutput: String, today: String): List<TransactionDraft> {
        val jsonText = StatementJsonParser.extractJson(modelOutput) ?: return emptyList()
        return try {
            val array = if (jsonText.trimStart().startsWith("[")) {
                JSONArray(jsonText)
            } else {
                val obj = JSONObject(jsonText)
                obj.optJSONArray("transactions") ?: obj.optJSONArray("data") ?: return emptyList()
            }
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let { obj -> parseOne(obj, today) } }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseOne(obj: JSONObject, today: String): TransactionDraft? {
        val amount = parseAmount(obj.opt("amount")) ?: return null
        if (amount <= 0.0 || !amount.isFinite()) return null
        val typeRaw = obj.optString("type").lowercase()
        val type = if (typeRaw.contains("income") || typeRaw.contains("credit")) "income" else "expense"
        val title = obj.optString("title")
            .ifBlank { obj.optString("merchant") }
            .ifBlank { obj.optString("description") }
            .trim()
            .ifBlank { if (type == "income") "Income" else "Expense" }
        val date = StatementJsonParser.normalizeDate(obj.optString("date")) ?: today
        val time = Regex("""^(\d{1,2}):(\d{2})""").find(obj.optString("time").trim())?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) "${h.toString().padStart(2, '0')}:${it.groupValues[2]}" else null
        }
        return TransactionDraft(
            amount = amount,
            type = type,
            title = title.take(80),
            note = obj.optString("note").trim().take(200),
            date = date,
            time = time,
            categoryName = obj.optString("category").trim().takeIf { it.isNotEmpty() },
            accountName = obj.optString("account").trim().takeIf { it.isNotEmpty() },
        )
    }

    private fun parseAmount(raw: Any?): Double? = when (raw) {
        null -> null
        is Number -> kotlin.math.abs(raw.toDouble())
        else -> raw.toString().replace(Regex("""[^\d.]"""), "").toDoubleOrNull()
    }

    fun toJson(drafts: List<TransactionDraft>): String {
        val array = JSONArray()
        drafts.forEach { d ->
            val obj = JSONObject()
                .put("amount", d.amount)
                .put("type", d.type)
                .put("title", d.title)
                .put("note", d.note)
                .put("date", d.date)
            d.time?.let { obj.put("time", it) }
            d.categoryName?.let { obj.put("category", it) }
            d.accountName?.let { obj.put("account", it) }
            array.put(obj)
        }
        return JSONObject().put("transactions", array).toString()
    }
}
