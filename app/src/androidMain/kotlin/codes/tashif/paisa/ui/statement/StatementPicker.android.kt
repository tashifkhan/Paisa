package codes.tashif.paisa.ui.statement

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import codes.tashif.paisa.ai.AndroidStatementSource
import codes.tashif.paisa.ai.StatementSource

@Composable
internal actual fun rememberStatementPicker(onPicked: (List<StatementSource>) -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val sources = uris.map { AndroidStatementSource(context, it) }
            sources.forEach { it.keepAccess() }
            onPicked(sources)
        }
    }
    return { picker.launch(STATEMENT_MIME_TYPES) }
}

@Composable
internal actual fun rememberPhotoPicker(onPicked: (List<StatementSource>) -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) onPicked(uris.map { AndroidStatementSource(context, it) })
    }
    return {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

private const val MAX_PHOTOS = 10

private val STATEMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "text/csv",
    "text/plain",
    "text/*",
    "application/csv",
    "image/jpeg",
    "image/png",
    "image/webp",
    "*/*"
)
