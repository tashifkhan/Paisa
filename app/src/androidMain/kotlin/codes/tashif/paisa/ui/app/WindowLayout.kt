package codes.tashif.paisa.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Material window width classes. Compact is a phone in portrait. Medium is an
 * unfolded book-style foldable, a small tablet in portrait or a phone in
 * landscape. Expanded is a tablet in landscape or an unfolded foldable turned
 * sideways.
 */
enum class WindowWidth { COMPACT, MEDIUM, EXPANDED }

/**
 * The width class of this window, not the whole display, so split screen and
 * freeform windows get the layout that fits them. The activity handles size
 * changes itself, so this recomposes when a foldable opens or closes.
 */
@Composable
fun windowWidth(): WindowWidth {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> WindowWidth.COMPACT
        widthDp < 840 -> WindowWidth.MEDIUM
        else -> WindowWidth.EXPANDED
    }
}

/** Widest a single column of cards and lists gets before it centers. */
val MaxContentWidth = 840.dp

/**
 * Centers [content] and caps it at [MaxContentWidth], so a list on a tablet
 * doesn't stretch cards across the whole screen. On a phone it does nothing.
 */
@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxSize(),
            content = content
        )
    }
}
