package codes.tashif.paisa.ui.settings

import androidx.compose.runtime.Composable

/** Where an export goes: a document the user picked on Android, a download on the web. */
interface ExportSink {
    suspend fun write(bytes: ByteArray)

    /** Finishes the file. Safe to call more than once. */
    fun close()
}

/**
 * Asks where to save a new [mimeType] file, then hands back a sink for it.
 * [onSink] gets null when the user cancels, or an error message when the
 * destination can't be opened. Returns the function that starts the request
 * with a suggested file name.
 */
@Composable
expect fun rememberCreateDocument(
    mimeType: String,
    onSink: (sink: ExportSink?, error: String?) -> Unit
): (fileName: String) -> Unit
