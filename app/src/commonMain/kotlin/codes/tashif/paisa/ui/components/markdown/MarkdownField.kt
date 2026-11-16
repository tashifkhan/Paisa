package codes.tashif.paisa.ui.components.markdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * Styles Markdown as it is typed. The text itself never changes, so the cursor
 * maps one to one: markers stay where they are, dimmed, and what they wrap
 * takes on the formatting.
 */
internal class MarkdownStyling(private val colors: MdColors) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(styled(text.text), OffsetMapping.Identity)

    fun styled(source: String): AnnotatedString {
        val builder = AnnotatedString.Builder(source)
        val dim = SpanStyle(color = colors.dim)
        val mono = SpanStyle(fontFamily = FontFamily.Monospace)

        fun inline(spans: List<MdSpan>) {
            for (span in spans) {
                if (span.innerStart > span.start) builder.addStyle(dim, span.start, span.innerStart)
                if (span.end > span.innerEnd) builder.addStyle(dim, span.innerEnd, span.end)
                builder.addStyle(span.kind.style(colors), span.innerStart, span.innerEnd)
                inline(span.children)
            }
        }

        var offset = 0
        var fenced = false
        for (line in source.split('\n')) {
            val end = offset + line.length
            var textStart = offset
            val heading = Markdown.headingLine(line)
            val item = Markdown.listLine(line)
            when {
                Markdown.isFence(line) -> {
                    fenced = !fenced
                    builder.addStyle(dim, offset, end)
                    textStart = end
                }
                fenced -> {
                    builder.addStyle(mono, offset, end)
                    textStart = end
                }
                heading != null -> {
                    val marks = line.indexOf('#') + heading.first
                    builder.addStyle(dim, offset, offset + marks)
                    builder.addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontSize = if (heading.first <= 2) 1.2.em else 1.08.em),
                        offset, end
                    )
                    textStart = offset + marks
                }
                Markdown.isRule(line) -> {
                    builder.addStyle(dim, offset, end)
                    textStart = end
                }
                line.trimStart().startsWith(">") -> {
                    val mark = offset + line.indexOf('>') + 1
                    builder.addStyle(dim, offset, mark)
                    builder.addStyle(SpanStyle(color = colors.dim.copy(alpha = 0.9f)), mark, end)
                    textStart = mark
                }
                item != null -> {
                    textStart = offset + item.prefixLength(line)
                    builder.addStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Bold), offset, textStart)
                    if (item.checked == true) {
                        builder.addStyle(
                            SpanStyle(color = colors.dim, textDecoration = TextDecoration.LineThrough),
                            textStart, end
                        )
                    }
                }
            }
            if (textStart < end) inline(Markdown.spans(source, textStart, end))
            offset = end + 1
        }
        return builder.toAnnotatedString()
    }
}

private class Tool(val icon: ImageVector, val label: String, val apply: (MdEdit) -> MdEdit)

private val TOOLS = listOf(
    Tool(Icons.Rounded.FormatBold, "Bold") { MarkdownEdits.wrap(it, "**") },
    Tool(Icons.Rounded.FormatItalic, "Italic") { MarkdownEdits.wrap(it, "*") },
    Tool(Icons.Rounded.FormatStrikethrough, "Strikethrough") { MarkdownEdits.wrap(it, "~~") },
    Tool(Icons.Rounded.Title, "Heading") { MarkdownEdits.linePrefix(it, "## ") },
    Tool(Icons.AutoMirrored.Rounded.FormatListBulleted, "Bullet list") { MarkdownEdits.linePrefix(it, "- ") },
    Tool(Icons.Rounded.FormatListNumbered, "Numbered list") { MarkdownEdits.linePrefix(it, "1. ") },
    Tool(Icons.Rounded.Checklist, "Task list") { MarkdownEdits.linePrefix(it, "- [ ] ") },
    Tool(Icons.Rounded.FormatQuote, "Quote") { MarkdownEdits.linePrefix(it, "> ") },
    Tool(Icons.Rounded.Code, "Code") { MarkdownEdits.wrap(it, "`") },
    Tool(Icons.Rounded.Link, "Link") { MarkdownEdits.link(it) }
)

/**
 * A Markdown note in a card. At rest it shows the rendered note. Tapping it
 * opens the editor, which formats the text while it is typed and has a toolbar
 * for the common marks.
 */
@Composable
fun MarkdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val colors = mdColors()
    val styling = remember(colors) { MarkdownStyling(colors) }
    var editing by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    // The value can change from outside, e.g. a task box ticked in the rendered view.
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))

    fun set(next: TextFieldValue) {
        field = next
        if (next.text != value) onValueChange(next.text)
    }

    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.spacing.medium, end = MaterialTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    haptics.click()
                    // Clearing focus closes the editor through onFocusChanged, so read the state first.
                    val wasEditing = editing
                    if (wasEditing) focusManager.clearFocus()
                    editing = !wasEditing
                }
            ) {
                Text(if (editing) "Done" else "Edit")
            }
        }

        if (!editing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editing = true }
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .padding(bottom = MaterialTheme.spacing.medium)
            ) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    MarkdownText(
                        source = value,
                        onToggleTask = { line -> onValueChange(Markdown.toggleTask(value, line)) }
                    )
                }
            }
        } else {
            BasicTextField(
                value = field,
                onValueChange = { next ->
                    val typedNewline = next.text.length == field.text.length + 1 && next.selection.collapsed &&
                        next.selection.start > 0 && next.text[next.selection.start - 1] == '\n'
                    val continued = if (typedNewline) {
                        MarkdownEdits.continueList(next.text, next.selection.start)
                    } else {
                        null
                    }
                    set(continued?.let { TextFieldValue(it.text, TextRange(it.start, it.end)) } ?: next)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .padding(bottom = MaterialTheme.spacing.small)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            hadFocus = true
                        } else if (hadFocus) {
                            // Tapping elsewhere finishes the edit and shows the rendered note.
                            hadFocus = false
                            editing = false
                        }
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                visualTransformation = styling,
                decorationBox = { inner ->
                    if (field.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.extraSmall)
            ) {
                for (tool in TOOLS) {
                    IconButton(
                        // A mouse click would otherwise move focus here and close the editor.
                        modifier = Modifier.focusProperties { canFocus = false },
                        onClick = {
                            haptics.tick()
                            val sel = field.selection
                            val edit = tool.apply(MdEdit(field.text, sel.min, sel.max))
                            set(TextFieldValue(edit.text, TextRange(edit.start, edit.end)))
                        }
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.label,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
