package codes.tashif.paisa.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.theme.spacing

@Composable
internal fun FeatureHighlightCard(
    highlight: FeatureHighlight,
    shapeIndex: Int
) {
    // Cycle through M3-inspired multi-corner tiles
    val shape: Shape = when (shapeIndex % 4) {
        0 -> RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 24.dp
        )
        1 -> RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 28.dp,
            bottomStart = 28.dp,
            bottomEnd = 12.dp
        )
        2 -> RoundedCornerShape(20.dp)
        else -> RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
            bottomStart = 8.dp,
            bottomEnd = 8.dp
        )
    }
    val iconShape: Shape = when (shapeIndex % 3) {
        0 -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 6.dp,
            bottomStart = 6.dp,
            bottomEnd = 16.dp
        )
        1 -> CircleShape
        else -> RoundedCornerShape(14.dp)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(iconShape)
                    .background(highlight.container()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = highlight.icon,
                    contentDescription = null,
                    tint = highlight.content(),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = highlight.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
