package codes.tashif.paisa.ui.components.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownTest {

    @Test
    fun parsesBlocks() {
        val blocks = Markdown.parse(
            """
            # Bowling night
            Socks for **bowling**
            second line

            - [x] paid
            - [ ] split with Sam
            1. first
            > worth it
            ---
            ```
            ref 631964662252
            ```
            """.trimIndent()
        )
        assertEquals(MdBlock.Heading(1, "Bowling night"), blocks[0])
        assertEquals(MdBlock.Paragraph("Socks for **bowling**\nsecond line"), blocks[1])
        assertEquals(MdBlock.ListItem(0, "•", true, "paid", 4), blocks[2])
        assertEquals(MdBlock.ListItem(0, "•", false, "split with Sam", 5), blocks[3])
        assertEquals(MdBlock.ListItem(0, "1.", null, "first", 6), blocks[4])
        assertEquals(MdBlock.Quote("worth it"), blocks[5])
        assertEquals(MdBlock.Rule, blocks[6])
        assertEquals(MdBlock.Code("ref 631964662252"), blocks[7])
        assertEquals(8, blocks.size)
    }

    @Test
    fun parsesTables() {
        val table = Markdown.parse("| Item | Cost |\n|---|--:|\n| Socks | 120 |\n| Lane |") .single() as MdBlock.Table
        assertEquals(listOf("Item", "Cost"), table.header)
        assertEquals(listOf(listOf("Socks", "120"), listOf("Lane", "")), table.rows)
    }

    @Test
    fun aPipeAloneIsNotATable() {
        // What SMS import writes: "merchant | detail".
        assertEquals(
            listOf<MdBlock>(MdBlock.Paragraph("Torq Socks for Bowling | Torq03 4 · ref 631964662252")),
            Markdown.parse("Torq Socks for Bowling | Torq03 4 · ref 631964662252")
        )
    }

    @Test
    fun autoDescriptionsReadAsWritten() {
        // Notes that SMS and statement import write: "merchant · ref number".
        for (note in listOf(
            "Torq Socks for Bowling | Torq03 4 · ref 631964662252",
            "AMAZON_PAY_INDIA · ref 4521_8890",
            "UPI/DR/4411/SWIGGY*ORDER/okaxis · ref 998",
            "Zomato"
        )) {
            assertEquals(listOf<MdBlock>(MdBlock.Paragraph(note)), Markdown.parse(note), note)
            assertTrue(Markdown.spans(note).isEmpty(), note)
            assertEquals(note, Markdown.plainText(note))
        }
    }

    @Test
    fun findsInlineRuns() {
        val text = "a **b *c*** ~~d~~ `e*` [f](https://x.y) https://z.w."
        val spans = Markdown.spans(text)
        assertEquals(
            listOf(MdKind.BOLD, MdKind.STRIKE, MdKind.CODE, MdKind.LINK, MdKind.AUTOLINK),
            spans.map { it.kind }
        )
        assertEquals(MdKind.ITALIC, spans[0].children.single().kind)
        assertEquals("https://x.y", spans[3].url)
        assertEquals("https://z.w", spans[4].url)
        assertEquals("a b c d e* f https://z.w.", Markdown.strip(text))
    }

    @Test
    fun leavesOrdinaryTextAlone() {
        for (text in listOf("snake_case_name", "2 * 3 * 4", "5 ** 2", "a_b and c_d", "\\*not italic\\*", "ref_no_12")) {
            assertTrue(Markdown.spans(text).isEmpty(), text)
        }
        assertEquals("*not italic*", Markdown.strip("\\*not italic\\*"))
    }

    @Test
    fun plainTextIsOneCleanLine() {
        assertEquals("Bowling Socks for Torq paid", Markdown.plainText("## Bowling\nSocks for **Torq**\n\n- [x] paid"))
        assertEquals("", Markdown.plainText(""))
    }

    @Test
    fun togglesTasks() {
        val source = "note\n- [ ] one\n  * [x] two"
        assertEquals("note\n- [x] one\n  * [x] two", Markdown.toggleTask(source, 1))
        assertEquals("note\n- [ ] one\n  * [ ] two", Markdown.toggleTask(source, 2))
        assertEquals(source, Markdown.toggleTask(source, 0))
    }

    @Test
    fun wrapsAndUnwraps() {
        val bold = MarkdownEdits.wrap(MdEdit("pay rent", 4, 8), "**")
        assertEquals(MdEdit("pay **rent**", 6, 10), bold)
        assertEquals(MdEdit("pay rent", 4, 8), MarkdownEdits.wrap(bold, "**"))
        assertEquals(MdEdit("pay rent", 4, 8), MarkdownEdits.wrap(MdEdit("pay **rent**", 4, 12), "**"))
        // Nothing selected: the cursor lands between the marks.
        assertEquals(MdEdit("a ****", 4, 4), MarkdownEdits.wrap(MdEdit("a ", 2), "**"))
    }

    @Test
    fun prefixesLines() {
        val bullets = MarkdownEdits.linePrefix(MdEdit("one\ntwo", 1, 6), "- ")
        assertEquals("- one\n- two", bullets.text)
        assertEquals("one\ntwo", MarkdownEdits.linePrefix(bullets, "- ").text)
        assertEquals("1. one\n2. two", MarkdownEdits.linePrefix(bullets, "1. ").text)
        assertEquals("- [ ] one\n- [ ] two", MarkdownEdits.linePrefix(bullets, "- [ ] ").text)
        assertEquals(MdEdit("## title", 8, 8), MarkdownEdits.linePrefix(MdEdit("title", 5), "## "))
    }

    @Test
    fun makesLinks() {
        assertEquals(MdEdit("see [bill]()", 11), MarkdownEdits.link(MdEdit("see bill", 4, 8)))
        assertEquals(MdEdit("[](https://a.b)", 1), MarkdownEdits.link(MdEdit("https://a.b", 0, 11)))
    }

    @Test
    fun enterContinuesLists() {
        assertEquals(MdEdit("- a\n- ", 6), MarkdownEdits.continueList("- a\n", 4))
        assertEquals(MdEdit("- [x] a\n- [ ] ", 14), MarkdownEdits.continueList("- [x] a\n", 8))
        assertEquals(MdEdit("  9. a\n  10. ", 13), MarkdownEdits.continueList("  9. a\n", 7))
        // Enter on an empty item ends the list.
        assertEquals(MdEdit("- a\n", 4), MarkdownEdits.continueList("- a\n- \n", 7))
        assertNull(MarkdownEdits.continueList("plain\n", 6))
    }
}
