package sh.taf.billi.statement

import sh.taf.billi.ai.LlmException
import sh.taf.billi.ai.LlmPdf
import sh.taf.billi.ai.aiIoDispatcher
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

/**
 * Pulls plain text from a user-selected statement file (PDF / CSV / XLSX / text).
 */
object StatementTextExtractor {

    /**
     * PDF parsing is heavy, so this runs on the IO dispatcher and the UI
     * thread never stalls on large statements.
     */
    suspend fun extract(source: StatementSource): String = withContext(aiIoDispatcher) {
        val mime = source.mimeType?.lowercase().orEmpty()
        val name = source.name?.lowercase().orEmpty()

        when {
            isSpreadsheet(source) -> source.spreadsheetText()
            isLegacyExcel(source) -> throw LlmException(
                "Old .xls workbooks can't be read. Save it as .xlsx or CSV and share that."
            )
            mime.contains("pdf") || name.endsWith(".pdf") -> source.pdfText()
            mime.contains("csv") || name.endsWith(".csv") ||
                mime.contains("text") || name.endsWith(".txt") ||
                name.endsWith(".tsv") -> extractText(source)
            else -> {
                // Try text first, then PDF
                val asText = runCatching { extractText(source) }.getOrDefault("")
                if (asText.isNotBlank()) asText else source.pdfText()
            }
        }.trim()
    }

    private suspend fun extractText(source: StatementSource): String =
        source.readBytes()?.decodeToString().orEmpty()

    /** An .xlsx workbook (Excel, Google Sheets or Numbers export). */
    fun isSpreadsheet(source: StatementSource): Boolean {
        val mime = source.mimeType?.lowercase().orEmpty()
        val name = source.name?.lowercase().orEmpty()
        return mime == XLSX_MIME || name.endsWith(".xlsx")
    }

    private fun isLegacyExcel(source: StatementSource): Boolean {
        val mime = source.mimeType?.lowercase().orEmpty()
        val name = source.name?.lowercase().orEmpty()
        if (name.endsWith(".xls")) return true
        // Plenty of apps label CSV files with the Excel type, so trust a text extension first.
        val textName = listOf(".csv", ".tsv", ".txt").any(name::endsWith)
        return mime == "application/vnd.ms-excel" && !textName
    }

    private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun isPdf(source: StatementSource): Boolean {
        val mime = source.mimeType?.lowercase().orEmpty()
        val name = source.name?.lowercase().orEmpty()
        return mime.contains("pdf") || name.endsWith(".pdf")
    }

    /** The whole file for a model that reads PDFs, or null when it's too big to send. */
    suspend fun readPdf(source: StatementSource): LlmPdf? = withContext(aiIoDispatcher) {
        val bytes = try {
            source.readBytes(MAX_PDF_BYTES)
        } catch (_: StatementFileTooLargeException) {
            null
        } ?: return@withContext null
        if (bytes.isEmpty()) return@withContext null
        LlmPdf(
            base64Data = Base64.encode(bytes),
            name = source.name?.substringAfterLast('/')
        )
    }

    private const val MAX_PDF_BYTES = 15 * 1024 * 1024

    /**
     * Split long statements into newline-aligned chunks that fit LLM context limits.
     * Chunks never split a line, and consecutive chunks overlap by a few lines so a
     * multi-line transaction at a boundary is fully visible in at least one chunk
     * (the extraction pass dedupes anything seen twice).
     */
    fun chunkText(text: String, maxChars: Int = 12_000, overlapChars: Int = 500): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val chunks = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val end = minOf(i + maxChars, text.length)
            // Prefer break on newline near the end
            var cut = end
            if (end < text.length) {
                val nl = text.lastIndexOf('\n', end)
                if (nl > i + maxChars / 2) cut = nl
            }
            chunks.add(text.substring(i, cut).trim())
            if (cut >= text.length) break
            // Step back a little, aligned to a line start, to overlap the boundary.
            var next = cut
            val back = text.lastIndexOf('\n', maxOf(i, cut - overlapChars))
            if (back > i) next = back
            if (next <= i) next = cut
            i = next
            while (i < text.length && text[i].isWhitespace()) i++
        }
        return chunks.filter { it.isNotBlank() }
    }
}
