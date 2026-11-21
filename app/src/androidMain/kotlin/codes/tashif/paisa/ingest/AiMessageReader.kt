package codes.tashif.paisa.ingest

import codes.tashif.paisa.ai.json.JSONObject
import codes.tashif.paisa.ai.local.LocalAiEngine
import codes.tashif.paisa.ai.local.LocalAiState
import codes.tashif.paisa.ai.provider.LlmRequest
import codes.tashif.paisa.ai.store.AiConfigStore
import codes.tashif.paisa.ai.store.MessageReaderMode
import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.notification.PaymentText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode

/** A cloud model for unreadable messages. The app wires it to the "SMS and alerts" ladder. */
interface MessageCloud {
    /** "Use AI" is on and the ladder has a cloud model with a key. */
    suspend fun canRun(): Boolean

    suspend fun complete(request: LlmRequest): String
}

/**
 * A model as the last resort for a bank SMS or app notification the rules
 * couldn't read. [MessageReaderMode] picks Gemini Nano on this phone or the
 * user's cloud model; with OFF nothing is asked.
 */
class AiMessageReader(
    private val engine: LocalAiEngine,
    private val config: AiConfigStore,
    private val cloud: MessageCloud? = null,
) {

    sealed interface Result {
        data class Parsed(val transaction: ParsedTransaction) : Result

        /** The model read it and says it isn't a payment, or answered with something unusable. Don't ask again. */
        data object NotPayment : Result

        /** The model couldn't run now (Paisa in the background, or no network). Ask again later. */
        data object Unavailable : Result
    }

    private val refreshMutex = Mutex()

    @Volatile
    private var lastCheckAt = 0L

    val mode: MessageReaderMode get() = config.config.value.messageReader

    /**
     * A model can be asked at all: on-device needs a phone with Gemini Nano, cloud
     * needs "Use AI" and a cloud model. Otherwise messages go to review instead of
     * waiting forever for a model that will never answer.
     */
    suspend fun active(): Boolean = when (mode) {
        MessageReaderMode.OFF -> false
        MessageReaderMode.ON_DEVICE -> engine.status.value.supported
        MessageReaderMode.CLOUD -> cloud?.canRun() == true
    }

    /** True when [message] is worth keeping for a later attempt. */
    suspend fun worthQueueing(message: String): Boolean = PaymentText.worthAskingModel(message) && active()

    /**
     * Asks the model about one message. [bankName] is the bank or app it came
     * from, so the saved row lands on the right account.
     */
    suspend fun read(message: String, sender: String, bankName: String, timestamp: Long): Result {
        if (!PaymentText.worthAskingModel(message)) return Result.NotPayment
        if (!active() || !modelReady()) return Result.Unavailable

        val request = LlmRequest(system = SYSTEM, prompt = prompt(message, bankName), json = true, feature = "message_parse")
        val reply = try {
            when (mode) {
                MessageReaderMode.ON_DEVICE -> engine.complete(request)
                MessageReaderMode.CLOUD -> cloud?.complete(request) ?: return Result.Unavailable
                MessageReaderMode.OFF -> return Result.Unavailable
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Result.Unavailable
        }

        val json = objectOf(reply) ?: return Result.NotPayment
        if (!json.optBoolean("isPayment", false)) return Result.NotPayment

        val type = when (json.optString("direction").trim().lowercase()) {
            "income", "credit", "in" -> TransactionType.INCOME
            "expense", "debit", "out" -> TransactionType.EXPENSE
            else -> return Result.NotPayment
        }
        val amount = amountOf(json.opt("amount")) ?: return Result.NotPayment
        if (amount <= BigDecimal.ZERO) return Result.NotPayment
        // The model may not invent a number that isn't in the message.
        if (!amountAppears(message, amount)) return Result.NotPayment

        return Result.Parsed(
            ParsedTransaction(
                amount = amount,
                type = type,
                merchant = text(json, "merchant")?.takeIf { it.length >= 2 && !ownAccount.matches(it) }?.take(60),
                reference = text(json, "reference")?.takeIf { it.length in 4..30 && it.none(Char::isWhitespace) },
                accountLast4 = text(json, "accountLast4")?.takeIf { it.length == 4 && it.all(Char::isDigit) },
                balance = null,
                smsBody = message,
                sender = sender,
                timestamp = timestamp,
                bankName = bankName,
                currency = "INR"
            )
        )
    }

    private suspend fun modelReady(): Boolean {
        if (mode == MessageReaderMode.CLOUD) return true
        if (engine.status.value.ready) return true
        val stale = System.currentTimeMillis() - lastCheckAt > CHECK_COOLDOWN_MS
        if (engine.status.value.state != LocalAiState.CHECKING && !stale) return false
        return refreshMutex.withLock {
            if (engine.status.value.ready) return@withLock true
            lastCheckAt = System.currentTimeMillis()
            engine.refresh().ready
        }
    }

    private fun prompt(message: String, bankName: String): String = buildString {
        appendLine("Message from $bankName:")
        appendLine(message)
        appendLine()
        appendLine("Reply with one JSON object and nothing else:")
        appendLine(
            """{"isPayment":true,"amount":123.45,"direction":"expense","merchant":"Swiggy",""" +
                """"accountLast4":"1234","reference":"412345678901"}"""
        )
        appendLine("direction is expense when money left the account, income when it arrived.")
        appendLine(
            "merchant is the company or person on the other side, as named in the message " +
                "(\"Acme payroll added 500\" gives \"Acme payroll\"). Never your own account or bank."
        )
        appendLine("Use null for anything the message does not contain.")
        append("""If this is an offer, request, OTP, balance-only or not a payment, reply {"isPayment":false}.""")
    }

    private fun objectOf(reply: String): JSONObject? {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(reply.substring(start, end + 1)) }.getOrNull()
    }

    /** A string field, with JSON null and the word "null" both treated as missing. */
    private fun text(json: JSONObject, key: String): String? =
        json.optString(key).trim().takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

    private fun amountOf(raw: Any?): BigDecimal? = when (raw) {
        is Number -> raw.toString().toBigDecimalOrNull()
        is String -> raw.replace(currency, "").replace(",", "").trim().toBigDecimalOrNull()
        else -> null
    }

    /** The amount as a whole number in the text: 500 matches "Rs.500" but not "5000" or "1500". */
    private fun amountAppears(message: String, amount: BigDecimal): Boolean {
        val compact = message.replace(Regex("""(?<=\d),(?=\d)"""), "")
        val forms = setOf(
            amount.stripTrailingZeros().toPlainString(),
            amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            amount.setScale(1, RoundingMode.HALF_UP).toPlainString(),
        )
        return forms.any { form ->
            Regex("""(?<!\d)(?<!\d\.)${Regex.escape(form)}(?![\d]|\.\d)""").containsMatchIn(compact) ||
                // "500.00" in the text when the model says 500.
                Regex("""(?<!\d)(?<!\d\.)${Regex.escape(form)}\.0{1,2}(?!\d)""").containsMatchIn(compact)
        }
    }

    private companion object {
        const val CHECK_COOLDOWN_MS = 60_000L

        /** "your account", "my a/c", "the wallet": the user's own account, not a payee. */
        val ownAccount = Regex("""(?:your|my|the)?\s*(?:account|a/c|acct|wallet|bank account|savings account)\.?""", RegexOption.IGNORE_CASE)

        val currency = Regex("""₹|\bINR\b|\bRs\.?""", RegexOption.IGNORE_CASE)

        const val SYSTEM =
            "You are Paisa's payment message reader. Read one bank or payment app message " +
                "and decide whether it records a real payment. Reply with JSON only. " +
                "Never invent an amount, payee or reference."
    }
}
