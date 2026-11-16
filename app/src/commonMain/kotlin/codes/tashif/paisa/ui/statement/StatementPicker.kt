package codes.tashif.paisa.ui.statement

import androidx.compose.runtime.Composable
import codes.tashif.paisa.statement.StatementSource

/**
 * The platform's multi-file picker for statements. Calling the returned
 * function opens it; [onPicked] gets the chosen files and isn't called when
 * the user picks nothing. Android uses the system document picker, the web
 * an `<input type=file>`.
 */
@Composable
internal expect fun rememberStatementPicker(onPicked: (List<StatementSource>) -> Unit): () -> Unit

/**
 * The platform's photo picker, for receipts and payment screenshots. Android
 * uses the system photo picker, the web an image-only `<input type=file>`.
 */
@Composable
internal expect fun rememberPhotoPicker(onPicked: (List<StatementSource>) -> Unit): () -> Unit

/**
 * Opens the camera and returns the photo. Android shows the in-app CameraX
 * capture screen (the same pipeline Pixel Camera uses). The web uses an image
 * input with `capture`, which opens the camera on a phone.
 * [onUnavailable] runs when this device has no camera.
 * [onPermissionDenied] runs when the user refuses the camera.
 */
@Composable
internal expect fun rememberCameraCapture(
    onCaptured: (StatementSource) -> Unit,
    onUnavailable: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
): () -> Unit
