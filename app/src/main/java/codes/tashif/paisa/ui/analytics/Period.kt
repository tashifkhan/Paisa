package codes.tashif.paisa.ui.analytics

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One point on a time axis: an hour, a day or a month. End is exclusive. */
internal class Bucket(val start: String, val end: String, val label: String, val tip: String) {
    fun contains(date: String) = normalizeDate(date).let { it >= start && it < end }
}

private const val ISO = "yyyy-MM-dd'T'HH:mm:ss"
private const val DAY_MS = 86_400_000L

/** Dates without a time count from midnight, so string comparison stays correct. */
private fun normalizeDate(date: String): String = if (date.length == 10) date + "T00:00:00" else date

/**
 * The time range on screen and the one just before it, for comparisons.
 * A month, a year, or a custom range of dates and times.
 */
internal class Period(
    val kind: AnalyticsPeriod,
    offset: Int,
    custom: Pair<Long, Long>,
    nowMillis: Long = System.currentTimeMillis()
) {
    private val now: Calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val start: Calendar = Calendar.getInstance()
    val end: Calendar = Calendar.getInstance()
    private val prevStart: Calendar

    init {
        when (kind) {
            AnalyticsPeriod.MONTH -> {
                start.apply { midnight(); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, -offset) }
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.MONTH, 1)
            }
            AnalyticsPeriod.YEAR -> {
                start.apply { midnight(); set(Calendar.DAY_OF_YEAR, 1); add(Calendar.YEAR, -offset) }
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.YEAR, 1)
            }
            AnalyticsPeriod.CUSTOM -> {
                // The picked end minute is included, so the range ends a minute later.
                val span = custom.second + 60_000L - custom.first
                start.timeInMillis = custom.first - offset * span
                end.timeInMillis = start.timeInMillis + span
            }
        }
        prevStart = (start.clone() as Calendar).apply {
            when (kind) {
                AnalyticsPeriod.MONTH -> add(Calendar.MONTH, -1)
                AnalyticsPeriod.YEAR -> add(Calendar.YEAR, -1)
                AnalyticsPeriod.CUSTOM -> timeInMillis -= end.timeInMillis - start.timeInMillis
            }
        }
    }

    private val startIso = fmt(ISO, start)
    private val endIso = fmt(ISO, end)
    private val prevStartIso = fmt(ISO, prevStart)

    fun contains(date: String) = normalizeDate(date).let { it >= startIso && it < endIso }

    private val nowIso = fmt(ISO, now)

    /** True for anything dated now or earlier, so "so far" totals skip future-dated entries. */
    fun hasHappened(date: String) = normalizeDate(date) <= nowIso
    fun inPrevious(date: String) = normalizeDate(date).let { it >= prevStartIso && it < startIso }

    /** The first day of the period, for the heatmap and the months of a year. */
    val cal: Calendar get() = start

    val isCurrent: Boolean = now.timeInMillis < end.timeInMillis

    private val lastIncluded: Calendar = (end.clone() as Calendar).apply { add(Calendar.MINUTE, -1) }

    val label: String = when (kind) {
        AnalyticsPeriod.MONTH -> fmt("MMMM yyyy", start)
        AnalyticsPeriod.YEAR -> fmt("yyyy", start)
        AnalyticsPeriod.CUSTOM -> rangeLabel(start, lastIncluded)
    }
    val short: String = when (kind) {
        AnalyticsPeriod.MONTH -> fmt("MMM", start)
        AnalyticsPeriod.YEAR -> fmt("yyyy", start)
        AnalyticsPeriod.CUSTOM -> "This range"
    }
    val prevLabel: String = when (kind) {
        AnalyticsPeriod.MONTH -> fmt("MMM", prevStart)
        AnalyticsPeriod.YEAR -> fmt("yyyy", prevStart)
        AnalyticsPeriod.CUSTOM -> "before"
    }

    /** Days the period has run so far: all of it if it's over, up to now if not. */
    val daysElapsed: Int = run {
        // Count calendar days, not 24-hour blocks, so a day with a clock change counts once.
        val last = Calendar.getInstance().apply {
            timeInMillis = minOf(now.timeInMillis, end.timeInMillis - 1)
        }
        (dayNumber(last) - dayNumber(start) + 1).toInt().coerceAtLeast(1)
    }

    /**
     * Hours for a range of two days or less, days up to about three months,
     * months up to five years, and years beyond that.
     */
    val bucketField: Int = when (kind) {
        AnalyticsPeriod.MONTH -> Calendar.DAY_OF_MONTH
        AnalyticsPeriod.YEAR -> Calendar.MONTH
        AnalyticsPeriod.CUSTOM -> {
            val span = end.timeInMillis - start.timeInMillis
            when {
                span <= 2 * DAY_MS -> Calendar.HOUR_OF_DAY
                span <= 92 * DAY_MS -> Calendar.DAY_OF_MONTH
                span <= 5 * 366 * DAY_MS -> Calendar.MONTH
                else -> Calendar.YEAR
            }
        }
    }

    val buckets: List<Bucket> = bucketsBetween(start, end)
    val prevBuckets: List<Bucket> = bucketsBetween(prevStart, start)

    /** Buckets that have started, so a running total stops at now. */
    val bucketsElapsed: Int = buckets.count { it.start <= nowIso }

    /** The last month the period covers, for 6-month trends. */
    val lastMonth: Calendar = (if (isCurrent) now.clone() else lastIncluded.clone()) as Calendar

    private fun bucketsBetween(from: Calendar, to: Calendar): List<Bucket> {
        val out = mutableListOf<Bucket>()
        val cursor = from.clone() as Calendar
        while (cursor.before(to) && out.size < 400) {
            val next = (cursor.clone() as Calendar).apply {
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                when (bucketField) {
                    Calendar.HOUR_OF_DAY -> add(Calendar.HOUR_OF_DAY, 1)
                    Calendar.DAY_OF_MONTH -> { set(Calendar.HOUR_OF_DAY, 0); add(Calendar.DAY_OF_MONTH, 1) }
                    Calendar.MONTH -> { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, 1) }
                    else -> { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.DAY_OF_YEAR, 1); add(Calendar.YEAR, 1) }
                }
                if (after(to)) timeInMillis = to.timeInMillis
            }
            val (label, tip) = when (bucketField) {
                Calendar.HOUR_OF_DAY -> fmt("HH:00", cursor) to fmt("d MMM, h a", cursor)
                Calendar.DAY_OF_MONTH -> fmt("d", cursor) to fmt("d MMM", cursor)
                Calendar.MONTH -> fmt("MMM", cursor) to fmt("MMMM yyyy", cursor)
                else -> fmt("yyyy", cursor) to fmt("yyyy", cursor)
            }
            out += Bucket(fmt(ISO, cursor), fmt(ISO, next), label, tip)
            cursor.timeInMillis = next.timeInMillis
        }
        return out
    }

    companion object {
        fun fmt(pattern: String, c: Calendar): String =
            SimpleDateFormat(pattern, Locale.getDefault()).format(c.time)

        /** Days since 1970 for the calendar date [c] shows, ignoring its time and zone offset. */
        private fun dayNumber(c: Calendar): Long {
            val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
            }
            return utc.timeInMillis / DAY_MS
        }

        private fun Calendar.midnight() {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        /** "1 – 9 Nov 2026" for whole days, with times when the range starts or ends mid-day. */
        fun rangeLabel(from: Calendar, to: Calendar): String {
            val wholeDays = from.get(Calendar.HOUR_OF_DAY) == 0 && from.get(Calendar.MINUTE) == 0 &&
                to.get(Calendar.HOUR_OF_DAY) == 23 && to.get(Calendar.MINUTE) == 59
            val sameYear = from.get(Calendar.YEAR) == to.get(Calendar.YEAR)
            val sameDay = sameYear && from.get(Calendar.DAY_OF_YEAR) == to.get(Calendar.DAY_OF_YEAR)
            return if (wholeDays && sameDay) {
                fmt("EEE, d MMM yyyy", from)
            } else if (wholeDays) {
                val left = if (sameYear) fmt("d MMM", from) else fmt("d MMM yyyy", from)
                "$left – ${fmt("d MMM yyyy", to)}"
            } else {
                "${fmt("d MMM, h:mm a", from)} – ${fmt("d MMM, h:mm a", to)}"
            }
        }
    }
}
