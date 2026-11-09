package codes.tashif.paisa.ui.format

import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.platform.parseDate

/** "Today", "Yesterday" or a date like "Mon, 3 Nov 2026" for a yyyy-MM-dd day. */
fun dayLabel(isoDay: String): String {
    val date = parseDate(isoDay, "yyyy-MM-dd") ?: return isoDay
    val now = nowMillis()
    val today = formatDate(now, "yyyy-MM-dd")
    val yesterday = formatDate(now - 86_400_000L, "yyyy-MM-dd")
    return when (isoDay) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> formatDate(date, "EEE, d MMM yyyy")
    }
}
