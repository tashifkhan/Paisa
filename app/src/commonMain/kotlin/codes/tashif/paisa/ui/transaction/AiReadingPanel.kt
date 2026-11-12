package codes.tashif.paisa.ui.transaction

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.theme.spacing
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

/** True for photos and screenshots, by extension or MIME type. */
internal val AttachedFile.isImage: Boolean
    get() = name.lowercase().let { n ->
        listOf(".jpg", ".jpeg", ".png", ".webp", ".heic").any(n::endsWith)
    } || source.mimeType?.startsWith("image/") == true

/**
 * Takes the place of the note form while the model reads. Styled after
 * Android's own "Checking for update…" screen: a big left-aligned title over a
 * slowly turning cluster of Material shapes. The model call reports no
 * progress, so nothing here pretends to; a seconds counter shows it's alive.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiReadingPanel(state: AiAddUiState, onCancel: () -> Unit) {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            seconds++
        }
    }

    val images = state.files.count { it.isImage }
    val title = when {
        state.voice != null && state.files.isEmpty() -> "Listening to your voice note…"
        state.files.size > 1 -> "Reading ${state.files.size} files…"
        state.files.size == 1 && images == 1 -> "Reading your image…"
        state.files.size == 1 -> "Reading your file…"
        else -> "Reading your note…"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .padding(top = MaterialTheme.spacing.small)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        Spacer(Modifier.height(MaterialTheme.spacing.smaller))
        Text(
            text = "Finding amounts, dates, merchants and accounts. You'll check every draft before it's saved.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ShapeCluster(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = MaterialTheme.spacing.larger)
        )

        Text(
            text = when {
                seconds < 3 -> " "
                seconds < 25 -> "${seconds}s"
                else -> "${seconds}s · big files and slow models can take a minute"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))

        // What went to the model, read-only, so it's clear what's being worked on.
        if (state.note.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(MaterialTheme.spacing.medium),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = "Your note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(
                        text = state.note.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.small))
        }
        state.voice?.let { clip ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StaticWaveform(clip.bars, width = 96.dp)
                    Spacer(Modifier.width(MaterialTheme.spacing.smaller))
                    Text(clock(clip.durationMs), style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.small))
        }
        if (state.files.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                state.files.forEach { file ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (file.isImage) Icons.Rounded.Image else Icons.Rounded.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(MaterialTheme.spacing.smaller))
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.small))
        }

        Spacer(Modifier.height(MaterialTheme.spacing.smaller))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("Stop", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(MaterialTheme.spacing.smaller))
        Text(
            text = "You can close this sheet. Paisa keeps reading and has the drafts ready when you open it again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Four Material shapes in the theme's tones, like the system update screen:
 * a cookie, a sunny burst, a circle and a pill. Each turns and drifts on its
 * own period, so the cluster never quite repeats. Decorative only.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShapeCluster(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "reading shapes")
    fun spin(periodMs: Int) = infiniteRepeatable<Float>(tween(periodMs, easing = LinearEasing), RepeatMode.Restart)

    val cookieTurn by transition.animateFloat(0f, 360f, spin(22_000), label = "cookie")
    val sunnyTurn by transition.animateFloat(360f, 0f, spin(16_000), label = "sunny")
    // One shared clock for the gentle drift; each shape reads it at its own phase.
    val clock by transition.animateFloat(0f, 1f, spin(6_000), label = "drift")

    val colors = MaterialTheme.colorScheme
    val cookie = MaterialShapes.Cookie4Sided.toShape()
    val sunny = MaterialShapes.Sunny.toShape()
    val circle = MaterialShapes.Circle.toShape()
    // A true capsule; MaterialShapes.Pill pinches into a leaf when stretched tall.
    val pill = CircleShape

    fun wave(phase: Float, amount: Float) = sin((clock + phase) * 2f * PI.toFloat()) * amount

    Box(modifier = modifier.size(width = 260.dp, height = 250.dp)) {
        Blob(
            shape = cookie, color = colors.primary, size = 140.dp,
            x = 112.dp, y = 4.dp, rotation = cookieTurn, dy = wave(0f, 4f)
        )
        Blob(
            shape = circle, color = colors.primaryContainer, size = 66.dp,
            x = 64.dp, y = 22.dp, rotation = 0f, dy = wave(0.25f, 6f)
        )
        Blob(
            shape = sunny, color = colors.secondary, size = 104.dp,
            x = 14.dp, y = 92.dp, rotation = sunnyTurn, dy = wave(0.5f, 5f)
        )
        Blob(
            shape = pill, color = colors.primaryContainer, size = 96.dp, widthFraction = 0.6f,
            x = 112.dp, y = 150.dp, rotation = 35f + wave(0.75f, 10f), dy = wave(0.75f, 3f)
        )
    }
}

@Composable
private fun Blob(
    shape: Shape,
    color: Color,
    size: Dp,
    x: Dp,
    y: Dp,
    rotation: Float,
    dy: Float,
    widthFraction: Float = 1f
) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y + dy.dp)
            .size(width = size * widthFraction, height = size)
            .graphicsLayer { rotationZ = rotation }
            .clip(shape)
            .background(color)
    )
}
