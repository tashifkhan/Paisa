package codes.tashif.paisa.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Reads a supported statement image into the inline format used by vision models. */
object StatementImageReader {
    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

    fun isImage(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
        val name = uri.lastPathSegment?.lowercase().orEmpty()
        return mime.startsWith("image/") || SUPPORTED_EXTENSIONS.any(name::endsWith)
    }

    suspend fun read(context: Context, uri: Uri): LlmImage = withContext(Dispatchers.IO) {
        val mime = resolveMimeType(context, uri)
        if (mime !in SUPPORTED_MIME_TYPES) {
            throw LlmException("Unsupported image format. Choose a JPG, PNG, or WebP image.")
        }

        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_IMAGE_BYTES) {
                    throw LlmException(
                        "Image is larger than 5 MB. Crop or compress it, then try again."
                    )
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: throw LlmException("Could not open the selected image.")

        if (bytes.isEmpty()) throw LlmException("The selected image is empty.")
        LlmImage(
            mimeType = mime,
            base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
        )
    }

    private fun resolveMimeType(context: Context, uri: Uri): String {
        val resolverMime = context.contentResolver.getType(uri)?.lowercase()
        if (resolverMime in SUPPORTED_MIME_TYPES) return resolverMime!!
        val name = uri.lastPathSegment?.lowercase().orEmpty()
        return when {
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
            name.endsWith(".png") -> "image/png"
            name.endsWith(".webp") -> "image/webp"
            else -> resolverMime.orEmpty()
        }
    }

    private val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    private val SUPPORTED_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp")
}
