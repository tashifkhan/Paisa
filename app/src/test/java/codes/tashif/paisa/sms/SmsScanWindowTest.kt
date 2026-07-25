package codes.tashif.paisa.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsScanWindowTest {
    private val day = 24L * 60L * 60L * 1_000L
    private val now = 10L * day

    @Test
    fun `full scan starts at beginning of inbox`() {
        assertEquals(0L, SmsScanWindow.startTime(lastScanAt = 8L * day, forceFull = true, now))
    }

    @Test
    fun `first scan starts at beginning of inbox`() {
        assertEquals(0L, SmsScanWindow.startTime(lastScanAt = 0L, forceFull = false, now))
    }

    @Test
    fun `incremental scan always rechecks the latest three days`() {
        assertEquals(7L * day, SmsScanWindow.startTime(lastScanAt = 9L * day, forceFull = false, now))
    }

    @Test
    fun `incremental scan retains an older unscanned watermark`() {
        assertEquals(4L * day, SmsScanWindow.startTime(lastScanAt = 4L * day, forceFull = false, now))
    }

    @Test
    fun `future watermark falls back to recent safety window`() {
        assertEquals(7L * day, SmsScanWindow.startTime(lastScanAt = 20L * day, forceFull = false, now))
    }
}
