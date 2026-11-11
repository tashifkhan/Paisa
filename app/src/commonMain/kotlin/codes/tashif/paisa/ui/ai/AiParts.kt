package codes.tashif.paisa.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.AiTestResult
import codes.tashif.paisa.ai.catalog.ModelInfo
import codes.tashif.paisa.ui.theme.spacing

/** Small pills for what a model can take: Image, PDF, Thinking. Unknown shows nothing. */
@Composable
internal fun ModelBadges(model: ModelInfo, modifier: Modifier = Modifier, extra: String? = null) {
    val labels = buildList {
        if (model.acceptsImage == true) add("Image")
        if (model.acceptsPdf == true) add("PDF")
        if (model.reasoning == true) add("Thinking")
        if (model.alias) add("Alias")
    }
    if (labels.isEmpty() && extra == null) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
        labels.forEach { Badge(it, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) }
        extra?.let { Badge(it, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun Badge(text: String, container: Color, content: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = container) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/** Outcome of Test connection: tertiary on success, error colours on failure. */
@Composable
internal fun AiResultCard(result: AiTestResult, modifier: Modifier = Modifier) {
    val container = if (result.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val content = if (result.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    NoteCard(
        text = result.message,
        icon = if (result.isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
        container = container,
        content = content,
        modifier = modifier
    )
}

/** A rounded card with an icon and a line of text. */
@Composable
internal fun NoteCard(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Info,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = container) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = content)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = content)
        }
    }
}
