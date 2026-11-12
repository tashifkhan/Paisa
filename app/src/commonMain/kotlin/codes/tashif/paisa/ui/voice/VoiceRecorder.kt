package codes.tashif.paisa.ui.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

/** Whether this platform can record from the microphone. */
internal expect val voiceCaptureSupported: Boolean

/**
 * Reads 16-bit mono PCM at [sampleRate] from the microphone and hands each
 * chunk to [onSamples] until the calling coroutine is cancelled. Blocking reads
 * happen off the main thread, but [onSamples] runs on the caller's dispatcher,
 * so the recorder's state is only touched from one thread. Throws when the
 * microphone can't be opened.
 */
internal expect suspend fun captureMic(sampleRate: Int, onSamples: (ShortArray, Int) -> Unit)

/** Microphone permission. [request] shows the system prompt and reports back through the callback. */
internal interface MicPermission {
    val granted: Boolean
    fun request()
}

@Composable
internal expect fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): MicPermission

/** A finished recording, ready to send. */
class VoiceClip(
    /** A complete WAV file: 16 kHz, mono, 16-bit. */
    val wav: ByteArray,
    val durationMs: Long,
    /** Loudness per bar, 0 to 1, for the attachment tile's waveform. */
    val bars: List<Float>,
)

/** How a recording ended. */
internal sealed interface VoiceResult {
    data class Clip(val clip: VoiceClip) : VoiceResult
    data object TooShort : VoiceResult

    /**
     * Nothing above room noise. Never sent: given silence, models make up a
     * whole note, amounts and all.
     */
    data object Silent : VoiceResult
}

/**
 * Records a voice note and keeps a short loudness history for the live
 * waveform. Compose state, so the UI redraws as levels arrive.
 */
internal class VoiceRecorder(private val scope: CoroutineScope) {

    var recording by mutableStateOf(false)
        private set
    var elapsedMs by mutableLongStateOf(0L)
        private set

    /** Recent loudness, oldest first, about [LEVELS_PER_SECOND] a second. */
    val levels = mutableStateListOf<Float>()

    private var job: Job? = null
    private val chunks = ArrayList<ShortArray>()
    private val allLevels = ArrayList<Float>()
    private var samples = 0
    private var smoothed = 0f
    /** Chunks loud enough to be speech rather than room noise. */
    private var voicedChunks = 0

    /** Starts recording. [onLimit] runs when it hits [MAX_MS]; [onError] when the mic won't open. */
    fun start(onLimit: () -> Unit, onError: (String) -> Unit) {
        if (recording) return
        reset()
        recording = true
        job = scope.launch {
            try {
                captureMic(SAMPLE_RATE) { buffer, count ->
                    if (count <= 0) return@captureMic
                    chunks += buffer.copyOf(count)
                    samples += count
                    elapsedMs = samples * 1_000L / SAMPLE_RATE
                    val level = levelOf(buffer, count)
                    if (level >= VOICE_LEVEL) voicedChunks++
                    // Rise fast, fall slower, so speech reads as words rather than flicker.
                    smoothed = if (level > smoothed) level else smoothed * 0.55f + level * 0.45f
                    allLevels += smoothed
                    levels += smoothed
                    while (levels.size > HISTORY) levels.removeAt(0)
                    if (elapsedMs >= MAX_MS) onLimit()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                recording = false
                onError(e.message ?: "Couldn't open the microphone")
            }
        }
    }

    /** Stops and returns the clip, unless it's too short or holds no speech. Null when not recording. */
    fun finish(): VoiceResult? {
        if (!recording) return null
        job?.cancel()
        job = null
        recording = false
        val result = when {
            elapsedMs < MIN_MS -> VoiceResult.TooShort
            voicedChunks < MIN_VOICED_CHUNKS -> VoiceResult.Silent
            else -> VoiceResult.Clip(
                VoiceClip(wav = wavOf(), durationMs = elapsedMs, bars = barsOf(allLevels, TILE_BARS))
            )
        }
        reset()
        return result
    }

    fun cancel() {
        job?.cancel()
        job = null
        recording = false
        reset()
    }

    private fun reset() {
        chunks.clear()
        allLevels.clear()
        levels.clear()
        samples = 0
        smoothed = 0f
        voicedChunks = 0
        elapsedMs = 0L
    }

    private fun wavOf(): ByteArray {
        val dataBytes = samples * 2
        val out = ByteArray(44 + dataBytes)
        fun text(at: Int, s: String) = s.forEachIndexed { i, c -> out[at + i] = c.code.toByte() }
        fun int32(at: Int, v: Int) {
            out[at] = v.toByte(); out[at + 1] = (v shr 8).toByte()
            out[at + 2] = (v shr 16).toByte(); out[at + 3] = (v shr 24).toByte()
        }
        fun int16(at: Int, v: Int) {
            out[at] = v.toByte(); out[at + 1] = (v shr 8).toByte()
        }
        text(0, "RIFF"); int32(4, 36 + dataBytes); text(8, "WAVE")
        text(12, "fmt "); int32(16, 16); int16(20, 1); int16(22, 1)
        int32(24, SAMPLE_RATE); int32(28, SAMPLE_RATE * 2); int16(32, 2); int16(34, 16)
        text(36, "data"); int32(40, dataBytes)
        var at = 44
        for (chunk in chunks) {
            for (sample in chunk) {
                val v = sample.toInt()
                out[at++] = v.toByte()
                out[at++] = (v shr 8).toByte()
            }
        }
        return out
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val LEVELS_PER_SECOND = 20
        /** Two minutes is about 3.8 MB of WAV, well under every provider's limit. */
        const val MAX_MS = 120_000L
        private const val MIN_MS = 600L

        /** About -40 dBFS. Speech into a phone sits well above it, a quiet room well below. */
        internal const val VOICE_LEVEL = 0.2f

        /** Half a second of speech, at [LEVELS_PER_SECOND] chunks a second. */
        private const val MIN_VOICED_CHUNKS = 10
        private const val HISTORY = 64
        private const val TILE_BARS = 24

        /** Loudness of one chunk on a 0 to 1 scale, from -50 dBFS (silence) to 0. */
        internal fun levelOf(buffer: ShortArray, count: Int): Float {
            var sum = 0.0
            for (i in 0 until count) {
                val v = buffer[i] / 32768.0
                sum += v * v
            }
            val rms = sqrt(sum / count)
            if (rms <= 0.0) return 0f
            val db = 20 * log10(rms)
            return ((db + 50) / 50).toFloat().coerceIn(0f, 1f)
        }

        /** Averages [levels] down to [count] bars. */
        internal fun barsOf(levels: List<Float>, count: Int): List<Float> {
            if (levels.isEmpty()) return List(count) { 0f }
            return List(count) { i ->
                val from = i * levels.size / count
                val to = ((i + 1) * levels.size / count).coerceAtLeast(from + 1).coerceAtMost(levels.size)
                levels.subList(from.coerceAtMost(levels.size - 1), to).let { part -> part.sum() / part.size }
            }
        }
    }
}

@Composable
internal fun rememberVoiceRecorder(): VoiceRecorder {
    val scope = rememberCoroutineScope()
    val recorder = remember { VoiceRecorder(scope) }
    // Closing the sheet mid-recording throws the recording away and frees the mic.
    DisposableEffect(recorder) { onDispose { recorder.cancel() } }
    return recorder
}
