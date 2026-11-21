package codes.tashif.paisa.statement

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxTextTest {

    private fun xlsx(vararg parts: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            parts.forEach { (name, body) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private val workbook = """
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Txns" sheetId="1" r:id="rId1"/>
            <sheet name="Notes" sheetId="2" r:id="rId2"/>
          </sheets>
        </workbook>
    """.trimIndent()

    private val rels = """
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="x" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="x" Target="worksheets/sheet2.xml"/>
        </Relationships>
    """.trimIndent()

    private val strings = """
        <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <si><t>Date</t></si>
          <si><t>Narration</t></si>
          <si><t>Debit</t></si>
          <si><r><t>UPI-</t></r><r><t>Swiggy</t></r></si>
        </sst>
    """.trimIndent()

    // Style 1 uses built-in format 14 (a date), style 2 a custom dd-mmm-yy.
    private val styles = """
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <numFmts count="1"><numFmt numFmtId="164" formatCode="dd\-mmm\-yy"/></numFmts>
          <cellXfs count="3">
            <xf numFmtId="0"/>
            <xf numFmtId="14"/>
            <xf numFmtId="164"/>
          </cellXfs>
        </styleSheet>
    """.trimIndent()

    private val sheet1 = """
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <sheetData>
            <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c><c r="D1" t="s"><v>2</v></c></row>
            <row r="2"><c r="A2" s="1"><v>46207</v></c><c r="B2" t="s"><v>3</v></c><c r="D2"><v>240.5</v></c></row>
            <row r="3"><c r="A3" s="2"><v>46208.5</v></c><c r="B3" t="inlineStr"><is><t>Rent</t></is></c><c r="D3"><v>18000</v></c></row>
          </sheetData>
        </worksheet>
    """.trimIndent()

    private val sheet2 = """
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <sheetData><row r="1"><c r="A1" t="str"><v>checked</v></c></row></sheetData>
        </worksheet>
    """.trimIndent()

    @Test
    fun reads_strings_dates_numbers_and_sheets_in_order() {
        val bytes = xlsx(
            "[Content_Types].xml" to "<Types/>",
            "xl/workbook.xml" to workbook,
            "xl/_rels/workbook.xml.rels" to rels,
            "xl/sharedStrings.xml" to strings,
            "xl/styles.xml" to styles,
            "xl/worksheets/sheet1.xml" to sheet1,
            "xl/worksheets/sheet2.xml" to sheet2,
        )
        val text = XlsxText.read(bytes.inputStream())
        val lines = text.lines()
        assertEquals("Sheet: Txns", lines[0])
        // Column C is empty in every row, so it's dropped.
        assertEquals("Date\tNarration\tDebit", lines[1])
        assertEquals("2026-07-04\tUPI-Swiggy\t240.5", lines[2])
        assertEquals("2026-07-05 12:00\tRent\t18000", lines[3])
        assertTrue(text.contains("Sheet: Notes\nchecked"))
    }

    @Test
    fun a_zip_without_worksheets_reads_as_empty() {
        val bytes = xlsx("word/document.xml" to "<doc/>")
        assertEquals("", XlsxText.read(bytes.inputStream()))
    }

    @Test
    fun date_format_detection_ignores_quoted_text_and_colours() {
        assertTrue(XlsxText.looksLikeDateFormat("dd/mm/yyyy"))
        assertTrue(XlsxText.looksLikeDateFormat("[\$-409]d-mmm-yy;@"))
        assertFalse(XlsxText.looksLikeDateFormat("#,##0.00"))
        assertFalse(XlsxText.looksLikeDateFormat("\"Rs\" #,##0;[Red]-#,##0"))
        assertFalse(XlsxText.looksLikeDateFormat("0.00\\ \"dr\""))
    }

    /** A workbook saved by openpyxl, with real styles, shared strings and empty cells. */
    @Test
    fun reads_a_real_workbook() {
        val stream = javaClass.getResourceAsStream("/statement/hdfc-nov-statement.xlsx")!!
        val lines = XlsxText.read(stream).lines()
        assertEquals("Date\tNarration\tRef\tDebit\tCredit\tBalance", lines[0])
        assertEquals("2026-11-02\tUPI-SWIGGY-swiggy@icici\t4512\t412\t\t25588", lines[1])
        assertEquals("2026-11-05\tNEFT-ACME CORP SALARY\tN88\t\t85000\t110348", lines[3])
        assertEquals(6, lines.size)
    }

    @Test
    fun excelSerialsBecomeDates() {
        org.junit.Assert.assertEquals("2023-03-15", XlsxText.serialToDate(45000.0))
        org.junit.Assert.assertEquals("2023-03-15 12:00", XlsxText.serialToDate(45000.5))
        org.junit.Assert.assertEquals("1900-03-01", XlsxText.serialToDate(61.0))
        org.junit.Assert.assertNull(XlsxText.serialToDate(0.5))
    }
}
