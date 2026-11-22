package sh.taf.billi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Billi, the cat who is also a wallet. Every mood shares the logo's ears,
 * whiskers and wallet body and changes the eyes, plus a prop for some.
 */
enum class BilliMood {
    /** The logo: eyes closed in a smile. Welcome, About. */
    Happy,

    /** One eye open. Setup is done, something saved. */
    Wink,

    /** Both eyes open. Reading SMS and notifications, new things to look at. */
    Curious,

    /** Eyes shut, a couple of z's. Empty lists. */
    Sleepy,

    /** Looking up at a sparkle. On-device and cloud AI at work. */
    Thinking,

    /** A padlock on the wallet. App lock and privacy. */
    Guarding
}

/** Fur and ink for the mascot. Ink turns light on dark backgrounds so the face stays visible. */
private object BilliColors {
    val Ear = Color(0xFFE88C2E)
    val Body = Color(0xFFDC781B)
    val InkOnLight = Color(0xFF5E2F0E)
    val InkOnDark = Color(0xFFF6DCC4)
    val Spark = Color(0xFFF2B431)
}

/**
 * Draws Billi in [mood]. With [animate], Billi bobs gently (and breathes when
 * sleepy). [contentDescription] is null for decoration next to a heading that
 * already says the same thing.
 */
@Composable
fun BilliMascot(
    mood: BilliMood,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    animate: Boolean = false,
    contentDescription: String? = null
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val ink = if (dark) BilliColors.InkOnDark else BilliColors.InkOnLight
    val image = remember(mood, ink) { billiVector(mood, ink) }

    val motion = if (animate) {
        val transition = rememberInfiniteTransition(label = "billi")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(if (mood == BilliMood.Sleepy) 2400 else 1800, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "billi_bob"
        )
    } else {
        null
    }

    Image(
        imageVector = image,
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val p = motion?.value ?: return@graphicsLayer
                if (mood == BilliMood.Sleepy) {
                    // Breathing: the wallet swells a little, anchored at the bottom.
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    scaleX = 1f + 0.02f * p
                    scaleY = 1f + 0.035f * p
                } else {
                    translationY = -size.toPx() * 0.035f * p
                }
            }
    )
}

// ── Geometry, in the logo's 240×240 space ──────────────────────────────────

private const val EARS =
    "M 36 97 C 36 66, 44 33, 62 31 C 78 30, 92 43, 102 55 Q 120 49, 138 55 " +
        "C 148 43, 162 30, 178 31 C 196 33, 204 66, 204 97 C 204 101, 192 103, 190 98 " +
        "C 190 72, 183 49, 172 48 C 162 47, 151 58, 142 70 Q 120 63, 98 70 " +
        "C 89 58, 78 47, 68 48 C 57 49, 50 72, 50 98 C 48 103, 36 101, 36 97 Z"

private const val BODY =
    "M 42 150 Q 42 137.2, 56 136 L 170 126 C 184 125, 194 135, 194 150 L 155 150 " +
        "A 22 22 0 0 0 155 194 L 193 194 C 190 204, 183 210, 174 210 L 70 210 " +
        "A 28 28 0 0 1 42 182 Z"

private const val CLASP =
    "M 155 158 L 194 158 A 4 4 0 0 1 198 162 L 198 182 A 4 4 0 0 1 194 186 L 155 186 " +
        "A 14 14 0 0 1 155 158 Z M 155 165.5 A 6.5 6.5 0 1 0 155 178.5 A 6.5 6.5 0 1 0 155 165.5 Z"

private val WHISKERS = listOf(
    "M 20 110 Q 33 111, 46 115",
    "M 20 131 Q 32.5 127.5, 45 126",
    "M 220 110 Q 207 111, 194 115",
    "M 220 131 Q 207.5 127.5, 195 126"
)

private const val EYE_LEFT_CLOSED = "M 74 111 C 77 97, 97 97, 100 111"
private const val EYE_RIGHT_CLOSED = "M 140 111 C 143 97, 163 97, 166 111"
private const val EYE_LEFT_ASLEEP = "M 74 104 C 78 113, 96 113, 100 104"
private const val EYE_RIGHT_ASLEEP = "M 140 104 C 144 113, 162 113, 166 104"

