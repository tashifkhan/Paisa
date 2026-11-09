package codes.tashif.paisa.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

private class StreamSink(private val out: OutputStream) : ExportSink {
    override suspend fun write(bytes: ByteArray) = withContext(Dispatchers.IO) { out.write(bytes) }

    override fun close() {
        try {
            out.close()
        } catch (_: Exception) {
        }
    }
}

@Composable
actual fun rememberCreateDocument(
    mimeType: String,
    onSink: (sink: ExportSink?, error: String?) -> Unit
): (fileName: String) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val out = context.contentResolver.openOutputStream(uri)
            if (out != null) onSink(StreamSink(out), null) else onSink(null, "Failed to open destination")
        } catch (e: Exception) {
            onSink(null, e.message ?: "Failed to open destination")
        }
    }
    return { name -> launcher.launch(name) }
}
