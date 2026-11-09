package codes.tashif.paisa.ui.transaction

import codes.tashif.paisa.platform.PaisaCalendar

/**
 * Sets the calendar date and keeps the time of day. [month] is zero-based.
 *
 * PaisaCalendar.set is applied one field at a time, so setting MONTH on the
 * 31st would roll into the next month before DAY_OF_MONTH is set. Going to
 * day 1 first gives the same result as java.util.Calendar, which applies the
 * fields together.
 */
internal fun PaisaCalendar.setDate(year: Int, month: Int, day: Int) {
    set(PaisaCalendar.DAY_OF_MONTH, 1)
    set(PaisaCalendar.YEAR, year)
    set(PaisaCalendar.MONTH, month)
    set(PaisaCalendar.DAY_OF_MONTH, day)
}

/** `clear()` then `set(year, month, day, hour, minute)` on java.util.Calendar. */
internal fun PaisaCalendar.setCleared(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) {
    setDate(year, month, day)
    set(PaisaCalendar.HOUR_OF_DAY, hour)
    set(PaisaCalendar.MINUTE, minute)
    set(PaisaCalendar.SECOND, 0)
    set(PaisaCalendar.MILLISECOND, 0)
}
