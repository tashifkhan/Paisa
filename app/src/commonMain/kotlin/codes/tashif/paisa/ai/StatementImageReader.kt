package codes.tashif.paisa.ai

import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

/** Reads a supported statement image into the inline format used by vision models. */
object StatementImageReader {
    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

    fun isImage(source: StatementSource): Boolean {
        val mime = source.mimeType?.lowercase().orEmpty()
        val name = source.name?.lowercase().orEmpty()
        return mime.startsWith("image/") || SUPPORTED_EXTENSIONS.any(name::endsWith)
    }

    suspend fun read(source: StatementSource): LlmImage = withContext(aiIoDispatcher) {
        val mime = resolveMimeType(source)
        if (mime !in SUPPORTED_MIME_TYPES) {
            throw LlmException("Unsupported image format. Choose a JPG, PNG, or WebP image.")
        }

        val bytes = try {
            source.readBytes(MAX_IMAGE_BYTES)
        } catch (_: StatementFileTooLargeException) {
            throw LlmException("Image is larger than 5 MB. Crop or compress it, then try again.")
        } ?: throw LlmException("Could not open the selected image.")

        if (bytes.isEmpty()) throw LlmException("The selected image is empty.")
        LlmImage(
            mimeType = mime,
            base64Data = Base64.encode(bytes)
        )
    }

    private fun resolveMimeType(source: StatementSource): String {
        val reported = source.mimeType?.lowercase()
        if (reported != null && reported in SUPPORTED_MIME_TYPES) return reported
        val name = source.name?.lowercase().orEmpty()
        return when {
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
            name.endsWith(".png") -> "image/png"
            name.endsWith(".webp") -> "image/webp"
            else -> reported.orEmpty()
        }
    }

    private val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    private val SUPPORTED_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp")
}
