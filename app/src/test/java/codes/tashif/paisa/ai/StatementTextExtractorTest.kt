package codes.tashif.paisa.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementTextExtractorTest {

    @Test
    fun short_text_is_single_chunk() {
        val text = "line1\nline2\nline3"
        val chunks = StatementTextExtractor.chunkText(text, maxChars = 100)
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0])
    }

    @Test
    fun splits_on_newline_near_boundary() {
        val line = "txn row " + "x".repeat(40)
        val text = (1..20).joinToString("\n") { line }
        val chunks = StatementTextExtractor.chunkText(text, maxChars = 200)
        assertTrue(chunks.size > 1)
        // No empty chunks
        assertTrue(chunks.all { it.isNotBlank() })
        // Rejoined content covers full text (whitespace-normalized)
        val rejoined = chunks.joinToString("\n")
        assertTrue(rejoined.replace("\n", "").length >= text.replace("\n", "").length - 20)
        // Chunks prefer line boundaries — each chunk starts at a line start
        chunks.forEach { chunk ->
            assertTrue(chunk.startsWith("txn row") || chunk.startsWith("x"))
        }
    }

    @Test
    fun consecutive_chunks_overlap_at_boundaries() {
        val text = (1..50).joinToString("\n") { "row-$it-ABCDEFGHIJ" }
        val chunks = StatementTextExtractor.chunkText(text, maxChars = 200, overlapChars = 60)
        assertTrue(chunks.size > 1)
        for (i in 0 until chunks.size - 1) {
            val tailLine = chunks[i].lines().last()
            assertTrue(
                "chunk ${i + 1} should re-include boundary line '$tailLine'",
                chunks[i + 1].contains(tailLine)
            )
        }
    }

    @Test
    fun covers_entire_input() {
        val text = (1..50).joinToString("\n") { "row-$it-ABCDEFGHIJ" }
        val chunks = StatementTextExtractor.chunkText(text, maxChars = 80)
        val allContent = chunks.joinToString("").replace("\n", "")
        text.lines().forEach { line ->
            assertTrue("missing $line", allContent.contains(line.replace("\n", "")))
        }
    }
}
