package codes.tashif.paisa.platform

/**
 * Locale-aware formatting. On Android these call the same JDK formatters the
 * app always used, so text is unchanged; the web formats in English.
 */

/** Wall clock in milliseconds since the epoch. */
expect fun nowMillis(): Long

/**
 * Formats [epochMillis] with a `SimpleDateFormat` [pattern], in the device's
 * time zone or UTC. [machine] uses US English, for strings that are stored
 * or parsed rather than shown.
 */
expect fun formatDate(epochMillis: Long, pattern: String, utc: Boolean = false, machine: Boolean = false): String

/** Parses [text] with a `SimpleDateFormat` [pattern]. Null when it doesn't match. */
expect fun parseDate(text: String, pattern: String, utc: Boolean = false, machine: Boolean = false): Long?

/** `DateFormat.getDateTimeInstance(MEDIUM, SHORT)`: "9 Nov 2026, 7:08 pm" and the like. */
expect fun formatMediumDateTime(epochMillis: Long): String

/** `"%.Nf".format(value)` in the device's locale. [machine] uses US English. */
expect fun formatFixed(value: Double, decimals: Int, machine: Boolean = false): String

/** `NumberFormat.getIntegerInstance().format(value)`: grouped digits. */
expect fun formatInteger(value: Long): String

/** The locale's first day of the week, PaisaCalendar.SUNDAY to SATURDAY. */
expect fun localeFirstDayOfWeek(): Int

/** `"%,.Nf".format(value)`: grouped digits with [decimals] places, in the device's locale. */
expect fun formatGrouped(value: Double, decimals: Int): String

/** `Math.toDegrees`. */
fun radiansToDegrees(radians: Double): Double = radians * 180.0 / kotlin.math.PI
