package codes.tashif.paisa.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream

/** A statement picked through the system document picker, read with the content resolver. */
class AndroidStatementSource(
    private val context: Context,
    val uri: Uri,
) : StatementSource {

    override val name: String? get() = uri.lastPathSegment

    override val mimeType: String? get() = context.contentResolver.getType(uri)

    /** Asks to keep read access to the document after this session. Failures are ignored. */
    fun keepAccess() {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override suspend fun displayName(): String? = runCatching {
        context.contentResolver
            .query(uri, null, null, null, null)?.use { cursor ->
                val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (col >= 0 && cursor.moveToFirst()) cursor.getString(col) else null
            }
    }.getOrNull()

    override suspend fun readBytes(maxBytes: Int): ByteArray? =
        context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) throw StatementFileTooLargeException(maxBytes)
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }

    override suspend fun pdfText(): String {
        ensurePdfBox(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { doc ->
                val stripper = PDFTextStripper()
                return stripper.getText(doc).orEmpty()
            }
        }
        return ""
    }

    private companion object {
        @Volatile
        private var pdfBoxReady = false

        private fun ensurePdfBox(context: Context) {
            if (!pdfBoxReady) {
                synchronized(this) {
                    if (!pdfBoxReady) {
                        PDFBoxResourceLoader.init(context.applicationContext)
                        pdfBoxReady = true
                    }
                }
            }
        }
    }
}
