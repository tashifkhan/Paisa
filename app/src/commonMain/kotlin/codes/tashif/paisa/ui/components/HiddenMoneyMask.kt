package codes.tashif.paisa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.format.formatMoneyAmount
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedContent

/**
 * Material You–style privacy mask: each “dot” is a different shape and a
 * different shade of a single base color (monochrome, multi-shape).
 */
@Composable
fun HiddenMoneyMask(
    modifier: Modifier = Modifier,
    count: Int = 6,
    /** Visual weight — scales with surrounding text when null. */
    size: Dp? = null,
    gap: Dp? = null,
    /** Base ink; each pip uses a different alpha of this color. */
    baseColor: Color? = LocalContentColor.current
) {
    val density = LocalDensity.current
    val textSize = LocalTextStyle.current.fontSize
    val resolvedSize = size ?: with(density) {
        (textSize.value * 0.55f).coerceIn(6f, 14f).dp
    }
    val resolvedGap = gap ?: (resolvedSize * 0.45f)
    val base = baseColor ?: MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(resolvedGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val shape = maskShape(index)
            // Monochrome: same hue, stepped alphas so neighbors read as different shades
            val alpha = when (index % 8) {
                0 -> 0.95f
                1 -> 0.48f
                2 -> 0.78f
                3 -> 0.38f
                4 -> 0.88f
                5 -> 0.55f
                6 -> 0.70f
                else -> 0.42f
            }
            // Slight size jitter for more expressive rhythm
            val scale = when (index % 4) {
                0 -> 1f
                1 -> 0.85f
                2 -> 1.1f
                else -> 0.92f
            }
            Box(
                modifier = Modifier
                    .size(resolvedSize * scale)
                    .clip(shape)
                    .background(base.copy(alpha = alpha))
            )
        }
    }
}

/** Cycles distinct M3-inspired corner recipes so every pip looks unique. */
private fun maskShape(index: Int): Shape = when (index % 8) {
    0 -> CircleShape
    1 -> RoundedCornerShape(
        topStart = 10.dp,
        topEnd = 3.dp,
        bottomStart = 3.dp,
        bottomEnd = 10.dp
    )
    2 -> RoundedCornerShape(3.dp)
    3 -> RoundedCornerShape(
        topStart = 2.dp,
        topEnd = 10.dp,
        bottomStart = 10.dp,
        bottomEnd = 2.dp
    )
    4 -> RoundedCornerShape(
        topStart = 10.dp,
        topEnd = 10.dp,
        bottomStart = 3.dp,
        bottomEnd = 3.dp
    )
    5 -> RoundedCornerShape(
        topStart = 3.dp,
        topEnd = 12.dp,
        bottomStart = 12.dp,
        bottomEnd = 3.dp
    )
    6 -> RoundedCornerShape(50) // stadium / full
    else -> RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 2.dp,
        bottomStart = 2.dp,
        bottomEnd = 12.dp
    )
}

/**
 * Shows formatted money or a [HiddenMoneyMask] when [hidden] is true.
 * Optional [prefix] (e.g. "+", "−") only appears when visible.
 */
@Composable
fun MoneyText(
    currency: String,
    amount: Double,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    prefix: String = "",
    maskCount: Int = 6,
    /**
     * Roll to a new amount instead of jumping, and swap softly between the
     * number and its mask. For the few headline numbers, not for list rows.
     */
    animated: Boolean = false
) {
    if (animated) {
        // Hidden amounts all look alike, so a change behind the mask doesn't roll.
        val shown = if (hidden) RolledAmount(null, 0.0) else {
            RolledAmount(prefix + formatMoneyAmount(currency, amount), amount)
        }
        AnimatedContent(
            targetState = shown,
            modifier = modifier,
            transitionSpec = {
                if ((targetState.text == null) != (initialState.text == null)) {
                    (fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.78f, stiffness = 380f), initialScale = 0.92f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.95f)) using
                        SizeTransform(clip = false)
                } else {
                    // Like a counter wheel: up for more, down for less.
                    val up = targetState.amount >= initialState.amount
                    (slideInVertically(spring(dampingRatio = 0.58f, stiffness = 430f)) { if (up) it / 2 else -it / 2 } +
                        scaleIn(spring(dampingRatio = 0.62f, stiffness = 430f), initialScale = 0.92f) +
                        fadeIn(tween(85))) togetherWith
                        (slideOutVertically(spring(dampingRatio = 0.78f, stiffness = 520f)) { if (up) -it / 2 else it / 2 } +
                            scaleOut(tween(120), targetScale = 0.96f) +
                            fadeOut(tween(75))) using
                        SizeTransform(clip = false)
                }
            },
            contentAlignment = Alignment.CenterStart,
            label = "money"
        ) { state ->
            MoneyText(
                currency = currency,
                amount = state.amount,
                hidden = state.text == null,
                style = style,
                color = color,
                fontWeight = fontWeight,
                prefix = prefix,
                maskCount = maskCount
            )
        }
        return
    }
    if (hidden) {
        val density = LocalDensity.current
        // Scale mask pips from the *requested* text style so a displaySmall
        // amount still looks like a big number when privacy is on.
        val pipSize = with(density) {
            (style.fontSize.toPx() * 0.42f).coerceIn(8f, 28f).toDp()
        }
        HiddenMoneyMask(
            modifier = modifier,
            count = maskCount,
            size = pipSize,
            baseColor = if (color != Color.Unspecified) {
                color
            } else {
                LocalContentColor.current
            }
        )
    } else {
        Text(
            text = prefix + formatMoneyAmount(currency, amount),
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight
        )
    }
}

/** What an animated [MoneyText] shows: the formatted amount, or null [text] for the mask. */
private data class RolledAmount(val text: String?, val amount: Double)
