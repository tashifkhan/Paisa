package codes.tashif.paisa.ui.components.markdown

/** Text plus a selection, so the toolbar's edits are plain functions. */
data class MdEdit(val text: String, val start: Int, val end: Int = start)

object MarkdownEdits {

    /** Wraps the selection in [mark], or unwraps it when it already is. */
    fun wrap(edit: MdEdit, mark: String): MdEdit {
        val (text, start, end) = edit
        val n = mark.length
        val selected = text.substring(start, end)
        // The marks sit just outside the selection.
        if (start >= n && text.startsWith(mark, start - n) && text.startsWith(mark, end)) {
            return MdEdit(text.removeRange(end, end + n).removeRange(start - n, start), start - n, end - n)
        }
        // The marks are part of the selection.
        if (selected.length >= 2 * n && selected.startsWith(mark) && selected.endsWith(mark)) {
            val inner = selected.substring(n, selected.length - n)
            return MdEdit(text.replaceRange(start, end, inner), start, start + inner.length)
        }
        return MdEdit(text.replaceRange(start, end, mark + selected + mark), start + n, end + n)
    }

    /** Turns the selection into a link and puts the cursor where the address goes. */
    fun link(edit: MdEdit): MdEdit {
        val (text, start, end) = edit
        val label = text.substring(start, end)
        if (label.startsWith("http://") || label.startsWith("https://")) {
            return MdEdit(text.replaceRange(start, end, "[]($label)"), start + 1)
        }
        val at = start + label.length + 3
        return MdEdit(text.replaceRange(start, end, "[$label]()"), at)
    }

    /**
     * Puts [prefix] at the start of every selected line, or takes it off when
     * they all have it. A different list or heading marker is replaced.
     */
    fun linePrefix(edit: MdEdit, prefix: String): MdEdit {
        val (text, start, end) = edit
        val from = text.lastIndexOf('\n', start - 1) + 1
        val to = text.indexOf('\n', end).let { if (it < 0) text.length else it }
        val lines = text.substring(from, to).split("\n")
        val numbered = prefix == "1. "
        fun has(line: String) = if (numbered) NUMBER.containsMatchIn(line) else line.startsWith(prefix)
        val remove = lines.all { has(it) }
        val changed = lines.mapIndexed { index, line ->
            val bare = MARKER.replaceFirst(line, "")
            when {
                remove -> bare
                numbered -> "${index + 1}. $bare"
                else -> prefix + bare
            }
        }
        val block = changed.joinToString("\n")
        val firstShift = changed.first().length - lines.first().length
        val newStart = (start + firstShift).coerceAtLeast(from)
        val newEnd = (end + block.length - (to - from)).coerceAtLeast(newStart)
        return MdEdit(text.replaceRange(from, to, block), newStart, newEnd)
    }

    /**
     * Call after the user types a newline at [cursor]. Carries a list on to the
     * next line, or ends it when the item just left was empty. Null when the
     * line above is not a list item.
     */
    fun continueList(text: String, cursor: Int): MdEdit? {
        if (cursor == 0 || text[cursor - 1] != '\n') return null
        val lineStart = text.lastIndexOf('\n', cursor - 2) + 1
        val line = text.substring(lineStart, cursor - 1)
        val item = Markdown.listLine(line) ?: return null
        if (item.text.isBlank()) {
            // Enter on an empty item ends the list.
            return MdEdit(text.removeRange(lineStart, cursor), lineStart)
        }
        val marker = if (item.marker[0].isDigit()) {
            "${item.marker.dropLast(1).toLong() + 1}${item.marker.last()}"
        } else {
            item.marker
        }
        val next = item.indent + marker + " " + if (item.task != null) "[ ] " else ""
        return MdEdit(text.substring(0, cursor) + next + text.substring(cursor), cursor + next.length)
    }

    private val NUMBER = Regex("""^\d{1,9}[.)] """)
    private val MARKER = Regex("""^(#{1,6} |> |[-*+] \[[ xX]] |[-*+] |\d{1,9}[.)] )""")
}
