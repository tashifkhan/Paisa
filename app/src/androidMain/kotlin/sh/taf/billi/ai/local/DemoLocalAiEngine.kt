package sh.taf.billi.ai.local

import sh.taf.billi.ai.LlmException
import sh.taf.billi.ai.LlmFailure
import sh.taf.billi.ai.provider.LlmRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug-only stand-in for Gemini Nano. It exercises the real local ladder,
 * JSON parsing and review flow on an emulator, where AICore doesn't exist.
 * Its replies say "demo" so a screenshot can't be mistaken for a real model.
 */
internal class DemoLocalAiEngine(
    /** When set, refuses to run unless Billi is on screen, like AICore's BACKGROUND_USE_BLOCKED. */
    private val inForeground: (() -> Boolean)? = null,
) : LocalAiEngine {

    private val _status = MutableStateFlow(
        LocalAiStatus(
            state = LocalAiState.AVAILABLE,
            modelName = "nano-v4 (demo)",
            detail = "Demo engine for emulator testing. No model runs and nothing leaves the app.",
            features = setOf(
                LocalAiFeature.TEXT,
                LocalAiFeature.IMAGE,
                LocalAiFeature.JSON,
                LocalAiFeature.SYSTEM_PROMPT,
            ),
            maxInputTokens = 4_000,
            maxOutputTokens = 4_096,
            checkedAt = System.currentTimeMillis(),
        )
    )
    override val status: StateFlow<LocalAiStatus> = _status.asStateFlow()

    override suspend fun refresh(): LocalAiStatus = _status.value

    override suspend fun download(): LocalAiStatus = _status.value

    override suspend fun complete(request: LlmRequest): String {
        if (inForeground?.invoke() == false) {
            throw LlmException("Gemini Nano only runs while Billi is open. (demo)", LlmFailure.CONFIG)
        }
        delay(450)
        if (!request.json) return "OK"
        return when {
            request.system.contains("payment message reader", ignoreCase = true) -> paymentReply(request.prompt)
            request.system.contains("restaurant bills", ignoreCase = true) -> billReply()
            else -> draftReply(request.prompt)
        }
    }

    /**
     * A deterministic stand-in for the local message reader. It pulls the amount
     * and direction out of the prompt so the whole AI fallback path can be
     * exercised on an emulator.
     */
    private fun paymentReply(prompt: String): String {
        val message = prompt.substringAfter("Message from ", prompt)
            .substringAfter('\n', prompt)
            .substringBefore("\n\nReply", prompt)
        val amount = Regex("""(?:₹|\bRs\.?|\bINR)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
            .find(message)
            ?.groupValues
            ?.get(1)
            ?.replace(",", "")
            ?: Regex("""\b(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d{2,}(?:\.\d{1,2})?)\b""")
                .find(message)
                ?.groupValues
                ?.get(1)
                ?.replace(",", "")
        if (amount == null) return """{"isPayment":false}"""
        val income = Regex("""\b(received|credited|salary|added|refund|deposited)\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(message)
        val leading = Regex(
            """^([A-Za-z][A-Za-z0-9 &'.-]{1,30})\s+(?:added|put|paid|sent|received|credited|debited)\b""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        ).find(message)?.groupValues?.get(1)?.trim()
        val merchant = leading ?: Regex("""\b(?:to|from|at)\s+([A-Za-z][A-Za-z0-9 &'.-]{1,30})""")
            .find(message)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val last4 = Regex("""(?:xx+|\*+)\s?(\d{4})\b""", RegexOption.IGNORE_CASE)
            .find(message)
            ?.groupValues
            ?.get(1)
        val reference = Regex("""\b(\d{12})\b""").find(message)?.groupValues?.get(1)
        return buildString {
            append("""{"isPayment":true,"amount":""")
            append(amount)
            append(""","direction":""")
            append(if (income) "\"income\"" else "\"expense\"")
            append(""","merchant":""")
            append(merchant?.let(::json) ?: "null")
            append(""","accountLast4":""")
            append(last4?.let(::json) ?: "null")
            append(""","reference":""")
            append(reference?.let(::json) ?: "null")
            append('}')
        }
    }

    private fun draftReply(prompt: String): String {
        val today = Regex("""\b\d{4}-\d{2}-\d{2}\b""").find(prompt)?.value ?: currentDate()
        val userText = prompt.substringAfter("What the user wrote:", prompt).substringBefore("\n\nThe file")
        val amount = Regex("""(?<![\d-])(\d{1,7}(?:\.\d{1,2})?)(?![\d-])""")
            .findAll(userText)
            .mapNotNull { it.value.toDoubleOrNull() }
            .firstOrNull { it in 1.0..1_000_000.0 }
            ?: 240.0
        val titleRaw = Regex("""(?:at|for|on)\s+([A-Za-z][A-Za-z0-9 &'-]{1,30})""")
            .find(userText)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Local demo"
        val title = titleRaw
            .split(' ')
            .takeWhile { it.lowercase() !in DATE_WORDS }
            .joinToString(" ")
            .ifEmpty { "Local demo" }
        val category = Regex("""Expense categories:\s*([^,\n]+)""")
            .find(prompt)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val categoryJson = category?.let { ",\"category\":${json(it)}" }.orEmpty()
        return buildString {
            append("""{"transactions":[{"amount":""")
            append(amount)
            append(""","type":"expense","title":""")
            append(json(title))
            append(""","note":"Made by the local demo engine","date":""")
            append('"')
            append(today)
            append('"')
            append(categoryJson)
            append("}]}")
        }
    }

    private fun billReply(): String =
        """{"items":[{"name":"Paneer tikka","amount":320},{"name":"Garlic naan","amount":90}],"total":460}"""

    private fun currentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun json(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        val DATE_WORDS = setOf("today", "yesterday", "tomorrow", "morning", "evening")
    }
}
