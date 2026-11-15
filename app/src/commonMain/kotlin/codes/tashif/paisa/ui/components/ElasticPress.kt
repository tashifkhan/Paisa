package codes.tashif.paisa.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/*
 * The press response below is adapted from `luminousElasticClickable` in
 * Expressive Clock (https://github.com/samuelbeckman/expressive-clock,
 * ui/Glass.kt), MIT License, Copyright (c) 2026 Rayane Bouafia. It is split
 * in two here so a shadow can sit between the part that moves the control
 * and the part that draws light inside its clipped shape. See COPYRIGHT.
 */

/** Where a finger is on a control, shared by [elasticPress] and [pressGlow]. */
@Stable
class ElasticPressState {
    /** Finger position while it is down, in the control's own pixels. */
    var touch by mutableStateOf<Offset?>(null)
        internal set

    /** Where the finger was last, so the light fades where it left. */
    internal var lastTouch by mutableStateOf<Offset?>(null)
    internal var rippleOrigin by mutableStateOf<Offset?>(null)
    internal val ripple = Animatable(1f)

    /** Finger offset from the centre, -1 to 1 on each axis. Zero when lifted. */
    internal var dx by mutableFloatStateOf(0f)
    internal var dy by mutableFloatStateOf(0f)

    val pressed: Boolean get() = touch != null
}

@Composable
fun rememberElasticPress(): ElasticPressState = remember { ElasticPressState() }

/**
 * Makes a control give under a finger. It shrinks to [pressedScale], stretches
 * up to [maxStretch] toward the touch, and shifts up to [maxTranslate] the
 * same way. On release it springs back with a small overshoot.
 *
 * It only watches touches, so buttons inside still get their clicks. It
 * doesn't clip: put a shadow after it and the shadow moves with the control.
 * Put it before `clickable` on the same element.
 */
@Composable
fun Modifier.elasticPress(
    state: ElasticPressState,
    maxStretch: Float = 0.06f,
    maxTranslate: Dp = 1.8.dp,
    pressedScale: Float = 0.95f,
    /** Let go once the finger starts dragging, so a card doesn't stay squashed while its list scrolls. */
    releaseOnDrag: Boolean = false
): Modifier {
    val scope = rememberCoroutineScope()
    val pressed = state.pressed
    // Firm while held, loose on the way back.
    val follow = if (pressed) {
        spring<Float>(dampingRatio = 0.82f, stiffness = 550f)
    } else {
        spring(dampingRatio = 0.52f, stiffness = 350f)
    }
    val dx by animateFloatAsState(state.dx, follow, label = "elastic_dx")
    val dy by animateFloatAsState(state.dy, follow, label = "elastic_dy")
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) {
            tween(durationMillis = 90, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.62f, stiffness = 420f)
        },
        label = "elastic_scale"
    )

    return this
        .pointerInput(state, releaseOnDrag) {
            awaitPointerEventScope {
                var down: Offset? = null
                var dragging = false
                while (true) {
                    // The initial pass, without consuming: children handle the tap.
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()
                        ?: continue
                    if (!change.pressed) {
                        down = null
                        dragging = false
                        state.touch = null
                        state.dx = 0f
                        state.dy = 0f
                        continue
                    }
                    if (dragging) continue
                    val position = change.position
                    val start = down ?: position.also { down = it }
                    if (releaseOnDrag && (position - start).getDistance() > viewConfiguration.touchSlop) {
                        dragging = true
                        state.touch = null
                        state.dx = 0f
                        state.dy = 0f
                        continue
                    }
                    if (state.touch == null) {
                        state.rippleOrigin = position
                        scope.launch {
                            state.ripple.snapTo(0f)
                            state.ripple.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                        }
                    }
                    state.touch = position
                    state.lastTouch = position
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    if (cx > 0f && cy > 0f) {
                        state.dx = ((position.x - cx) / cx).coerceIn(-1f, 1f)
                        state.dy = ((position.y - cy) / cy).coerceIn(-1f, 1f)
                    }
                }
            }
        }
        .graphicsLayer {
            // Stretching one way thins the other a little, like something soft.
            val stretchX = 1f + maxStretch * abs(dx) - 0.03f * abs(dy)
            val stretchY = 1f + maxStretch * 1.06f * abs(dy) - 0.03f * abs(dx)
            // Pivot on the far side, so only the side under the finger moves out.
            transformOrigin = TransformOrigin(
                (0.5f - 0.3f * dx).coerceIn(0.15f, 0.85f),
                (0.5f - 0.3f * dy).coerceIn(0.15f, 0.85f)
            )
            scaleX = scale * stretchX
            scaleY = scale * stretchY
            translationX = dx * maxTranslate.toPx()
            translationY = dy * maxTranslate.toPx()
        }
}

/**
 * Draws a soft light over the content that follows the finger tracked by
 * [state], and a ring that spreads from where it first touched. Goes inside
 * the control's clip, on something the same size as what [elasticPress] is on.
 */
@Composable
fun Modifier.pressGlow(state: ElasticPressState): Modifier {
    val pressed = state.pressed
    val glow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(if (pressed) 120 else 260, easing = FastOutSlowInEasing),
        label = "press_glow"
    )
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    // White light on dark themes, the accent on light ones, where white wouldn't show.
    val centre = if (dark) Color.White.copy(alpha = 0.2f) else colors.primary.copy(alpha = 0.2f)
    val edge = colors.primary.copy(alpha = if (dark) 0.14f else 0.08f)

    return drawWithContent {
        drawContent()
        if (glow > 0.01f) {
            val at = state.touch ?: state.lastTouch ?: Offset(size.width / 2f, size.height / 2f)
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        centre.copy(alpha = centre.alpha * glow),
                        edge.copy(alpha = edge.alpha * glow),
                        Color.Transparent
                    ),
                    center = at,
                    radius = size.minDimension * 2.2f
                )
            )
        }
        val spread = state.ripple.value
        val origin = state.rippleOrigin
        val rippleAlpha = (1f - spread) * (if (pressed) 1f else glow)
        if (origin != null && spread < 1f && rippleAlpha > 0.01f) {
            val smallest = 12.dp.toPx()
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        centre.copy(alpha = centre.alpha * 0.85f * rippleAlpha),
                        edge.copy(alpha = edge.alpha * 0.45f * rippleAlpha),
                        Color.Transparent
                    ),
                    center = Offset(origin.x.coerceIn(0f, size.width), origin.y.coerceIn(0f, size.height)),
                    radius = smallest + (size.maxDimension * 0.9f - smallest) * spread
                )
            )
        }
    }
}

/** [elasticPress] tuned for a card as wide as the screen: a small dip, barely any lean. */
@Composable
fun Modifier.elasticCard(): Modifier = elasticPress(
    state = rememberElasticPress(),
    maxStretch = 0.012f,
    maxTranslate = 1.dp,
    pressedScale = 0.98f,
    releaseOnDrag = true
)

/** [elasticPress] tuned for a small round button, which can move more before it looks wrong. */
@Composable
fun Modifier.elasticButton(): Modifier = elasticPress(
    state = rememberElasticPress(),
    maxStretch = 0.08f,
    maxTranslate = 2.dp,
    pressedScale = 0.94f,
    releaseOnDrag = true
)
