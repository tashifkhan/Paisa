package codes.tashif.paisa.ai

/**
 * A statement file the user picked. On Android it wraps a content Uri; on
 * the web, a browser File.
 */
interface StatementSource {
    /** The last path segment or file name, used to guess the type. Null when unknown. */
    val name: String?

    /** The MIME type the platform reports, or null. */
    val mimeType: String?

    /** The name to show when this file fails, or null when there is none. */
    suspend fun displayName(): String?

    /**
     * The whole file, or null when it can't be opened. Throws
     * [StatementFileTooLargeException] once more than [maxBytes] have been read.
     */
    suspend fun readBytes(maxBytes: Int = Int.MAX_VALUE): ByteArray?

    /** The text layer of a PDF, or "" when the file can't be opened or has none. */
    suspend fun pdfText(): String
}

/** A statement file is bigger than the caller allows. */
class StatementFileTooLargeException(val maxBytes: Int) : Exception("File is larger than $maxBytes bytes")
