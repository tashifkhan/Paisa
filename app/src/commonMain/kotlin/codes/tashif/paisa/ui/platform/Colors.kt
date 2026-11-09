package codes.tashif.paisa.ui.platform

import androidx.compose.ui.graphics.Color

private val NAMED = mapOf(
    "black" to 0xFF000000, "white" to 0xFFFFFFFF, "red" to 0xFFFF0000, "green" to 0xFF00FF00,
    "blue" to 0xFF0000FF, "yellow" to 0xFFFFFF00, "cyan" to 0xFF00FFFF, "magenta" to 0xFFFF00FF,
    "gray" to 0xFF888888, "grey" to 0xFF888888, "lightgray" to 0xFFCCCCCC, "lightgrey" to 0xFFCCCCCC,
    "darkgray" to 0xFF444444, "darkgrey" to 0xFF444444, "aqua" to 0xFF00FFFF, "fuchsia" to 0xFFFF00FF,
    "lime" to 0xFF00FF00, "maroon" to 0xFF800000, "navy" to 0xFF000080, "olive" to 0xFF808000,
    "purple" to 0xFF800080, "silver" to 0xFFC0C0C0, "teal" to 0xFF008080
)

/**
 * `android.graphics.Color.parseColor`: `#RRGGBB`, `#AARRGGBB` or a few names.
 * Throws [IllegalArgumentException] for anything else, as parseColor does.
 */
fun parseColor(text: String): Color {
    if (text.startsWith("#")) {
        val hex = text.substring(1)
        val value = hex.toLongOrNull(16) ?: throw IllegalArgumentException("Unknown color")
        return when (hex.length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> throw IllegalArgumentException("Unknown color")
        }
    }
    return NAMED[text.lowercase()]?.let { Color(it) } ?: throw IllegalArgumentException("Unknown color")
}
