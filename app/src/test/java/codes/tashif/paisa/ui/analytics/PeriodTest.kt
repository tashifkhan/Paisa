package codes.tashif.paisa.ui.analytics

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** The time maths behind Analytics: day counts, buckets and "so far". */
class PeriodTest {

    private lateinit var savedZone: TimeZone

    @Before
    fun saveZone() {
        savedZone = TimeZone.getDefault()
    }

    @After
    fun restoreZone() = TimeZone.setDefault(savedZone)

    private fun millis(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, min) }.timeInMillis

    @Test
    fun aDayWithAClockChangeCountsAsOneDay() {
        // 1 Nov 2026 is 25 hours long in New York.
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        val range = millis(2026, 11, 1) to millis(2026, 11, 1, 23, 59)
        val period = Period(AnalyticsPeriod.CUSTOM, 0, range, nowMillis = millis(2026, 12, 1))
        assertEquals(1, period.daysElapsed)
    }

    @Test
    fun aFinishedMonthCountsEveryDay() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
        val period = Period(AnalyticsPeriod.MONTH, 0, 0L to 0L, nowMillis = millis(2026, 11, 9, 10))
        assertEquals(9, period.daysElapsed)
        assertEquals(30, period.buckets.size)
        assertEquals(9, period.bucketsElapsed)
    }

    @Test
    fun laterTodayHasNotHappenedYet() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
        val period = Period(AnalyticsPeriod.MONTH, 0, 0L to 0L, nowMillis = millis(2026, 11, 9, 10))
        assertTrue(period.hasHappened("2026-11-09T09:59:00"))
        assertFalse(period.hasHappened("2026-11-09T22:00:00"))
        assertTrue(period.contains("2026-11-09T22:00:00"))
    }

    @Test
    fun longRangesUseYearsAndReachTheEnd() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
        val range = millis(1980, 1, 1) to millis(2025, 12, 31, 23, 59)
        val period = Period(AnalyticsPeriod.CUSTOM, 0, range, nowMillis = millis(2026, 11, 9))
        assertEquals(Calendar.YEAR, period.bucketField)
        assertEquals(46, period.buckets.size)
        assertTrue(period.buckets.last().contains("2025-12-31T12:00:00"))
    }
}
