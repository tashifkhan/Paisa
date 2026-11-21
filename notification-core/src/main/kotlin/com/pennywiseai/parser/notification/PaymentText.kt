package com.pennywiseai.parser.notification

/**
 * Cheap checks on a message's text, shared by the payment-app reader, the
 * on-device AI fallback and the review queue, so all three agree on what an
 * offer or a balance alert looks like.
 */
object PaymentText {

    /** Offers, rewards, reminders, requests, failures and OTPs: amounts in these moved no money. */
    private val notAPayment = Regex(
        """\b(offers?|win|won|rewards?|scratch|cashback|coupons?|vouchers?|discount|flat|earn|chance|hurry|limited|""" +
            """recharge now|pay now|pay before|bills? (?:is )?due|due (?:on|today|tomorrow|date)|reminder|""" +
            """requested|requesting|request|collect|invite|refer|upto|up to|deals?|sale|claim|expir\w*|kyc|""" +
            """pending|processing|failed|declined|unsuccessful|reversed|get ₹|get rs|""" +
            """otp|one[- ]time password|verification code|passcode)\b|\d+\s?% off""",
        RegexOption.IGNORE_CASE
    )

    private val balanceWord = Regex("""\b(?:avl|available)?\s*bal(?:ance)?\b""", RegexOption.IGNORE_CASE)

    private val activityWord = Regex(
        """\b(paid|sent|debited|credited|received|spent|purchase|transferred|deposit(?:ed)?|withdraw\w*|refund\w*|added|payment)\b""",
        RegexOption.IGNORE_CASE
    )

    private val rupees = Regex("""(?:₹|\bRs\.?|\bINR)\s?[0-9]""", RegexOption.IGNORE_CASE)

    private val number = Regex("""\b\d{2,}(?:[.,]\d{1,2})?\b""")

    /** An offer, request, reminder, failure or OTP. */
    fun isPromotion(text: String): Boolean = notAPayment.containsMatchIn(text)

    /** Names a balance but no money moving. */
    fun isBalanceOnly(text: String): Boolean =
        balanceWord.containsMatchIn(text) && !activityWord.containsMatchIn(text)

    /** True when [text] is a balance line ("Avl Bal Rs 5,000"), used to skip that clause's amount. */
    fun mentionsBalance(text: String): Boolean = balanceWord.containsMatchIn(text)

    /**
     * Worth keeping for the user to review: not an offer or a balance alert,
     * and it names an amount and money moving.
     */
    fun looksLikePayment(text: String): Boolean =
        !isPromotion(text) && !isBalanceOnly(text) &&
            (rupees.containsMatchIn(text) || number.containsMatchIn(text)) &&
            activityWord.containsMatchIn(text)

    /** Worth asking an on-device model about: short, not an offer or balance alert, and has a number. */
    fun worthAskingModel(text: String): Boolean =
        text.length in 6..800 && !isPromotion(text) && !isBalanceOnly(text) &&
            (rupees.containsMatchIn(text) || number.containsMatchIn(text))
}
