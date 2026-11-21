package com.pennywiseai.parser.notification

import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Reads a payment app's notification: "₹250 paid to Swiggy", "Paid ₹1,200 to Arjun",
 * "Arjun sent you ₹500". Stricter than the SMS parsers, because the same apps also
 * post offers ("Get ₹100 cashback!"), reminders and payment requests.
 *
 * A result needs an amount and a direction. Offers and balance alerts are
 * rejected by [PaymentText]. The offer filter and the
 * "X sent you" pattern come from Leftovers (MIT).
 */
object PaymentNotificationParser {

    private val amount = Regex(
        """([+-])?\s*(?:₹|\bRs\.?|\bINR)\s?([+-])?\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    /** A bare signed number, for apps that leave out ₹/Rs entirely. */
    private val signedAmount = Regex(
        // Grouped ("-1,250") or at most 7 digits, so "Ref - 412345678901" is not a charge.
        """(?<![A-Za-z\d])([+-])\s?((?:[0-9]{1,3}(?:,[0-9]{2,3})+|[0-9]{1,7})(?:\.[0-9]{1,2})?)(?![\d,])"""
    )

    /** "Arjun sent you ₹500", "Priya paid you Rs 300": money in, from whoever is named first. */
    private val sentYou = Regex("""^\s*(.{2,40}?)\s+(?:has\s+)?(?:sent|paid|transferred)\s+you\b""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    private val incomeWords = Regex(
        """\b(sent you|paid you|received|credited|credit of|refund(?:ed)?|deposit(?:ed)?|""" +
            """money added|added to (?:your|the) account|salary|income|cr\.?)\b""",
        RegexOption.IGNORE_CASE
    )
    private val expenseWords = Regex(
        """\b(paid|sent|debited|debit|spent|purchase|withdrawn|withdrawal|transferred|""" +
            """payment of|payment to|paying|dr\.?)\b""",
        RegexOption.IGNORE_CASE
    )
    private val incomePhrases = Regex(
        """\b(payment received|received payment|money received|amount received|""" +
            """added to (?:your|the) account|sent you|paid you)\b""",
        RegexOption.IGNORE_CASE
    )
    private val CLAUSE_BREAK = Regex("""(?:[.;](?=\s|$))|\n""")

    private const val NAME = """([A-Za-z0-9][A-Za-z0-9 .&'@_-]{0,40}?)"""
    private const val NAME_END =
        """(?=\s+(?:on|via|using|from|for|at|with|successfully|is|was|has|ref|upi|txn|through)\b|\s*[.,!:\n(]|\s*$)"""
    private val toName = Regex("""\bto\s+$NAME$NAME_END""", RegexOption.IGNORE_CASE)
    private val atName = Regex("""\bat\s+$NAME$NAME_END""", RegexOption.IGNORE_CASE)
    private val fromName = Regex("""\bfrom\s+$NAME$NAME_END""", RegexOption.IGNORE_CASE)
    private val towardsName = Regex("""\btowards\s+$NAME$NAME_END""", RegexOption.IGNORE_CASE)
    private val forName = Regex("""\bfor\s+$NAME$NAME_END""", RegexOption.IGNORE_CASE)
    private val byName = Regex("""\bby\s+$NAME$NAME_END""", RegexOption.IGNORE_CASE)

    /** A 12-digit UPI reference ("UPI Ref: 412345678901", "UTR 412345678901"). */
    private val upiRef = Regex(
        """(?:UPI\s*)?(?:Ref(?:erence)?|UTR|RRN|Txn(?:\s*ID)?|Transaction\s*ID)\.?\s*(?:No\.?|ID|#)?\s*[:\-]?\s*([0-9]{12})\b""",
        RegexOption.IGNORE_CASE
    )

    private val last4 = Regex("""(?:xx+|\*{2,}|•+|ending(?:\s+in)?|a/c(?:\s+no\.?)?|acct?\.?)\s?([0-9]{4})\b""", RegexOption.IGNORE_CASE)

    /** Words that name the account rather than the other party ("from HDFC Bank •• 1234"). */
    private val accountish = Regex(
        """\b(bank|a/c|account|wallet|upi lite|balance)\b|\d{4,}|^x+\d+$""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        app: NotificationApp,
        message: String,
        timestamp: Long
    ): ParsedTransaction? {
        val body = message.trim()
        if (body.isEmpty() || PaymentText.isPromotion(body)) return null

        // A balance-only alert has a number, but no payment happened.
        if (PaymentText.isBalanceOnly(body)) return null

        // Pick the amount from a clause that isn't just a balance line: bank apps
        // often put "Avl Bal Rs.5,000" beside the payment amount.
        val money = body.split(CLAUSE_BREAK)
            .asSequence()
            .filterNot { PaymentText.mentionsBalance(it) }
            .mapNotNull { clause -> amount.find(clause) }
            .firstOrNull()
            ?: amount.find(body)
        val signed = money ?: signedAmount.find(body) ?: return null
        val sign = when {
            money != null -> signed.groupValues[1].ifBlank { signed.groupValues[2] }
            else -> signed.groupValues[1]
        }
        val valueText = if (money != null) signed.groupValues[3] else signed.groupValues[2]
        val value = valueText.replace(",", "").toBigDecimalOrNull() ?: return null
        if (value <= BigDecimal.ZERO) return null

        val sender = sentYou.find(body)?.groupValues?.get(1)?.trim()
        val type = when {
            sign == "+" -> TransactionType.INCOME
            sign == "-" -> TransactionType.EXPENSE
            sender != null || incomePhrases.containsMatchIn(body) -> TransactionType.INCOME
            expenseWords.containsMatchIn(body) -> TransactionType.EXPENSE
            incomeWords.containsMatchIn(body) -> TransactionType.INCOME
            else -> return null
        }

        val merchant = if (type == TransactionType.INCOME) {
            sender ?: party(fromName, body) ?: party(byName, body)
        } else {
            party(toName, body) ?: party(atName, body) ?: party(towardsName, body) ?: party(forName, body)
        }

        return ParsedTransaction(
            amount = value,
            type = type,
            merchant = merchant,
            reference = upiRef.find(body)?.groupValues?.get(1),
            accountLast4 = last4.find(body)?.groupValues?.get(1),
            balance = null,
            smsBody = body,
            sender = app.packageName,
            timestamp = timestamp,
            bankName = app.name,
            currency = "INR"
        )
    }

    private fun party(pattern: Regex, body: String): String? =
        pattern.findAll(body)
            .map { it.groupValues[1].trim().trimEnd('.', '-', '_') }
            .map { it.removePrefix("a ").removePrefix("an ").removePrefix("the ") }
            .firstOrNull { it.length >= 2 && !accountish.containsMatchIn(it) && amount.find(it) == null }
            ?.take(40)
}
