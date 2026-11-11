package codes.tashif.paisa.ui.statement

import androidx.compose.runtime.Composable
import codes.tashif.paisa.ai.StatementSource

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
 * uses the system photo picker, the web an image-only `<input type=file>`,
 * which phones offer the camera for.
 */
@Composable
internal expect fun rememberPhotoPicker(onPicked: (List<StatementSource>) -> Unit): () -> Unit
