package com.pennywiseai.parser.notification

/**
 * Turns a notification's text fields into one message, and decides which posts to
 * skip. Callers pass the extras as strings, so this stays free of Android types.
 */
object NotificationText {

    /** Notification.FLAG_ONGOING_EVENT */
    const val FLAG_ONGOING_EVENT = 0x00000002

    /** Notification.FLAG_GROUP_SUMMARY */
    const val FLAG_GROUP_SUMMARY = 0x00000200

    /**
     * A group summary repeats its children, and an ongoing notification (a payment
     * in progress, a download) keeps updating its text, so each update would read
     * as a new payment.
     */
    fun skips(flags: Int): Boolean =
        (flags and FLAG_GROUP_SUMMARY) != 0 || (flags and FLAG_ONGOING_EVENT) != 0

    /**
     * Big text, inbox lines, text and summary, joined, with the title in front.
     * Apps that set big text usually repeat the short text inside it, so a part
     * already contained in an earlier one is dropped.
     */
    fun message(
        title: String?,
        bigText: String?,
        textLines: List<String> = emptyList(),
        text: String?,
        summary: String? = null
    ): String {
        val parts = mutableListOf<String>()
        (listOf(bigText) + textLines + listOf(text, summary)).forEach { raw ->
            val part = raw?.trim().orEmpty()
            if (part.isNotEmpty() && parts.none { it.contains(part) }) parts += part
        }
        return withTitle(title, parts.joinToString("\n"))
    }

    /**
     * Keeps the title with the body. Some apps put the heading ("Card Transaction",
     * "₹250 paid to Swiggy") in the title and only details in the text, and the
     * heading is what tells a payment from a refund.
     */
    fun withTitle(title: String?, body: String): String {
        val t = title?.trim().orEmpty()
        return when {
            body.isBlank() -> t
            t.isEmpty() || body.contains(t) -> body
            else -> "$t\n$body"
        }
    }
}
