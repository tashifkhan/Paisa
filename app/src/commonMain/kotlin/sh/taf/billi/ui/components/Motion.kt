package sh.taf.billi.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Shared motion for the app. The spring values come from Expressive Clock
 * (https://github.com/samuelbeckman/expressive-clock, MIT), where the same
 * few springs run every list, panel and empty state. See COPYRIGHT.
 */

/** For `animateItem(placementSpec = ...)`: rows glide to their new place and settle with a small bounce. */
fun <T> listPlacementSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.68f, stiffness = 380f)

/** A panel opening inside a card. Pairs with [springCollapse]. */
fun springExpand(): EnterTransition =
    expandVertically(spring(dampingRatio = 0.8f, stiffness = 450f)) + fadeIn(tween(180))

fun springCollapse(): ExitTransition =
    shrinkVertically(spring(dampingRatio = 0.8f, stiffness = 450f)) + fadeOut(tween(150))

/**
 * Fades the content in while it rises [rise] and grows from [fromScale], once.
 * It doesn't replay after a rotation or when a tab comes back.
 */
@Composable
fun Modifier.riseIn(fromScale: Float = 0.96f, rise: Dp = 12.dp): Modifier {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 380f))
            played = true
        }
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * rise.toPx()
        val scale = fromScale + (1f - fromScale) * p
        scaleX = scale
        scaleY = scale
    }
}

/** Degrees for an expand arrow: it turns over instead of swapping to a second icon. */
@Composable
fun expandArrowRotation(expanded: Boolean): Float {
    val turn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "expand_arrow"
    )
    return turn
}
