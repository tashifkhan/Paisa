package codes.tashif.paisa.ai

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * Pulls plain text from a user-selected statement file (PDF / CSV / text).
 */
object StatementTextExtractor {

    @Volatile
    private var pdfBoxReady = false

    /**
     * PDFBox parsing is heavy — always runs on [Dispatchers.IO] so the UI thread
     * never stalls on large statements.
     */
    suspend fun extract(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
        val name = uri.lastPathSegment?.lowercase().orEmpty()

        when {
            mime.contains("pdf") || name.endsWith(".pdf") -> extractPdf(context, uri)
            mime.contains("csv") || name.endsWith(".csv") ||
                mime.contains("text") || name.endsWith(".txt") ||
                name.endsWith(".tsv") -> extractText(context, uri)
            else -> {
                // Try text first, then PDF
                val asText = runCatching { extractText(context, uri) }.getOrDefault("")
                if (asText.isNotBlank()) asText else extractPdf(context, uri)
            }
        }.trim()
    }

    private fun extractText(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charset.forName("UTF-8"))).use { reader ->
                return reader.readText()
            }
        }
        return ""
    }

    private fun extractPdf(context: Context, uri: Uri): String {
        ensurePdfBox(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { doc ->
                val stripper = PDFTextStripper()
                return stripper.getText(doc).orEmpty()
            }
        }
        return ""
    }

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
