package codes.tashif.paisa.ui.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRecorderTest {

    @Test
    fun silence_is_zero_and_full_scale_is_one() {
        assertEquals(0f, VoiceRecorder.levelOf(ShortArray(800), 800))
        assertEquals(1f, VoiceRecorder.levelOf(ShortArray(800) { Short.MAX_VALUE }, 800), 0.001f)
        // A quiet voice around -30 dBFS lands in the lower half, not at the floor.
        val quiet = VoiceRecorder.levelOf(ShortArray(800) { 1000 }, 800)
        assertTrue(quiet in 0.2f..0.6f)
    }

    @Test
    fun bars_average_down_and_pad_short_recordings() {
        assertEquals(listOf(0.5f, 1f), VoiceRecorder.barsOf(listOf(0f, 1f, 1f, 1f), 2))
        assertEquals(4, VoiceRecorder.barsOf(listOf(0.3f), 4).size)
        assertEquals(List(3) { 0f }, VoiceRecorder.barsOf(emptyList(), 3))
    }

    @Test
    fun speech_threshold_sits_between_room_noise_and_a_quiet_voice() {
        val roomNoise = VoiceRecorder.levelOf(ShortArray(800) { 184 }, 800) // about -45 dBFS
        val quietVoice = VoiceRecorder.levelOf(ShortArray(800) { 1036 }, 800) // about -30 dBFS
        assertTrue(roomNoise < VoiceRecorder.VOICE_LEVEL)
        assertTrue(quietVoice >= VoiceRecorder.VOICE_LEVEL)
    }
}
