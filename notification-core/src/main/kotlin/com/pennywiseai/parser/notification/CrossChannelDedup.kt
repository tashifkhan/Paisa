package com.pennywiseai.parser.notification

import kotlin.math.abs

/** Where a saved or incoming charge came from. */
enum class Channel {
    SMS,
    BANK_APP,
    PAYMENT_APP,

    /** Manual entry, statement import, AI: never matched, the user put it there on purpose. */
    OTHER
}

/** The fields the dedup rules compare. Amounts are positive; [type] is "income" or "expense". */
data class Charge(
    val amount: Double,
    val type: String,
    val timeMillis: Long,
    val channel: Channel,
    val bankName: String? = null,
    val merchant: String? = null,
    val accountLast4: String? = null,
    val reference: String? = null
)

/**
 * Decides when one payment reached Paisa twice: as a bank SMS, a bank app
 * notification and a UPI app notification. Each channel has its own text, so the
 * content hashes never match and these rules compare what the charge was.
 *
 * Built on PennyWise's TransactionDeduplication: a shared 12-digit UPI reference
 * is the strongest proof, then bank + merchant inside a short window. Two charges
 * from the same channel are never merged on looks alone: two equal coffees a
 * minute apart are two coffees.
 */
object CrossChannelDedup {

    enum class Verdict {
        /** Different charges. */
        DISTINCT,

        /** The incoming charge is already saved. Drop it. */
        DUPLICATE,

        /**
         * The saved row came from an app and the incoming one from the bank: an SMS
         * that followed a payment app's notification, or a bank app alert that
         * followed a payment app's. The bank message knows the account and balance,
         * so move those onto the saved row instead of adding a second one.
         */
        ENRICH_EXISTING
    }

    /** Bank SMS and bank app notifications for one charge arrive within a minute or two. */
    const val BANK_WINDOW_MS = 5 * 60_000L

    /** A UPI app notification can lead or trail the bank SMS by several minutes. */
    const val PAYMENT_APP_WINDOW_MS = 15 * 60_000L

    /** A shared UPI reference proves the match across a longer carrier delay. */
    const val REFERENCE_WINDOW_MS = 24 * 60 * 60_000L

    /** The widest window any rule uses, for the caller's candidate query. */
    const val MAX_WINDOW_MS = REFERENCE_WINDOW_MS

    private val upiReference = Regex("""\d{12}""")

    fun verdict(incoming: Charge, existing: Charge): Verdict {
        if (incoming.channel == Channel.OTHER || existing.channel == Channel.OTHER) return Verdict.DISTINCT
        if (abs(incoming.amount - existing.amount) > 0.005) return Verdict.DISTINCT
        if (incoming.type != existing.type) return Verdict.DISTINCT
        if (!accountsMatch(incoming.accountLast4, existing.accountLast4)) return Verdict.DISTINCT

        val gap = abs(incoming.timeMillis - existing.timeMillis)
        val inRef = incoming.reference?.takeIf { upiReference.matches(it) }
        val oldRef = existing.reference?.takeIf { upiReference.matches(it) }
        if (inRef != null && oldRef != null) {
            // Two references settle it either way.
            if (inRef != oldRef || gap > REFERENCE_WINDOW_MS) return Verdict.DISTINCT
            return sameCharge(incoming, existing)
        }

        if (incoming.channel == existing.channel) return Verdict.DISTINCT

        val paymentApp = incoming.channel == Channel.PAYMENT_APP || existing.channel == Channel.PAYMENT_APP
        if (paymentApp) {
            // A UPI app names the payee, the bank SMS often only a VPA, so the
            // amount, direction and timing decide.
            return if (gap <= PAYMENT_APP_WINDOW_MS) sameCharge(incoming, existing) else Verdict.DISTINCT
        }

        // SMS against the same bank's app notification.
        if (gap > BANK_WINDOW_MS) return Verdict.DISTINCT
        if (!incoming.bankName.equals(existing.bankName, ignoreCase = true)) return Verdict.DISTINCT
        if (!merchantsMatch(incoming.merchant, existing.merchant)) return Verdict.DISTINCT
        return sameCharge(incoming, existing)
    }

    /** First match among [existing], with ENRICH_EXISTING preferred over DUPLICATE. */
    fun <T> firstMatch(incoming: Charge, existing: List<Pair<T, Charge>>): Pair<T, Verdict>? {
        val verdicts = existing.map { (row, charge) -> row to verdict(incoming, charge) }
        return verdicts.firstOrNull { it.second == Verdict.ENRICH_EXISTING }
            ?: verdicts.firstOrNull { it.second == Verdict.DUPLICATE }
    }

    private fun sameCharge(incoming: Charge, existing: Charge): Verdict = when {
        // A payment app names the payee but not the account; the bank message has the rest.
        existing.channel == Channel.PAYMENT_APP && incoming.channel != Channel.PAYMENT_APP ->
            Verdict.ENRICH_EXISTING

        // A bank app alert saved first, then the SMS for the same charge: the SMS wins.
        existing.channel == Channel.BANK_APP && incoming.channel == Channel.SMS ->
            Verdict.ENRICH_EXISTING

        else -> Verdict.DUPLICATE
    }

    private fun accountsMatch(a: String?, b: String?): Boolean =
        a.isNullOrBlank() || b.isNullOrBlank() || a == b

    private fun merchantsMatch(a: String?, b: String?): Boolean {
        val x = normalize(a)
        val y = normalize(b)
        return x.isEmpty() || y.isEmpty() || x == y || x.contains(y) || y.contains(x)
    }

    private fun normalize(name: String?): String =
        name.orEmpty().lowercase().filter { it.isLetterOrDigit() }.removePrefix("unknownmerchant")
}
