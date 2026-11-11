package codes.tashif.paisa.platform

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant

/**
 * The part of `java.util.Calendar` Paisa uses, for common code. Gregorian,
 * in the device's time zone or UTC, with Calendar's behaviour:
 *
 * - [set] is lenient. Day 0 of a month is the last day of the one before,
 *   and February 31 rolls into March.
 * - [add] of months or years keeps the day but pins it to the new month's
 *   last day, so January 31 plus a month is February 28 or 29.
 *
 * MONTH is 0-based and DAY_OF_WEEK runs from SUNDAY (1) to SATURDAY (7),
 * as in Calendar.
 */
class PaisaCalendar private constructor(
    private val zone: TimeZone,
    private var millis: Long
) {
    var timeInMillis: Long
        get() = millis
        set(value) {
            millis = value
        }

    /** The locale's first day of the week, SUNDAY or MONDAY and so on. */
    val firstDayOfWeek: Int get() = localeFirstDayOfWeek()

    private fun local(): LocalDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)

    private fun setLocal(dt: LocalDateTime) {
        millis = dt.toInstant(zone).toEpochMilliseconds()
    }

    fun get(field: Int): Int {
        val dt = local()
        return when (field) {
            YEAR -> dt.year
            MONTH -> dt.monthNumber - 1
            DAY_OF_MONTH -> dt.dayOfMonth
            HOUR_OF_DAY -> dt.hour
            MINUTE -> dt.minute
            SECOND -> dt.second
            MILLISECOND -> dt.nanosecond / 1_000_000
            DAY_OF_WEEK -> dt.dayOfWeek.isoDayNumber % 7 + 1
            DAY_OF_YEAR -> dt.dayOfYear
            WEEK_OF_YEAR -> weekOfYear(dt.date)
            else -> error("Unsupported calendar field $field")
        }
    }

    fun set(field: Int, value: Int) {
        val dt = local()
        val date = dt.date
        val time = dt.time
        when (field) {
            YEAR -> setLocal(LocalDateTime(lenientDate(value, date.monthNumber - 1, date.dayOfMonth), time))
            MONTH -> setLocal(LocalDateTime(lenientDate(date.year, value, date.dayOfMonth), time))
            DAY_OF_MONTH -> setLocal(LocalDateTime(lenientDate(date.year, date.monthNumber - 1, value), time))
            DAY_OF_YEAR -> setLocal(LocalDateTime(LocalDate(date.year, 1, 1).plus(DatePeriod(days = value - 1)), time))
            HOUR_OF_DAY, MINUTE, SECOND, MILLISECOND -> {
                var h = time.hour
                var m = time.minute
                var s = time.second
                var ms = time.nanosecond / 1_000_000
                when (field) {
                    HOUR_OF_DAY -> h = value
                    MINUTE -> m = value
                    SECOND -> s = value
                    else -> ms = value
                }
                // Lenient: carry overflow into the date, like Calendar does.
                val total = ((h.toLong() * 60 + m) * 60 + s) * 1000 + ms
                val dayMs = 86_400_000L
                val days = Math.floorDiv(total, dayMs)
                val rem = Math.floorMod(total, dayMs)
                val newDate = date.plus(DatePeriod(days = days.toInt()))
                val newTime = LocalTime.fromMillisecondOfDay(rem.toInt())
                setLocal(LocalDateTime(newDate, newTime))
            }
            else -> error("Unsupported calendar field $field")
        }
    }

    fun add(field: Int, amount: Int) {
        if (amount == 0) return
        val dt = local()
        when (field) {
            YEAR -> setLocal(LocalDateTime(dt.date.plus(DatePeriod(years = amount)), dt.time))
            MONTH -> setLocal(LocalDateTime(dt.date.plus(DatePeriod(months = amount)), dt.time))
            DAY_OF_MONTH, DAY_OF_YEAR, DAY_OF_WEEK ->
                setLocal(LocalDateTime(dt.date.plus(DatePeriod(days = amount)), dt.time))
            WEEK_OF_YEAR -> setLocal(LocalDateTime(dt.date.plus(DatePeriod(days = amount * 7)), dt.time))
            HOUR_OF_DAY -> millis += amount * 3_600_000L
            MINUTE -> millis += amount * 60_000L
            SECOND -> millis += amount * 1_000L
            MILLISECOND -> millis += amount
            else -> error("Unsupported calendar field $field")
        }
    }

    fun getActualMaximum(field: Int): Int {
        val date = local().date
        return when (field) {
            DAY_OF_MONTH -> {
                val first = LocalDate(date.year, date.monthNumber, 1)
                first.plus(DatePeriod(months = 1)).plus(DatePeriod(days = -1)).dayOfMonth
            }
            DAY_OF_YEAR -> if (isLeap(date.year)) 366 else 365
            MONTH -> 11
            else -> error("Unsupported calendar field $field")
        }
    }

    fun before(other: PaisaCalendar): Boolean = millis < other.millis
    fun after(other: PaisaCalendar): Boolean = millis > other.millis

    fun clone(): PaisaCalendar = PaisaCalendar(zone, millis)

    /** PaisaCalendar's WEEK_OF_YEAR with the locale's first day and a minimal first week of one day. */
    private fun weekOfYear(date: LocalDate): Int {
        val first = firstDayOfWeek
        val jan1 = LocalDate(date.year, 1, 1)
        val jan1Dow = jan1.dayOfWeek.isoDayNumber % 7 + 1
        val offset = (jan1Dow - first + 7) % 7
        return (date.dayOfYear - 1 + offset) / 7 + 1
    }

    companion object {
        const val YEAR = 1
        const val MONTH = 2
        const val WEEK_OF_YEAR = 3
        const val DAY_OF_MONTH = 5
        const val DAY_OF_YEAR = 6
        const val DAY_OF_WEEK = 7
        const val HOUR_OF_DAY = 11
        const val MINUTE = 12
        const val SECOND = 13
        const val MILLISECOND = 14

        const val SUNDAY = 1
        const val MONDAY = 2
        const val SATURDAY = 7

        /** Now, in the device's time zone. */
        fun getInstance(): PaisaCalendar = PaisaCalendar(TimeZone.currentSystemDefault(), nowMillis())

        /** Now, in UTC. Material date pickers speak UTC midnight. */
        fun getUtcInstance(): PaisaCalendar = PaisaCalendar(TimeZone.UTC, nowMillis())

        private fun isLeap(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

        /** PaisaCalendar's lenient year/month/day: months and days overflow into the next unit. */
        private fun lenientDate(year: Int, month0: Int, day: Int): LocalDate {
            val y = year + Math.floorDiv(month0, 12)
            val m = Math.floorMod(month0, 12) + 1
            return LocalDate(y, m, 1).plus(DatePeriod(days = day - 1))
        }
    }
}

private object Math {
    fun floorDiv(a: Int, b: Int): Int = kotlin.math.floor(a.toDouble() / b).toInt()
    fun floorMod(a: Int, b: Int): Int = a - floorDiv(a, b) * b
    fun floorDiv(a: Long, b: Long): Long = kotlin.math.floor(a.toDouble() / b).toLong()
    fun floorMod(a: Long, b: Long): Long = a - floorDiv(a, b) * b
}
