package codes.tashif.paisa.ui.home

import codes.tashif.paisa.platform.PaisaCalendar
import codes.tashif.paisa.platform.formatDate

private const val DAY = "yyyy-MM-dd"

private fun PaisaCalendar.day(): String = formatDate(timeInMillis, DAY)

/**
 * The inclusive `yyyy-MM-dd` window a filter's date selection covers, or
 * null when there's no date filter. Transaction dates are ISO strings, so
 * comparing their first ten characters against these bounds is enough.
 */
fun TxFilters.dateBounds(): Pair<String, String>? {
    datePreset?.let { return it.bounds() }
    val from = dateFrom ?: return null
    return from to (dateTo ?: from)
}

fun DatePreset.bounds(): Pair<String, String> {
    val today = PaisaCalendar.getInstance()
    val end = today.day()
    val start = today.clone()
    when (this) {
        DatePreset.TODAY -> Unit
        DatePreset.LAST_7_DAYS -> start.add(PaisaCalendar.DAY_OF_MONTH, -6)
        DatePreset.THIS_MONTH -> start.set(PaisaCalendar.DAY_OF_MONTH, 1)
        DatePreset.LAST_MONTH -> {
            start.set(PaisaCalendar.DAY_OF_MONTH, 1)
            start.add(PaisaCalendar.MONTH, -1)
            val last = start.clone()
            last.set(PaisaCalendar.DAY_OF_MONTH, last.getActualMaximum(PaisaCalendar.DAY_OF_MONTH))
            return start.day() to last.day()
        }
        DatePreset.LAST_3_MONTHS -> start.add(PaisaCalendar.MONTH, -3)
        DatePreset.THIS_YEAR -> {
            start.set(PaisaCalendar.MONTH, 0)
            start.set(PaisaCalendar.DAY_OF_MONTH, 1)
        }
    }
    return start.day() to end
}

/** "4 Nov – 11 Nov", or one date when the range is a single day. */
fun customRangeLabel(from: String, to: String?): String {
    fun short(d: String): String {
        val parts = d.split("-")
        if (parts.size != 3) return d
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val m = parts[1].toIntOrNull()?.let { months.getOrNull(it - 1) } ?: return d
        return "${parts[2].trimStart('0')} $m"
    }
    return if (to == null || to == from) short(from) else "${short(from)} – ${short(to)}"
}
