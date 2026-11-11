package codes.tashif.paisa.ui.analytics

import codes.tashif.paisa.platform.PaisaCalendar
import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.ui.transaction.setCleared

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
    private val now: PaisaCalendar = PaisaCalendar.getInstance().apply { timeInMillis = nowMillis }
    val start: PaisaCalendar = PaisaCalendar.getInstance()
    val end: PaisaCalendar = PaisaCalendar.getInstance()
    private val prevStart: PaisaCalendar

    init {
        when (kind) {
            AnalyticsPeriod.MONTH -> {
                start.apply { midnight(); set(PaisaCalendar.DAY_OF_MONTH, 1); add(PaisaCalendar.MONTH, -offset) }
                end.timeInMillis = start.timeInMillis
                end.add(PaisaCalendar.MONTH, 1)
            }
            AnalyticsPeriod.YEAR -> {
                start.apply { midnight(); set(PaisaCalendar.DAY_OF_YEAR, 1); add(PaisaCalendar.YEAR, -offset) }
                end.timeInMillis = start.timeInMillis
                end.add(PaisaCalendar.YEAR, 1)
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
                AnalyticsPeriod.MONTH -> add(PaisaCalendar.MONTH, -1)
                AnalyticsPeriod.YEAR -> add(PaisaCalendar.YEAR, -1)
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
    val cal: PaisaCalendar get() = start

    val isCurrent: Boolean = now.timeInMillis < end.timeInMillis

    private val lastIncluded: PaisaCalendar = (end.clone()).apply { add(PaisaCalendar.MINUTE, -1) }

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
        val last = PaisaCalendar.getInstance().apply {
            timeInMillis = minOf(now.timeInMillis, end.timeInMillis - 1)
        }
        (dayNumber(last) - dayNumber(start) + 1).toInt().coerceAtLeast(1)
    }

    /**
     * Hours for a range of two days or less, days up to about three months,
     * months up to five years, and years beyond that.
     */
    val bucketField: Int = when (kind) {
        AnalyticsPeriod.MONTH -> PaisaCalendar.DAY_OF_MONTH
        AnalyticsPeriod.YEAR -> PaisaCalendar.MONTH
        AnalyticsPeriod.CUSTOM -> {
            val span = end.timeInMillis - start.timeInMillis
            when {
                span <= 2 * DAY_MS -> PaisaCalendar.HOUR_OF_DAY
                span <= 92 * DAY_MS -> PaisaCalendar.DAY_OF_MONTH
                span <= 5 * 366 * DAY_MS -> PaisaCalendar.MONTH
                else -> PaisaCalendar.YEAR
            }
        }
    }

    val buckets: List<Bucket> = bucketsBetween(start, end)
    val prevBuckets: List<Bucket> = bucketsBetween(prevStart, start)

    /** Buckets that have started, so a running total stops at now. */
    val bucketsElapsed: Int = buckets.count { it.start <= nowIso }

    /** The last month the period covers, for 6-month trends. */
    val lastMonth: PaisaCalendar = if (isCurrent) now.clone() else lastIncluded.clone()

    private fun bucketsBetween(from: PaisaCalendar, to: PaisaCalendar): List<Bucket> {
        val out = mutableListOf<Bucket>()
        val cursor = from.clone()
        while (cursor.before(to) && out.size < 400) {
            val next = (cursor.clone()).apply {
                set(PaisaCalendar.MINUTE, 0)
                set(PaisaCalendar.SECOND, 0)
                set(PaisaCalendar.MILLISECOND, 0)
                when (bucketField) {
                    PaisaCalendar.HOUR_OF_DAY -> add(PaisaCalendar.HOUR_OF_DAY, 1)
                    PaisaCalendar.DAY_OF_MONTH -> { set(PaisaCalendar.HOUR_OF_DAY, 0); add(PaisaCalendar.DAY_OF_MONTH, 1) }
                    PaisaCalendar.MONTH -> { set(PaisaCalendar.HOUR_OF_DAY, 0); set(PaisaCalendar.DAY_OF_MONTH, 1); add(PaisaCalendar.MONTH, 1) }
                    else -> { set(PaisaCalendar.HOUR_OF_DAY, 0); set(PaisaCalendar.DAY_OF_YEAR, 1); add(PaisaCalendar.YEAR, 1) }
                }
                if (after(to)) timeInMillis = to.timeInMillis
            }
            val (label, tip) = when (bucketField) {
                PaisaCalendar.HOUR_OF_DAY -> fmt("HH:00", cursor) to fmt("d MMM, h a", cursor)
                PaisaCalendar.DAY_OF_MONTH -> fmt("d", cursor) to fmt("d MMM", cursor)
                PaisaCalendar.MONTH -> fmt("MMM", cursor) to fmt("MMMM yyyy", cursor)
                else -> fmt("yyyy", cursor) to fmt("yyyy", cursor)
            }
            out += Bucket(fmt(ISO, cursor), fmt(ISO, next), label, tip)
            cursor.timeInMillis = next.timeInMillis
        }
        return out
    }

    companion object {
        fun fmt(pattern: String, c: PaisaCalendar): String =
            formatDate(c.timeInMillis, pattern)

        /** Days since 1970 for the calendar date [c] shows, ignoring its time and zone offset. */
        private fun dayNumber(c: PaisaCalendar): Long {
            val utc = PaisaCalendar.getUtcInstance().apply {
                setCleared(c.get(PaisaCalendar.YEAR), c.get(PaisaCalendar.MONTH), c.get(PaisaCalendar.DAY_OF_MONTH))
            }
            return utc.timeInMillis / DAY_MS
        }

        private fun PaisaCalendar.midnight() {
            set(PaisaCalendar.HOUR_OF_DAY, 0)
            set(PaisaCalendar.MINUTE, 0)
            set(PaisaCalendar.SECOND, 0)
            set(PaisaCalendar.MILLISECOND, 0)
        }

        /** "1 – 9 Nov 2026" for whole days, with times when the range starts or ends mid-day. */
        fun rangeLabel(from: PaisaCalendar, to: PaisaCalendar): String {
            val wholeDays = from.get(PaisaCalendar.HOUR_OF_DAY) == 0 && from.get(PaisaCalendar.MINUTE) == 0 &&
                to.get(PaisaCalendar.HOUR_OF_DAY) == 23 && to.get(PaisaCalendar.MINUTE) == 59
            val sameYear = from.get(PaisaCalendar.YEAR) == to.get(PaisaCalendar.YEAR)
            val sameDay = sameYear && from.get(PaisaCalendar.DAY_OF_YEAR) == to.get(PaisaCalendar.DAY_OF_YEAR)
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
