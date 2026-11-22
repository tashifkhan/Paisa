package sh.taf.billi.ui.components.markdown

/** One block of a parsed note. [line] is the source line it starts on. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock

    /** [checked] is null for a plain item, and set for a task item like `- [x] done`. */
    data class ListItem(
        val indent: Int,
        val marker: String,
        val checked: Boolean?,
        val text: String,
        val line: Int
    ) : MdBlock

    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
    data object Rule : MdBlock
}

enum class MdKind { BOLD, ITALIC, STRIKE, CODE, LINK, AUTOLINK }

/**
 * An inline run. [start] to [end] covers the markers too; [innerStart] to
 * [innerEnd] is the text they wrap.
 */
data class MdSpan(
    val kind: MdKind,
    val start: Int,
    val end: Int,
    val innerStart: Int,
    val innerEnd: Int,
    val url: String? = null,
    val children: List<MdSpan> = emptyList()
)

/**
 * The parts of GitHub-flavored Markdown a transaction note needs: headings,
 * bold, italic, strikethrough, inline and fenced code, links and bare URLs,
 * bullet, numbered and task lists, quotes, tables and rules. No images, HTML,
 * footnotes or nested quotes.
 */
object Markdown {

    private val LIST = Regex("""^(\s*)([-*+]|\d{1,9}[.)])\s+(\[[ xX]]\s+)?(.*)$""")
    private val HEADING = Regex("""^ {0,3}(#{1,6})\s+(.*?)\s*#*\s*$""")
    private val TABLE_RULE = Regex("""^\s*\|?\s*:?-+:?\s*(\|\s*:?-+:?\s*)*\|?\s*$""")

    class ListLine(val indent: String, val marker: String, val task: String?, val text: String) {
        val checked: Boolean? get() = task?.let { it[1] != ' ' }

        /** Length of everything before the item's text. */
        fun prefixLength(line: String): Int = line.length - text.length
    }

    fun listLine(line: String): ListLine? = LIST.matchEntire(line)?.let { m ->
        ListLine(m.groupValues[1], m.groupValues[2], m.groupValues[3].ifEmpty { null }, m.groupValues[4])
    }

    /** Heading level and the offset where its text starts, or null. */
    fun headingLine(line: String): Pair<Int, String>? =
        HEADING.matchEntire(line)?.let { it.groupValues[1].length to it.groupValues[2] }

    fun isFence(line: String): Boolean = line.trimStart().let { it.startsWith("```") || it.startsWith("~~~") }

    fun isRule(line: String): Boolean {
        val marks = line.filterNot { it == ' ' }
        return marks.length >= 3 && marks[0] in "-*_" && marks.all { it == marks[0] }
    }

