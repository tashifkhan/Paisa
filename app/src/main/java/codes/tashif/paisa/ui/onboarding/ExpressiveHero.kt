package codes.tashif.paisa.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Stacked multi-shape hero — cookie-cutter layers like M3 expressive demos.
 */
@Composable
internal fun ExpressiveHero(
    icon: ImageVector,
    tone: Tone,
    pageIndex: Int
) {
    val outer = tone.container()
    val inner = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val accent = when (pageIndex % 3) {
        0 -> MaterialTheme.colorScheme.secondaryContainer
        1 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    // Compact multi-shape hero — closer to home-screen icon scale
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .offset(x = 6.dp, y = 5.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 16.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 32.dp
                    )
                )
                .background(accent.copy(alpha = 0.7f))
        )
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 30.dp,
                        bottomStart = 26.dp,
                        bottomEnd = 14.dp
                    )
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(outer, outer.copy(alpha = 0.75f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 10.dp,
                            bottomStart = 10.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(inner),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tone.content(),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 6.dp),
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 3.dp,
                bottomStart = 3.dp,
                bottomEnd = 10.dp
            ),
            color = MaterialTheme.colorScheme.tertiary,
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onTertiary)
                )
            }
        }
    }
}
