package codes.tashif.paisa.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import java.util.concurrent.ConcurrentHashMap

/**
 * Material 3 Expressive shapes for the widgets.
 *
 * Glance only rounds rectangle corners, so real expressive shapes (scalloped
 * cookies, clovers, sunbursts) are rendered from [MaterialShapes] polygons into
 * white bitmaps here, then tinted per widget via a Glance `ColorFilter`. White
 * fill keeps them theme-agnostic — the tint carries light/dark and the palette.
 */
enum class WidgetShape {
    Cookie,
    Clover,
    Sunny,
    Burst,
    Flower,
    Squircle
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun polygonFor(shape: WidgetShape): RoundedPolygon = when (shape) {
    WidgetShape.Cookie -> MaterialShapes.Cookie9Sided
    WidgetShape.Clover -> MaterialShapes.Clover4Leaf
    WidgetShape.Sunny -> MaterialShapes.Sunny
    WidgetShape.Burst -> MaterialShapes.SoftBurst
    WidgetShape.Flower -> MaterialShapes.Flower
    WidgetShape.Squircle -> MaterialShapes.Square
}

// Bitmaps are immutable once drawn and keyed by shape+size, so caching them
// keeps recompositions (and multiple widget instances) from re-rasterizing.
private val cache = ConcurrentHashMap<String, Bitmap>()

/**
 * A [shape] rasterized to a white bitmap of [widthPx]×[heightPx], centered and
 * scaled to fill. Intended to sit behind widget content, tinted at draw time.
 */
fun shapeBitmap(shape: WidgetShape, widthPx: Int, heightPx: Int): Bitmap {
    val w = widthPx.coerceAtLeast(1)
    val h = heightPx.coerceAtLeast(1)
    val key = "${shape.name}_${w}x$h"
    cache[key]?.let { return it }

    val path = polygonFor(shape).toPath()
    val bounds = RectF()
    path.computeBounds(bounds, true)

    // Normalize the polygon's own coordinate space onto the bitmap. A tiny inset
    // keeps antialiased edges (scallop tips) from clipping at the border.
    val inset = 1f
    val matrix = Matrix().apply {
        postTranslate(-bounds.left, -bounds.top)
        postScale((w - inset * 2) / bounds.width(), (h - inset * 2) / bounds.height())
        postTranslate(inset, inset)
    }
    path.transform(matrix)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawPath(
        path,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
    )
    cache[key] = bitmap
    return bitmap
}

/**
 * Privacy mask matching the in-app [codes.tashif.paisa.ui.components.HiddenMoneyMask]:
 * a row of mixed shapes at stepped alphas, drawn white so Glance can tint it.
 */
fun maskedMoneyBitmap(
    density: Float,
    count: Int = 6,
    pipDp: Float = 10f
): Bitmap {
    val pip = (pipDp * density).toInt().coerceAtLeast(6)
    val gap = (pip * 0.45f).toInt().coerceAtLeast(2)
    val w = count * pip + (count - 1) * gap
    val h = (pip * 1.15f).toInt()
    val key = "mask_${count}_${pip}x$h"
    cache[key]?.let { return it }

    val bitmap = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // Shape cycle echoes HiddenMoneyMask: circle, squircle, diamond-ish, stadium, etc.
    repeat(count) { index ->
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
        val scale = when (index % 4) {
            0 -> 1f
            1 -> 0.85f
            2 -> 1.1f
            else -> 0.92f
        }
        val size = (pip * scale).toInt().coerceAtLeast(4)
        val left = index * (pip + gap) + (pip - size) / 2f
        val top = (h - size) / 2f
        paint.color = Color.argb((alpha * 255).toInt(), 255, 255, 255)

        when (index % 6) {
            0 -> canvas.drawCircle(left + size / 2f, top + size / 2f, size / 2f, paint)
            1 -> canvas.drawRoundRect(RectF(left, top, left + size, top + size), size * 0.2f, size * 0.2f, paint)
            2 -> canvas.drawRoundRect(RectF(left, top, left + size, top + size), size * 0.45f, size * 0.45f, paint)
            3 -> {
                // Soft diamond via rotated round-rect feel — just a tall stadium.
                canvas.drawRoundRect(
                    RectF(left + size * 0.15f, top, left + size * 0.85f, top + size),
                    size.toFloat(),
                    size.toFloat(),
                    paint
                )
            }
            4 -> canvas.drawRoundRect(
                RectF(left, top + size * 0.15f, left + size, top + size * 0.85f),
                size * 0.5f,
                size * 0.5f,
                paint
            )
            else -> canvas.drawRoundRect(RectF(left, top, left + size, top + size), size * 0.35f, size * 0.35f, paint)
        }
    }

    cache[key] = bitmap
    return bitmap
}
