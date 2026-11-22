package sh.taf.billi.ui.analytics

import sh.taf.billi.platform.BilliCalendar
import sh.taf.billi.platform.nowMillis
import sh.taf.billi.platform.formatDate
import sh.taf.billi.ui.transaction.setCleared

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
    nowMillis: Long = nowMillis()
) {
    private val now: BilliCalendar = BilliCalendar.getInstance().apply { timeInMillis = nowMillis }
    val start: BilliCalendar = BilliCalendar.getInstance()
    val end: BilliCalendar = BilliCalendar.getInstance()
    private val prevStart: BilliCalendar

    init {
        when (kind) {
            AnalyticsPeriod.MONTH -> {
                start.apply { midnight(); set(BilliCalendar.DAY_OF_MONTH, 1); add(BilliCalendar.MONTH, -offset) }
                end.timeInMillis = start.timeInMillis
                end.add(BilliCalendar.MONTH, 1)
            }
            AnalyticsPeriod.YEAR -> {
                start.apply { midnight(); set(BilliCalendar.DAY_OF_YEAR, 1); add(BilliCalendar.YEAR, -offset) }
                end.timeInMillis = start.timeInMillis
                end.add(BilliCalendar.YEAR, 1)
            }
            AnalyticsPeriod.CUSTOM -> {
                // The picked end minute is included, so the range ends a minute later.
                val span = custom.second + 60_000L - custom.first
                start.timeInMillis = custom.first - offset * span
                end.timeInMillis = start.timeInMillis + span
            }
        }
        prevStart = (start.clone()).apply {
            when (kind) {
                AnalyticsPeriod.MONTH -> add(BilliCalendar.MONTH, -1)
                AnalyticsPeriod.YEAR -> add(BilliCalendar.YEAR, -1)
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
    val cal: BilliCalendar get() = start

    val isCurrent: Boolean = now.timeInMillis < end.timeInMillis

    private val lastIncluded: BilliCalendar = (end.clone()).apply { add(BilliCalendar.MINUTE, -1) }

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
        val last = BilliCalendar.getInstance().apply {
            timeInMillis = minOf(now.timeInMillis, end.timeInMillis - 1)
        }
        (dayNumber(last) - dayNumber(start) + 1).toInt().coerceAtLeast(1)
    }

    /**
     * Hours for a range of two days or less, days up to about three months,
     * months up to five years, and years beyond that.
     */
    val bucketField: Int = when (kind) {
        AnalyticsPeriod.MONTH -> BilliCalendar.DAY_OF_MONTH
        AnalyticsPeriod.YEAR -> BilliCalendar.MONTH
        AnalyticsPeriod.CUSTOM -> {
            val span = end.timeInMillis - start.timeInMillis
            when {
                span <= 2 * DAY_MS -> BilliCalendar.HOUR_OF_DAY
                span <= 92 * DAY_MS -> BilliCalendar.DAY_OF_MONTH
                span <= 5 * 366 * DAY_MS -> BilliCalendar.MONTH
                else -> BilliCalendar.YEAR
            }
        }
    }

    val buckets: List<Bucket> = bucketsBetween(start, end)
    val prevBuckets: List<Bucket> = bucketsBetween(prevStart, start)

    /** Buckets that have started, so a running total stops at now. */
    val bucketsElapsed: Int = buckets.count { it.start <= nowIso }

    /** The last month the period covers, for 6-month trends. */
    val lastMonth: BilliCalendar = if (isCurrent) now.clone() else lastIncluded.clone()

    private fun bucketsBetween(from: BilliCalendar, to: BilliCalendar): List<Bucket> {
        val out = mutableListOf<Bucket>()
        val cursor = from.clone()
        while (cursor.before(to) && out.size < 400) {
            val next = (cursor.clone()).apply {
                set(BilliCalendar.MINUTE, 0)
                set(BilliCalendar.SECOND, 0)
                set(BilliCalendar.MILLISECOND, 0)
                when (bucketField) {
                    BilliCalendar.HOUR_OF_DAY -> add(BilliCalendar.HOUR_OF_DAY, 1)
                    BilliCalendar.DAY_OF_MONTH -> { set(BilliCalendar.HOUR_OF_DAY, 0); add(BilliCalendar.DAY_OF_MONTH, 1) }
                    BilliCalendar.MONTH -> { set(BilliCalendar.HOUR_OF_DAY, 0); set(BilliCalendar.DAY_OF_MONTH, 1); add(BilliCalendar.MONTH, 1) }
                    else -> { set(BilliCalendar.HOUR_OF_DAY, 0); set(BilliCalendar.DAY_OF_YEAR, 1); add(BilliCalendar.YEAR, 1) }
                }
                if (after(to)) timeInMillis = to.timeInMillis
            }
            val (label, tip) = when (bucketField) {
                BilliCalendar.HOUR_OF_DAY -> fmt("HH:00", cursor) to fmt("d MMM, h a", cursor)
                BilliCalendar.DAY_OF_MONTH -> fmt("d", cursor) to fmt("d MMM", cursor)
                BilliCalendar.MONTH -> fmt("MMM", cursor) to fmt("MMMM yyyy", cursor)
                else -> fmt("yyyy", cursor) to fmt("yyyy", cursor)
            }
            out += Bucket(fmt(ISO, cursor), fmt(ISO, next), label, tip)
            cursor.timeInMillis = next.timeInMillis
        }
        return out
    }

    companion object {
        fun fmt(pattern: String, c: BilliCalendar): String =
            formatDate(c.timeInMillis, pattern)

        /** Days since 1970 for the calendar date [c] shows, ignoring its time and zone offset. */
        private fun dayNumber(c: BilliCalendar): Long {
            val utc = BilliCalendar.getUtcInstance().apply {
                setCleared(c.get(BilliCalendar.YEAR), c.get(BilliCalendar.MONTH), c.get(BilliCalendar.DAY_OF_MONTH))
            }
            return utc.timeInMillis / DAY_MS
        }

        private fun BilliCalendar.midnight() {
            set(BilliCalendar.HOUR_OF_DAY, 0)
            set(BilliCalendar.MINUTE, 0)
            set(BilliCalendar.SECOND, 0)
            set(BilliCalendar.MILLISECOND, 0)
        }

        /** "1 – 9 Nov 2026" for whole days, with times when the range starts or ends mid-day. */
        fun rangeLabel(from: BilliCalendar, to: BilliCalendar): String {
            val wholeDays = from.get(BilliCalendar.HOUR_OF_DAY) == 0 && from.get(BilliCalendar.MINUTE) == 0 &&
                to.get(BilliCalendar.HOUR_OF_DAY) == 23 && to.get(BilliCalendar.MINUTE) == 59
            val sameYear = from.get(BilliCalendar.YEAR) == to.get(BilliCalendar.YEAR)
            val sameDay = sameYear && from.get(BilliCalendar.DAY_OF_YEAR) == to.get(BilliCalendar.DAY_OF_YEAR)
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
