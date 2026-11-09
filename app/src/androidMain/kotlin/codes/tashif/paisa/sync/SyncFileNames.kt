package codes.tashif.paisa.sync

/**
 * `paisa-sync-<device>-<seq>-<d|f>.chg`
 *
 * Each device writes only its own files, numbered 1, 2, 3, ... `d` holds the
 * changes since the previous file; `f` holds everything the device knows, so
 * a device that joins late (or fell behind after old files were deleted) can
 * start from it instead of replaying the whole history.
 */
object SyncFileNames {
    const val PREFIX = "paisa-sync-"
    private const val EXTENSION = ".chg"
    private val PATTERN = Regex("""^paisa-sync-([0-9a-f]{8,32})-(\d{1,12})-([df])\.chg$""")

    data class Parsed(val deviceId: String, val seq: Long, val full: Boolean)

    fun name(deviceId: String, seq: Long, full: Boolean): String =
        PREFIX + deviceId + "-" + seq.toString().padStart(8, '0') + "-" + (if (full) "f" else "d") + EXTENSION

    fun parse(name: String): Parsed? {
        val m = PATTERN.matchEntire(name) ?: return null
        return Parsed(m.groupValues[1], m.groupValues[2].toLong(), m.groupValues[3] == "f")
    }

    fun isSyncFile(name: String): Boolean = parse(name) != null
}
