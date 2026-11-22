package sh.taf.billi.ui.platform

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * How far a predictive back swipe has gone on the page that holds it. The
 * Android back handler writes it while the finger moves; [PredictiveBackPage]
 * shrinks the page to match.
 */
class BackProgress {
    /** 0 at rest, 1 at the end of the swipe. */
    var value by mutableFloatStateOf(0f)

    /** True while a finger is on the gesture, so the page tracks it without lag. */
    var dragging by mutableStateOf(false)

    /** True when the swipe started at the left edge of the screen. */
    var fromLeft by mutableStateOf(true)

    fun reset() {
        dragging = false
        value = 0f
    }
}

/** The progress of the page around this point, or null outside one. */
val LocalBackProgress = staticCompositionLocalOf<BackProgress?> { null }

/**
 * A page that shrinks and slides away from the swiped edge during an Android
 * predictive back gesture, as system apps do. Back handlers inside it drive
 * the animation. On the web nothing writes the progress, so it stays still.
 */
@Composable
fun PredictiveBackPage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { BackProgress() }
    val animated by animateFloatAsState(
        targetValue = progress.value,
        animationSpec = if (progress.dragging) snap() else spring(),
        label = "predictive_back"
    )
    val backdrop = MaterialTheme.colorScheme.surfaceDim
    CompositionLocalProvider(LocalBackProgress provides progress) {
        Box(
            modifier = modifier
                .fillMaxSize()
                // A darker tone shows around the shrinking page, so its edges read.
                .drawBehind { if (animated > 0f) drawRect(backdrop) }
                .graphicsLayer {
                    // Material's numbers: down to 90%, and up to a twentieth of
                    // the width minus 8dp toward the side the swipe came from.
                    val scale = 1f - 0.1f * animated
                    scaleX = scale
                    scaleY = scale
                    val shift = (size.width / 20f - 8.dp.toPx()).coerceAtLeast(0f) * animated
                    translationX = if (progress.fromLeft) shift else -shift
                    shape = RoundedCornerShape((28 * animated).dp)
                    clip = animated > 0f
                }
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}
