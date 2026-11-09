package codes.tashif.paisa.sms

/**
 * Incremental scans deliberately overlap the recent inbox. Some devices expose
 * an SMS through the provider after its message timestamp has already fallen
 * behind our last-scan watermark. Transaction hashes make the overlap safe.
 *
 * Pennywise uses the same three-day safety window for incremental scans.
 */
internal object SmsScanWindow {
    private const val RECENT_LOOKBACK_MS = 3L * 24L * 60L * 60L * 1_000L

    fun startTime(lastScanAt: Long, forceFull: Boolean, now: Long): Long {
        if (forceFull || lastScanAt <= 0L) return 0L
        val recentWindowStart = (now - RECENT_LOOKBACK_MS).coerceAtLeast(0L)
        return minOf(lastScanAt, recentWindowStart).coerceAtLeast(0L)
    }
}
