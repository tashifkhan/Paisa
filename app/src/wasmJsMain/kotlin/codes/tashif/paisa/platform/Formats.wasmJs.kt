package codes.tashif.paisa.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant

private fun jsNow(): Double = js("Date.now()")

actual fun nowMillis(): Long = jsNow().toLong()

private val MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** Splits a SimpleDateFormat pattern into runs of one letter and quoted literals. */
private fun tokens(pattern: String): List<Pair<Boolean, String>> {
    val out = mutableListOf<Pair<Boolean, String>>()
    var i = 0
    while (i < pattern.length) {
        val c = pattern[i]
        when {
            c == '\'' -> {
                val end = pattern.indexOf('\'', i + 1).let { if (it < 0) pattern.length else it }
                out += false to (if (end == i + 1) "'" else pattern.substring(i + 1, end))
                i = end + 1
            }
            c.isLetter() -> {
                var j = i
                while (j < pattern.length && pattern[j] == c) j++
                out += true to pattern.substring(i, j)
                i = j
            }
            else -> {
                out += false to c.toString()
                i++
            }
        }
    }
    return out
}

actual fun formatDate(epochMillis: Long, pattern: String, utc: Boolean, machine: Boolean): String {
    val zone = if (utc) TimeZone.UTC else TimeZone.currentSystemDefault()
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone)
    return buildString {
        for ((isField, t) in tokens(pattern)) {
            if (!isField) {
                append(t)
                continue
            }
            val n = t.length
            when (t[0]) {
                'y' -> append(if (n == 2) (dt.year % 100).toString().padStart(2, '0') else dt.year.toString().padStart(n, '0'))
                'M' -> {
                    val m = dt.monthNumber
                    append(
                        when {
                            n >= 4 -> MONTHS[m - 1]
                            n == 3 -> MONTHS[m - 1].take(3)
                            else -> m.toString().padStart(n, '0')
                        }
                    )
                }
                'd' -> append(dt.dayOfMonth.toString().padStart(n, '0'))
                'E' -> {
                    val name = DAYS[dt.dayOfWeek.isoDayNumber - 1]
                    append(if (n >= 4) name else name.take(3))
                }
                'H' -> append(dt.hour.toString().padStart(n, '0'))
                'h' -> append((if (dt.hour % 12 == 0) 12 else dt.hour % 12).toString().padStart(n, '0'))
                'm' -> append(dt.minute.toString().padStart(n, '0'))
                's' -> append(dt.second.toString().padStart(n, '0'))
                'S' -> append((dt.nanosecond / 1_000_000).toString().padStart(3, '0').take(n))
                'a' -> append(if (dt.hour < 12) "AM" else "PM")
                else -> append(t)
            }
        }
    }
}

actual fun parseDate(text: String, pattern: String, utc: Boolean, machine: Boolean): Long? {
    // Paisa only parses numeric patterns: yyyy-MM-dd, yyyy-MM-dd'T'HH:mm:ss and the like.
    var year = 1970
    var month = 1
    var day = 1
    var hour = 0
    var minute = 0
    var second = 0
    var pos = 0
    for ((isField, t) in tokens(pattern)) {
        if (!isField) {
            if (!text.startsWith(t, pos)) return null
            pos += t.length
            continue
        }
        val width = t.length
        var end = pos
        while (end < text.length && text[end].isDigit() && (width == 1 || end - pos < width)) end++
        if (end == pos) return null
        val value = text.substring(pos, end).toIntOrNull() ?: return null
        when (t[0]) {
            'y' -> year = value
            'M' -> month = value
            'd' -> day = value
            'H' -> hour = value
            'm' -> minute = value
            's' -> second = value
            else -> return null
        }
        pos = end
    }
    return try {
        val zone = if (utc) TimeZone.UTC else TimeZone.currentSystemDefault()
        LocalDateTime(LocalDate(year, month, day), LocalTime(hour, minute, second))
            .toInstant(zone).toEpochMilliseconds()
    } catch (e: IllegalArgumentException) {
        null
    }
}

actual fun formatMediumDateTime(epochMillis: Long): String =
    formatDate(epochMillis, "d MMM yyyy, h:mm a")

private fun jsToFixed(value: Double, decimals: Int): String = js("value.toFixed(decimals)")

actual fun formatFixed(value: Double, decimals: Int, machine: Boolean): String = jsToFixed(value, decimals)

actual fun formatInteger(value: Long): String {
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (value < 0) "-$grouped" else grouped
}

actual fun localeFirstDayOfWeek(): Int = PaisaCalendar.SUNDAY

actual fun formatGrouped(value: Double, decimals: Int): String {
    val fixed = formatFixed(kotlin.math.abs(value), decimals)
    val whole = fixed.substringBefore('.')
    val frac = fixed.substringAfter('.', "")
    val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
    val sign = if (value < 0 && fixed.any { it in '1'..'9' }) "-" else ""
    return sign + grouped + if (frac.isNotEmpty()) ".$frac" else ""
}
