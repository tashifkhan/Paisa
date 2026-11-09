package codes.tashif.paisa.ui.format

import java.util.Locale

/** "Today", "Yesterday" or a date like "Mon, 3 Nov 2026" for a yyyy-MM-dd day. */
fun dayLabel(isoDay: String): String {
    val parser = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = runCatching { parser.parse(isoDay) }.getOrNull() ?: return isoDay
    val today = parser.format(java.util.Date())
    val yesterday = parser.format(java.util.Date(System.currentTimeMillis() - 86_400_000L))
    return when (isoDay) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> java.text.SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(date)
    }
}
