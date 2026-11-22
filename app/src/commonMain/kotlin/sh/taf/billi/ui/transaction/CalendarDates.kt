package sh.taf.billi.ui.transaction

import sh.taf.billi.platform.BilliCalendar

/**
 * Sets the calendar date and keeps the time of day. [month] is zero-based.
 *
 * BilliCalendar.set is applied one field at a time, so setting MONTH on the
 * 31st would roll into the next month before DAY_OF_MONTH is set. Going to
 * day 1 first gives the same result as java.util.Calendar, which applies the
 * fields together.
 */
internal fun BilliCalendar.setDate(year: Int, month: Int, day: Int) {
    set(BilliCalendar.DAY_OF_MONTH, 1)
    set(BilliCalendar.YEAR, year)
    set(BilliCalendar.MONTH, month)
    set(BilliCalendar.DAY_OF_MONTH, day)
}

/** `clear()` then `set(year, month, day, hour, minute)` on java.util.Calendar. */
internal fun BilliCalendar.setCleared(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) {
    setDate(year, month, day)
    set(BilliCalendar.HOUR_OF_DAY, hour)
    set(BilliCalendar.MINUTE, minute)
    set(BilliCalendar.SECOND, 0)
    set(BilliCalendar.MILLISECOND, 0)
}
