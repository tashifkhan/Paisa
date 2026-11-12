package codes.tashif.paisa.ui.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** The web build has no recorder yet, so the mic button stays hidden there. */
internal actual val voiceCaptureSupported: Boolean = false

internal actual suspend fun captureMic(sampleRate: Int, onSamples: (ShortArray, Int) -> Unit) {
    throw UnsupportedOperationException("Voice notes aren't available on the web yet")
}

@Composable
internal actual fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): MicPermission =
    remember {
        object : MicPermission {
            override val granted: Boolean = false
            override fun request() = onResult(false)
        }
    }
