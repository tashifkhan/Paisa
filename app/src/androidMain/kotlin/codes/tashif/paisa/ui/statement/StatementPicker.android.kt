package codes.tashif.paisa.ui.statement

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import codes.tashif.paisa.statement.AndroidStatementSource
import codes.tashif.paisa.statement.StatementSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val sources = withContext(Dispatchers.Default) {
                uris.map { uri ->
                    val prepared = BillPhoto.prepare(context, uri) ?: uri
                    AndroidStatementSource(context, prepared)
                }
            }
            picked(sources)
        }
    }
    return {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@Composable
internal actual fun rememberCameraCapture(
    onCaptured: (StatementSource) -> Unit,
    onUnavailable: () -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val captured by rememberUpdatedState(onCaptured)
    val unavailable by rememberUpdatedState(onUnavailable)
    val denied by rememberUpdatedState(onPermissionDenied)
    var open by remember { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) open = true else denied()
    }

    if (open) {
        BillCameraScreen(
            onDismiss = { open = false },
            onFailed = {
                open = false
                unavailable()
            },
            onCaptured = { file ->
                open = false
                scope.launch {
                    val uri = BillPhoto.uriFor(appContext, file)
                    val prepared = withContext(Dispatchers.Default) { BillPhoto.prepare(appContext, uri) }
                    if (prepared == null) unavailable() else captured(AndroidStatementSource(appContext, prepared))
                }
            }
        )
    }

    val launch: () -> Unit = {
        // Don't ask PackageManager for a camera feature. Emulators report none
        // even when the virtual camera binds. A failed bind reports unavailable.
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (allowed) open = true else permission.launch(Manifest.permission.CAMERA)
    }
    return launch
}

private const val MAX_PHOTOS = 10

private val STATEMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "text/csv",
    "text/plain",
    "text/*",
    "application/csv",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "image/jpeg",
    "image/png",
    "image/webp",
    "*/*"
)