private fun circle(cx: Float, cy: Float, r: Float) =
    "M ${cx - r} $cy A $r $r 0 1 0 ${cx + r} $cy A $r $r 0 1 0 ${cx - r} $cy Z"

/** A four-point sparkle centered at [cx], [cy]. */
private fun sparkle(cx: Float, cy: Float, r: Float): String {
    val k = r * 0.16f
    return "M $cx ${cy - r} Q ${cx + k} ${cy - k} ${cx + r} $cy Q ${cx + k} ${cy + k} $cx ${cy + r} " +
        "Q ${cx - k} ${cy + k} ${cx - r} $cy Q ${cx - k} ${cy - k} $cx ${cy - r} Z"
}

private fun billiVector(mood: BilliMood, ink: Color): ImageVector =
    ImageVector.Builder(
        name = "Billi$mood",
        defaultWidth = 240.dp,
        defaultHeight = 240.dp,
        viewportWidth = 240f,
        viewportHeight = 240f
    ).apply {
        fun fill(d: String, color: Color, evenOdd: Boolean = false) = addPath(
            pathData = addPathNodes(d),
            fill = SolidColor(color),
            pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
        )

        fun stroke(d: String, width: Float, color: Color = ink) = addPath(
            pathData = addPathNodes(d),
            stroke = SolidColor(color),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )

        fun openEyes(dx: Float, dy: Float) {
            fill(circle(87f + dx, 105f + dy, 9f), ink)
            fill(circle(153f + dx, 105f + dy, 9f), ink)
            // Catchlights, so the open eyes read as eyes at small sizes.
            val glint = if (ink == BilliColors.InkOnLight) Color.White else BilliColors.InkOnLight
            fill(circle(90.5f + dx, 101.5f + dy, 2.8f), glint)
            fill(circle(156.5f + dx, 101.5f + dy, 2.8f), glint)
        }

        fill(EARS, BilliColors.Ear)
        WHISKERS.forEach { stroke(it, 6.5f) }
        fill(BODY, BilliColors.Body)
        fill(CLASP, BilliColors.Body, evenOdd = true)

        when (mood) {
            BilliMood.Happy -> {
                stroke(EYE_LEFT_CLOSED, 7.5f)
                stroke(EYE_RIGHT_CLOSED, 7.5f)
            }
            BilliMood.Wink -> {
                stroke(EYE_LEFT_CLOSED, 7.5f)
                fill(circle(153f, 105f, 9f), ink)
                fill(
                    circle(156.5f, 101.5f, 2.8f),
                    if (ink == BilliColors.InkOnLight) Color.White else BilliColors.InkOnLight
                )
                fill(sparkle(214f, 36f, 13f), BilliColors.Spark)
                fill(sparkle(28f, 52f, 8f), BilliColors.Spark)
            }
            BilliMood.Curious -> openEyes(0f, 0f)
            BilliMood.Sleepy -> {
                stroke(EYE_LEFT_ASLEEP, 7.5f)
                stroke(EYE_RIGHT_ASLEEP, 7.5f)
                stroke("M 196 12 L 212 12 L 196 28 L 212 28", 5f)
                stroke("M 219 2 L 229 2 L 219 12 L 229 12", 4f)
            }
            BilliMood.Thinking -> {
                openEyes(4f, -4f)
                fill(sparkle(220f, 36f, 14f), BilliColors.Spark)
                fill(sparkle(200f, 10f, 8f), BilliColors.Spark)
            }
            BilliMood.Guarding -> {
                openEyes(0f, 0f)
                // Padlock on the front of the wallet, in ink so it reads on the orange.
                stroke("M 102 162 L 102 152 A 8 8 0 0 1 118 152 L 118 162", 4.5f)
                fill("M 99 160 L 121 160 A 3 3 0 0 1 124 163 L 124 183 A 3 3 0 0 1 121 186 L 99 186 A 3 3 0 0 1 96 183 L 96 163 A 3 3 0 0 1 99 160 Z", ink)
                fill(circle(110f, 171f, 3.5f), BilliColors.Body)
                fill("M 108.5 172 L 111.5 172 L 112 179 L 108 179 Z", BilliColors.Body)
            }
        }
    }.build()
