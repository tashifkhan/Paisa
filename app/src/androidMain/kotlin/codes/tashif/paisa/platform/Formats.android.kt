package codes.tashif.paisa.platform

import java.text.DateFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual fun nowMillis(): Long = System.currentTimeMillis()

private fun dateFormat(pattern: String, utc: Boolean, machine: Boolean): SimpleDateFormat =
    SimpleDateFormat(pattern, if (machine) Locale.US else Locale.getDefault()).apply {
        if (utc) timeZone = TimeZone.getTimeZone("UTC")
    }

actual fun formatDate(epochMillis: Long, pattern: String, utc: Boolean, machine: Boolean): String =
    dateFormat(pattern, utc, machine).format(Date(epochMillis))

actual fun parseDate(text: String, pattern: String, utc: Boolean, machine: Boolean): Long? =
    try {
        dateFormat(pattern, utc, machine).parse(text)?.time
    } catch (e: ParseException) {
        null
    }

actual fun formatMediumDateTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

actual fun formatFixed(value: Double, decimals: Int, machine: Boolean): String =
    String.format(if (machine) Locale.US else Locale.getDefault(), "%.${decimals}f", value)

actual fun formatInteger(value: Long): String = NumberFormat.getIntegerInstance().format(value)

actual fun localeFirstDayOfWeek(): Int = Calendar.getInstance().firstDayOfWeek
