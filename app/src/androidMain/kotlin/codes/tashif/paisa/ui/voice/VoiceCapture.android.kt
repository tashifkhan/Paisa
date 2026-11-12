package codes.tashif.paisa.ui.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

internal actual val voiceCaptureSupported: Boolean = true

@SuppressLint("MissingPermission") // The UI asks for RECORD_AUDIO before it starts a recording.
internal actual suspend fun captureMic(sampleRate: Int, onSamples: (ShortArray, Int) -> Unit) {
    val channel = AudioFormat.CHANNEL_IN_MONO
    val encoding = AudioFormat.ENCODING_PCM_16BIT
    val chunk = sampleRate / VoiceRecorder.LEVELS_PER_SECOND
    val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
    if (minBuffer <= 0) throw IllegalStateException("This device can't record at $sampleRate Hz")

    val record = withContext(Dispatchers.IO) {
        // VOICE_RECOGNITION skips the gain and effects tuned for calls, which suits speech-to-model.
        AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, channel, encoding, maxOf(minBuffer, chunk * 4))
    }
    if (record.state != AudioRecord.STATE_INITIALIZED) {
        record.release()
        throw IllegalStateException("Couldn't open the microphone. Another app may be using it")
    }
    try {
        record.startRecording()
        val buffer = ShortArray(chunk)
        while (currentCoroutineContext().isActive) {
            val read = withContext(Dispatchers.IO) { record.read(buffer, 0, buffer.size) }
            if (read < 0) throw IllegalStateException("The microphone stopped working ($read)")
            onSamples(buffer, read)
        }
    } finally {
        runCatching { record.stop() }
        record.release()
    }
}

@Composable
internal actual fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): MicPermission {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    fun check() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(check()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok || check()
        callback(granted)
    }
    return remember(launcher) {
        object : MicPermission {
            override val granted: Boolean get() = granted
            override fun request() = launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
