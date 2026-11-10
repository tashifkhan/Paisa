package codes.tashif.paisa.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Switches between side-by-side choices, like pill tabs, with a sideways
 * swipe anywhere on the content. Swiping left goes to [onNext], right to
 * [onPrevious]. Vertical scrolling and text fields keep their own drags;
 * only a clear sideways swipe of 64dp or more counts.
 */
@Composable
fun Modifier.swipeToSwitch(
    enabled: Boolean = true,
    onPrevious: () -> Unit,
    onNext: () -> Unit
): Modifier {
    if (!enabled) return this
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    val previous by rememberUpdatedState(onPrevious)
    val next by rememberUpdatedState(onNext)
    return pointerInput(threshold) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                when {
                    total <= -threshold -> next()
                    total >= threshold -> previous()
                }
            },
            onHorizontalDrag = { change, amount ->
                total += amount
                if (abs(total) > threshold / 4) change.consume()
            }
        )
    }
}
