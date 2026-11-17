package codes.tashif.paisa.statement

import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * Turns an .xlsx workbook into tab-separated text, one block per sheet, so
 * the model reads a spreadsheet the same way it reads a CSV.
 *
 * An .xlsx is a zip of XML parts. This reads the shared strings, the cell
 * styles (to tell date cells from plain numbers) and every worksheet. Dates
 * come out as yyyy-MM-dd, or yyyy-MM-dd HH:mm when they carry a time.
 */
object XlsxText {

    /** Stops runaway sheets. A statement is a few thousand rows at most. */
    private const val MAX_ROWS_PER_SHEET = 20_000

    fun read(input: InputStream): String {
        val parts = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.trimStart('/')
                if (name.startsWith("xl/") && name.endsWith(".xml") || name.endsWith(".rels")) {
                    parts[name] = zip.readBytes()
                }
            }
        }
        if (parts.none { it.key.startsWith("xl/worksheets/") }) return ""

        val strings = parts["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        val dateStyles = parts["xl/styles.xml"]?.let(::parseDateStyles).orEmpty()
        val sheets = sheetOrder(parts)

        return buildString {
            sheets.forEach { (title, path) ->
                val bytes = parts[path] ?: return@forEach
                val rows = parseSheet(bytes, strings, dateStyles)
                if (rows.isEmpty()) return@forEach
                if (isNotEmpty()) append("\n\n")
                if (sheets.size > 1) append("Sheet: ").append(title).append('\n')
                rows.forEach { append(it.joinToString("\t")).append('\n') }
            }
        }.trim()
    }

    /** Sheet titles and their part paths, in workbook order. */
    private fun sheetOrder(parts: Map<String, ByteArray>): List<Pair<String, String>> {
        val targets = HashMap<String, String>()
        parts["xl/_rels/workbook.xml.rels"]?.let { bytes ->
            sax(bytes, object : DefaultHandler() {
                override fun startElement(uri: String?, localName: String?, qName: String, atts: Attributes) {
                    if (local(qName) != "Relationship") return
                    val id = atts.getValue("Id") ?: return
                    val target = atts.getValue("Target") ?: return
                    targets[id] = if (target.startsWith("/")) target.trimStart('/') else "xl/$target"
                }
            })
        }
        val ordered = mutableListOf<Pair<String, String>>()
        parts["xl/workbook.xml"]?.let { bytes ->
            sax(bytes, object : DefaultHandler() {
                override fun startElement(uri: String?, localName: String?, qName: String, atts: Attributes) {
                    if (local(qName) != "sheet") return
                    val title = atts.getValue("name") ?: "Sheet ${ordered.size + 1}"
                    val rel = attr(atts, "id") ?: return
                    targets[rel]?.let { ordered += title to it }
                }
            })
        }
        if (ordered.isNotEmpty()) return ordered
        // No workbook index: take the worksheets by number.
        return parts.keys
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
            .mapIndexed { i, path -> "Sheet ${i + 1}" to path }
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val out = mutableListOf<String>()
        val text = StringBuilder()
        var inItem = false
        var inText = false
        var inPhonetic = false
        sax(bytes, object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String, atts: Attributes) {
                when (local(qName)) {
                    "si" -> { inItem = true; text.clear() }
                    "t" -> inText = inItem && !inPhonetic
                    "rPh" -> inPhonetic = true
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String) {
                when (local(qName)) {
                    "si" -> { out += text.toString(); inItem = false }
                    "t" -> inText = false
                    "rPh" -> inPhonetic = false
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inText) text.appendRange(ch, start, start + length)
            }
        })
        return out
    }

    /** Indexes into cellXfs whose number format shows a date. */
    private fun parseDateStyles(bytes: ByteArray): Set<Int> {
        val customDateFormats = HashSet<Int>()
        val dateStyles = HashSet<Int>()
        var inCellXfs = false
        var xfIndex = 0
        sax(bytes, object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String, atts: Attributes) {
                when (local(qName)) {
                    "numFmt" -> {
                        val id = atts.getValue("numFmtId")?.toIntOrNull() ?: return
                        if (looksLikeDateFormat(atts.getValue("formatCode").orEmpty())) customDateFormats += id
                    }
                    "cellXfs" -> { inCellXfs = true; xfIndex = 0 }
                    "xf" -> if (inCellXfs) {
                        val id = atts.getValue("numFmtId")?.toIntOrNull() ?: 0
                        if (id in BUILT_IN_DATE_FORMATS || id in customDateFormats) dateStyles += xfIndex
                        xfIndex++
                    }
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String) {
                if (local(qName) == "cellXfs") inCellXfs = false
            }
        })
        return dateStyles
    }

    private fun parseSheet(bytes: ByteArray, strings: List<String>, dateStyles: Set<Int>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = sortedMapOf<Int, String>()
        var nextColumn = 0
        var column = 0
        var type = ""
        var style = -1
        val value = StringBuilder()
        var inValue = false
        var done = false

        sax(bytes, object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String, atts: Attributes) {
                if (done) return
                when (local(qName)) {
                    "row" -> { row = sortedMapOf(); nextColumn = 0 }
                    "c" -> {
                        column = atts.getValue("r")?.let(::columnIndex) ?: nextColumn
                        nextColumn = column + 1
                        type = atts.getValue("t").orEmpty()
                        style = atts.getValue("s")?.toIntOrNull() ?: -1
                        value.clear()
                    }
                    // <v> holds the value; <t> holds inline string text.
                    "v", "t" -> inValue = true
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String) {
                if (done) return
                when (local(qName)) {
                    "v", "t" -> inValue = false
                    "c" -> {
                        val cell = cellText(value.toString(), type, style, strings, dateStyles)
                        if (cell.isNotBlank()) row[column] = cell
                    }
                    "row" -> if (row.isNotEmpty()) {
                        val width = row.lastKey() + 1
                        rows += List(width) { row[it].orEmpty() }
                        if (rows.size >= MAX_ROWS_PER_SHEET) done = true
                    }
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inValue) value.appendRange(ch, start, start + length)
            }
        })
        return trimEmptyColumns(rows)
    }

    private fun cellText(raw: String, type: String, style: Int, strings: List<String>, dateStyles: Set<Int>): String {
        val text = when (type) {
            "s" -> raw.trim().toIntOrNull()?.let { strings.getOrNull(it) }.orEmpty()
            "b" -> if (raw.trim() == "1") "TRUE" else "FALSE"
            "inlineStr", "str", "e" -> raw
            else -> {
                val number = raw.trim().toDoubleOrNull()
                when {
                    number == null -> raw
                    style in dateStyles -> serialToDate(number) ?: plainNumber(number)
                    else -> plainNumber(number)
                }
            }
        }
        // Tabs and line breaks would break the row layout the model reads.
        return text.replace(Regex("""[\t\r\n]+"""), " ").trim()
    }

    /** Excel's 1900 date system, with its fake 29 Feb 1900 skipped by the 1899-12-30 epoch. */
    internal fun serialToDate(serial: Double): String? {
        if (serial < 1 || serial > 2_958_465) return null
        val days = serial.toLong()
        val minutes = Math.round((serial - days) * 24 * 60).toInt()
        val date = LocalDate.of(1899, 12, 30).plusDays(days)
        if (minutes == 0) return date.toString()
        val time = LocalDateTime.of(date, java.time.LocalTime.MIDNIGHT).plusMinutes(minutes.toLong())
        return "%s %02d:%02d".format(time.toLocalDate(), time.hour, time.minute)
    }

    private fun plainNumber(number: Double): String =
        if (number == Math.rint(number) && kotlin.math.abs(number) < 1e15) {
            number.toLong().toString()
        } else {
            // Float noise like 1234.5000000001 reads as a different amount.
            java.math.BigDecimal(number).round(java.math.MathContext(15)).stripTrailingZeros().toPlainString()
        }

    /** "AB12" is column 27 (zero-based). */
    private fun columnIndex(ref: String): Int? {
        var index = 0
        var letters = 0
        for (ch in ref) {
            if (ch !in 'A'..'Z') break
            index = index * 26 + (ch - 'A' + 1)
            letters++
        }
        return if (letters == 0) null else index - 1
    }

    /** Drops columns that are empty in every row, which formatted-but-blank sheets have plenty of. */
    private fun trimEmptyColumns(rows: List<List<String>>): List<List<String>> {
        if (rows.isEmpty()) return rows
        val width = rows.maxOf { it.size }
        val keep = (0 until width).filter { c -> rows.any { it.getOrNull(c)?.isNotEmpty() == true } }
        if (keep.size == width) return rows
        return rows.map { r -> keep.map { r.getOrNull(it).orEmpty() } }
    }

    internal fun looksLikeDateFormat(code: String): Boolean {
        // Strip quoted text, escapes and colour/condition brackets, e.g. [Red] or "Rs".
        val bare = code
            .replace(Regex("\"[^\"]*\""), "")
            .replace(Regex("""\\."""), "")
            .replace(Regex("""\[[^\]]*]"""), "")
            .lowercase()
        return bare.any { it == 'd' || it == 'y' } || (bare.contains('m') && bare.contains('h'))
    }

    private val BUILT_IN_DATE_FORMATS = (14..22).toSet() + (27..36) + (45..47) + (50..58)

    private fun sax(bytes: ByteArray, handler: DefaultHandler) {
        val factory = SAXParserFactory.newInstance().apply { isNamespaceAware = false }
        // A workbook has no business declaring entities. Refuse them.
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        factory.newSAXParser().parse(bytes.inputStream(), handler)
    }

    private fun local(qName: String): String = qName.substringAfter(':')

    /** Reads `r:id` whatever the namespace prefix. */
    private fun attr(atts: Attributes, localName: String): String? {
        for (i in 0 until atts.length) {
            if (local(atts.getQName(i)) == localName && atts.getQName(i).contains(':')) return atts.getValue(i)
        }
        return null
    }
}
