package sh.taf.billi.ui.components.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Colors the inline styles need, read once from the theme. */
internal class MdColors(val link: Color, val codeBackground: Color, val dim: Color, val accent: Color)

@Composable
internal fun mdColors(): MdColors {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        MdColors(
            link = scheme.primary,
            codeBackground = scheme.surfaceContainerHighest,
            dim = scheme.onSurfaceVariant.copy(alpha = 0.55f),
            accent = scheme.primary
        )
    }
}

internal fun MdKind.style(colors: MdColors): SpanStyle = when (this) {
    MdKind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    MdKind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    MdKind.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    MdKind.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, background = colors.codeBackground)
    MdKind.LINK, MdKind.AUTOLINK -> SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline)
}

/** [text] with its inline Markdown applied and the markers gone. Links open in the browser. */
internal fun renderInline(text: String, colors: MdColors): AnnotatedString = buildAnnotatedString {
    fun emit(from: Int, to: Int, spans: List<MdSpan>) {
        var at = from
        for (span in spans) {
            append(Markdown.unescape(text.substring(at, span.start)))
            val url = span.url
            when {
                span.kind == MdKind.CODE ->
                    withStyle(span.kind.style(colors)) { append(text, span.innerStart, span.innerEnd) }
                url != null -> withLink(LinkAnnotation.Url(url, TextLinkStyles(span.kind.style(colors)))) {
                    if (span.kind == MdKind.AUTOLINK) append(url) else emit(span.innerStart, span.innerEnd, span.children)
                }
                else -> withStyle(span.kind.style(colors)) { emit(span.innerStart, span.innerEnd, span.children) }
            }
            at = span.end
        }
        append(Markdown.unescape(text.substring(at, to)))
    }
    emit(0, text.length, Markdown.spans(text))
}

/**
 * A note rendered as GitHub-flavored Markdown. With [onToggleTask] set, tapping
 * a task box calls it with the source line to flip.
 */
@Composable
fun MarkdownText(
    source: String,
    modifier: Modifier = Modifier,
    onToggleTask: ((line: Int) -> Unit)? = null
) {
    val colors = mdColors()
    val blocks = remember(source) { Markdown.parse(source) }
    val type = MaterialTheme.typography
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Heading -> Text(
                    text = renderInline(block.text, colors),
                    style = when (block.level) {
                        1 -> type.titleLarge
                        2 -> type.titleMedium
                        else -> type.titleSmall
                    },
                    fontWeight = FontWeight.SemiBold
                )
                is MdBlock.Paragraph -> Text(renderInline(block.text, colors), style = type.bodyLarge)
                is MdBlock.ListItem -> Row(modifier = Modifier.padding(start = (block.indent * 16).dp)) {
                    val checked = block.checked
                    if (checked == null) {
                        Text(
                            text = block.marker,
                            style = type.bodyLarge,
                            color = colors.accent,
                            modifier = Modifier.width(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = if (checked) "Done" else "Not done",
                            tint = if (checked) colors.accent else scheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .then(
                                    if (onToggleTask != null) Modifier.clickable { onToggleTask(block.line) } else Modifier
                                )
                        )
                    }
                    Text(
                        text = renderInline(block.text, colors),
                        style = type.bodyLarge,
                        color = if (checked == true) scheme.onSurfaceVariant else scheme.onSurface,
                        textDecoration = if (checked == true) TextDecoration.LineThrough else null
                    )
                }
                is MdBlock.Quote -> Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(scheme.outlineVariant, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = renderInline(block.text, colors),
                        style = type.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                is MdBlock.Code -> Text(
                    text = block.text,
                    style = type.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.codeBackground, RoundedCornerShape(12.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                )
                is MdBlock.Table -> MarkdownTable(block, colors)
                MdBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun MarkdownTable(table: MdBlock.Table, colors: MdColors) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceContainer)
    ) {
        Row(Modifier.fillMaxWidth().background(colors.codeBackground)) {
            for (cell in table.header) {
                Text(
                    text = renderInline(cell, colors),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
        table.rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth()) {
                for (cell in row) {
                    Text(
                        text = renderInline(cell, colors),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
