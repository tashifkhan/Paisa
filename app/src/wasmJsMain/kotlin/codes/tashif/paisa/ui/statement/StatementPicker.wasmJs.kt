package codes.tashif.paisa.ui.statement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import codes.tashif.paisa.ai.StatementSource
import codes.tashif.paisa.ai.WebStatementSource
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.get

/**
 * A hidden `<input type=file multiple>`. Like the Android picker, which
 * offers every file type, it sets no accept filter.
 */
@Composable
internal actual fun rememberStatementPicker(onPicked: (List<StatementSource>) -> Unit): () -> Unit =
    rememberFileInput(accept = null, onPicked = onPicked)

/** The same input, limited to images, so phones offer the camera too. */
@Composable
internal actual fun rememberPhotoPicker(onPicked: (List<StatementSource>) -> Unit): () -> Unit =
    rememberFileInput(accept = "image/jpeg,image/png,image/webp", onPicked = onPicked)

@Composable
private fun rememberFileInput(accept: String?, onPicked: (List<StatementSource>) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onPicked)
    return remember(accept) {
        {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.multiple = true
            if (accept != null) input.accept = accept
            input.onchange = {
                val files = input.files
                val picked = mutableListOf<StatementSource>()
                if (files != null) {
                    for (i in 0 until files.length) {
                        val file: File = files[i] ?: continue
                        picked += WebStatementSource(file)
                    }
                }
                if (picked.isNotEmpty()) callback(picked)
                null
            }
            input.click()
        }
    }
}
