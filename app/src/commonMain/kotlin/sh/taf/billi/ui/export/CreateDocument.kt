package sh.taf.billi.ui.export

import androidx.compose.runtime.Composable
import sh.taf.billi.export.ExportSink

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