    fun parse(source: String): List<MdBlock> {
        val lines = source.lines()
        val out = mutableListOf<MdBlock>()
        val paragraph = mutableListOf<String>()
        fun flush() {
            if (paragraph.isNotEmpty()) out += MdBlock.Paragraph(paragraph.joinToString("\n"))
            paragraph.clear()
        }
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                isFence(line) -> {
                    flush()
                    val code = mutableListOf<String>()
                    i++
                    while (i < lines.size && !isFence(lines[i])) code += lines[i++]
                    out += MdBlock.Code(code.joinToString("\n"))
                }
                line.isBlank() -> flush()
                headingLine(line) != null -> {
                    flush()
                    val (level, text) = headingLine(line)!!
                    out += MdBlock.Heading(level, text)
                }
                isRule(line) -> {
                    flush()
                    out += MdBlock.Rule
                }
                line.trimStart().startsWith(">") -> {
                    flush()
                    val quote = mutableListOf<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                        quote += lines[i].trimStart().removePrefix(">").removePrefix(" ")
                        i++
                    }
                    i--
                    out += MdBlock.Quote(quote.joinToString("\n"))
                }
                '|' in line && i + 1 < lines.size && '-' in lines[i + 1] && TABLE_RULE.matches(lines[i + 1]) -> {
                    flush()
                    val header = cells(line)
                    val rows = mutableListOf<List<String>>()
                    i += 2
                    while (i < lines.size && '|' in lines[i] && lines[i].isNotBlank()) {
                        // Short rows are padded and long ones cut, as GitHub does.
                        val row = cells(lines[i++])
                        rows += List(header.size) { row.getOrElse(it) { "" } }
                    }
                    i--
                    out += MdBlock.Table(header, rows)
                }
                listLine(line) != null -> {
                    flush()
                    val item = listLine(line)!!
                    val bullet = if (item.marker[0].isDigit()) item.marker.dropLast(1) + "." else "•"
                    out += MdBlock.ListItem(item.indent.length / 2, bullet, item.checked, item.text, i)
                }
                else -> paragraph += line.trim()
            }
            i++
        }
        flush()
        return out
    }

    private fun cells(line: String): List<String> =
        line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

    /** Flips the task box on source line [line], if it has one. */
    fun toggleTask(source: String, line: Int): String {
        val lines = source.lines().toMutableList()
        val text = lines.getOrNull(line) ?: return source
        val item = listLine(text) ?: return source
        val task = item.task ?: return source
        val at = item.indent.length + item.marker.length + (text.length - item.indent.length - item.marker.length -
            task.length - item.text.length) + 1
        lines[line] = text.substring(0, at) + (if (text[at] == ' ') 'x' else ' ') + text.substring(at + 1)
        return lines.joinToString("\n")
    }

    /** Inline runs in [text] between [from] and [to], which should sit on one line. */
    fun spans(text: String, from: Int = 0, to: Int = text.length): List<MdSpan> {
        val out = mutableListOf<MdSpan>()
        var i = from
        while (i < to) {
            val c = text[i]
            if (c == '\\') {
                i += 2
                continue
            }
            if (c == '`') {
                val j = text.indexOf('`', i + 1)
                if (j > i + 1 && j < to) {
                    out += MdSpan(MdKind.CODE, i, j + 1, i + 1, j)
                    i = j + 1
                    continue
                }
            } else if (i + 1 < to && text[i + 1] == c && c in "*_~") {
                val j = closingDouble(text, c, i + 2, to)
                if (j >= 0) {
                    val kind = if (c == '~') MdKind.STRIKE else MdKind.BOLD
                    out += MdSpan(kind, i, j + 2, i + 2, j, children = spans(text, i + 2, j))
                    i = j + 2
                } else {
                    i += 2
                }
                continue
            } else if (c == '*' || c == '_') {
                val insideWord = c == '_' && i > from && text[i - 1].isLetterOrDigit()
                val j = if (insideWord) -1 else closingSingle(text, c, i + 1, to)
                if (j >= 0) {
                    out += MdSpan(MdKind.ITALIC, i, j + 1, i + 1, j, children = spans(text, i + 1, j))
                    i = j + 1
                    continue
                }
            } else if (c == '[') {
                val mid = text.indexOf("](", i + 1)
                val end = if (mid > i + 1 && mid < to) text.indexOf(')', mid + 2) else -1
                if (end > mid + 2 && end < to) {
                    val url = text.substring(mid + 2, end)
                    if (url.none { it.isWhitespace() } && '\n' !in text.substring(i, mid)) {
                        out += MdSpan(MdKind.LINK, i, end + 1, i + 1, mid, url, spans(text, i + 1, mid))
                        i = end + 1
                        continue
                    }
                }
            } else if (c == 'h' && (text.startsWith("http://", i) || text.startsWith("https://", i)) &&
                (i == from || !text[i - 1].isLetterOrDigit())
            ) {
                var j = i
                while (j < to && !text[j].isWhitespace() && text[j] !in "<>") j++
                while (j > i && text[j - 1] in ".,;:!?)") j--
                if (j > i + 8) {
                    out += MdSpan(MdKind.AUTOLINK, i, j, i, j, text.substring(i, j))
                    i = j
                    continue
                }
            }
            i++
        }
        return out
    }

    private fun closingDouble(text: String, c: Char, start: Int, to: Int): Int {
        if (start >= to || text[start].isWhitespace()) return -1
        var k = start + 1
        while (k + 1 < to) {
            if (text[k] == c && text[k + 1] == c && !text[k - 1].isWhitespace()) {
                // "***x***": leave the inner mark for the italic inside.
                return if (k + 2 < to && text[k + 2] == c) k + 1 else k
            }
            k++
        }
        return -1
    }

    private fun closingSingle(text: String, c: Char, start: Int, to: Int): Int {
        if (start >= to || text[start].isWhitespace()) return -1
        var k = start + 1
        while (k < to) {
            if (text[k] == c) {
                val doubled = k + 1 < to && text[k + 1] == c
                when {
                    doubled -> {
                        // Skip a bold run nested inside, such as "*a **b** c*".
                        val close = closingDouble(text, c, k + 2, to)
                        k = if (close >= 0) close + 1 else k + 1
                    }
                    text[k - 1].isWhitespace() -> Unit
                    c == '_' && k + 1 < to && text[k + 1].isLetterOrDigit() -> Unit
                    else -> return k
                }
            }
            k++
        }
        return -1
    }

    /** [text] between [from] and [to] with the inline markers taken out. */
    fun strip(text: String, from: Int = 0, to: Int = text.length, spans: List<MdSpan> = spans(text, from, to)): String =
        buildString {
            var at = from
            for (span in spans) {
                append(unescape(text.substring(at, span.start)))
                if (span.kind == MdKind.CODE || span.kind == MdKind.AUTOLINK) {
                    append(text, span.innerStart, span.innerEnd)
                } else {
                    append(strip(text, span.innerStart, span.innerEnd, span.children))
                }
                at = span.end
            }
            append(unescape(text.substring(at, to)))
        }

    /** Drops the backslash from escapes such as `\*`. */
    fun unescape(text: String): String {
        if ('\\' !in text) return text
        return buildString {
            var i = 0
            while (i < text.length) {
                if (text[i] == '\\' && i + 1 < text.length && !text[i + 1].isLetterOrDigit() && text[i + 1] != ' ') i++
                append(text[i++])
            }
        }
    }

    /** The note as one line of plain text, for titles, rows and search. */
    fun plainText(source: String): String = parse(source).mapNotNull { block ->
        when (block) {
            is MdBlock.Heading -> strip(block.text)
            is MdBlock.Paragraph -> strip(block.text.replace('\n', ' '))
            is MdBlock.ListItem -> strip(block.text)
            is MdBlock.Quote -> strip(block.text.replace('\n', ' '))
            is MdBlock.Code -> block.text.replace('\n', ' ')
            is MdBlock.Table -> block.header.joinToString(" ") { strip(it) }
            MdBlock.Rule -> null
        }
    }.filter { it.isNotBlank() }.joinToString(" ")
}
