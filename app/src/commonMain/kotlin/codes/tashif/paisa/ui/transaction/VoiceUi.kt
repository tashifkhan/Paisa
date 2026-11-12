package codes.tashif.paisa.ui.transaction

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.platform.handCursor
import codes.tashif.paisa.ui.theme.spacing
import codes.tashif.paisa.ui.voice.VoiceClip
import codes.tashif.paisa.ui.voice.VoiceRecorder

/**
 * Takes the place of the note field while recording: a live waveform, the
 * time, a way to throw it away, and a done button on a shape that swells
 * with your voice.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun VoiceRecordingPanel(
    recorder: VoiceRecorder,
    onDiscard: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val latest = recorder.levels.lastOrNull() ?: 0f
    val swell by animateFloatAsState(
        targetValue = 1f + latest * 0.45f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "voice swell"
    )
    val transition = rememberInfiniteTransition(label = "recording")
    val turn by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Restart), label = "turn"
    )
    val blink by transition.animateFloat(
        1f, 0.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "blink"
    )
    val left = (VoiceRecorder.MAX_MS - recorder.elapsedMs).coerceAtLeast(0L)

    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        LiveWaveform(
            levels = recorder.levels,
            color = colors.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .padding(horizontal = MaterialTheme.spacing.small)
                .semantics { contentDescription = "Recording" }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDiscard, modifier = Modifier.handCursor()) {
                Icon(Icons.Rounded.Close, contentDescription = "Discard recording")
            }
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = blink }
                    .background(colors.error, CircleShape)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.smaller))
            Text(
                text = clock(recorder.elapsedMs) + if (left <= 15_000L) " · ${left / 1_000}s left" else "",
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = if (left <= 15_000L) colors.error else colors.onSurface,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            Spacer(Modifier.weight(1f))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                Box(
                    Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = swell
                            scaleY = swell
                            rotationZ = turn
                        }
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(colors.primaryContainer)
                )
                FilledIconButton(
                    onClick = onDone,
                    modifier = Modifier.size(48.dp).handCursor(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Done recording")
                }
            }
        }
    }
}

/**
 * Rounded bars, newest on the right, sliding left as you talk. Silence
 * leaves a row of dots; older bars fade toward the left edge.
 */
@Composable
private fun LiveWaveform(levels: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val bar = 4.dp.toPx()
        val gap = 3.dp.toPx()
        val step = bar + gap
        val slots = (size.width / step).toInt().coerceAtLeast(1)
        val shown = levels.takeLast(slots)
        val mid = size.height / 2f
        for (slot in 0 until slots) {
            // Right-align: empty slots on the left stay dots until speech fills them.
            val index = slot - (slots - shown.size)
            val level = if (index >= 0) shown[index] else 0f
            val h = (bar + level * (size.height - bar)).coerceAtMost(size.height)
            val x = slot * step
            val fade = 0.25f + 0.75f * (slot + 1f) / slots
            drawRoundRect(
                color = color.copy(alpha = color.alpha * fade),
                topLeft = Offset(x, mid - h / 2f),
                size = Size(bar, h),
                cornerRadius = CornerRadius(bar / 2f)
            )
        }
    }
}

/** A recorded voice note inside the composer: its waveform, length, and a remove button. */
@Composable
internal fun VoiceTile(clip: VoiceClip, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(MaterialTheme.spacing.smaller))
            StaticWaveform(clip.bars, width = 72.dp)
            Spacer(Modifier.width(MaterialTheme.spacing.smaller))
            Text(clock(clip.durationMs), style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove voice note", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun StaticWaveform(bars: List<Float>, width: Dp, height: Dp = 20.dp) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(width, height)) {
        if (bars.isEmpty()) return@Canvas
        val step = size.width / bars.size
        val bar = (step * 0.6f).coerceAtLeast(1f)
        val mid = size.height / 2f
        bars.forEachIndexed { i, level ->
            val h = (bar + level * (size.height - bar)).coerceAtMost(size.height)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * step, mid - h / 2f),
                size = Size(bar, h),
                cornerRadius = CornerRadius(bar / 2f)
            )
        }
    }
}

/** What the model heard, on the review page, so a mishearing is easy to spot. */
@Composable
internal fun TranscriptNote(transcript: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(Modifier.padding(MaterialTheme.spacing.medium), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Rounded.Mic,
                contentDescription = "Heard in your voice note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Text(
                text = "\"$transcript\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4
            )
        }
    }
}

internal fun clock(ms: Long): String {
    val total = ms / 1_000
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}
