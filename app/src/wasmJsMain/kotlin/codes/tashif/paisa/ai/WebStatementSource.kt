package codes.tashif.paisa.ai

import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume

/** A statement the user picked in the browser. */
class WebStatementSource(private val file: File) : StatementSource {

    override val name: String? get() = file.name.ifEmpty { null }

    override val mimeType: String? get() = file.type.ifEmpty { null }

    override suspend fun displayName(): String? = name

    override suspend fun readBytes(maxBytes: Int): ByteArray? {
        if (file.size.toDouble() > maxBytes) throw StatementFileTooLargeException(maxBytes)
        val buffer = readArrayBuffer() ?: return null
        val view = Int8Array(buffer)
        return ByteArray(view.length) { view[it] }
    }

    override suspend fun pdfText(): String =
        throw LlmException("PDF import isn't available on the web yet. Use a CSV, text file or a statement image.")

    /** The file's bytes, or null when the browser can't read it. */
    private suspend fun readArrayBuffer(): ArrayBuffer? = suspendCancellableCoroutine { cont ->
        val reader = FileReader()
        reader.onload = {
            cont.resume(reader.result as? ArrayBuffer)
            null
        }
        reader.onerror = {
            cont.resume(null)
            null
        }
        cont.invokeOnCancellation { runCatching { reader.abort() } }
        reader.readAsArrayBuffer(file)
    }
}
