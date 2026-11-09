package codes.tashif.paisa.ui.platform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

/** A draggable scrollbar for [state] on the web. Nothing on Android, which shows its own while scrolling. */
@Composable
expect fun VerticalScrollbarFor(state: LazyListState, modifier: Modifier = Modifier)

@Composable
expect fun VerticalScrollbarFor(state: LazyGridState, modifier: Modifier = Modifier)

@Composable
expect fun VerticalScrollbarFor(state: ScrollState, modifier: Modifier = Modifier)

/**
 * Keys pressed while the page itself has focus and the app's canvas doesn't,
 * as on first load before any click. [onKey] gets the key's name ("n", "2",
 * "/", "?") and whether Ctrl or Cmd was held, and returns true to use it.
 * Nothing on Android.
 */
@Composable
expect fun PageKeys(onKey: (key: String, command: Boolean) -> Boolean)

/** Sets the browser tab's title. Nothing on Android. */
expect fun setWindowTitle(title: String)

/** The hand cursor over things you can click with a mouse. */
fun Modifier.handCursor(): Modifier = pointerHoverIcon(PointerIcon.Hand)

/**
 * Calls [onClick] on a right click (or a two-finger trackpad click) with
 * where it happened, for a context menu. Touch never triggers it.
 */
fun Modifier.onSecondaryClick(onClick: (Offset) -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.forEach { it.consume() }
                onClick(event.changes.first().position)
            }
        }
    }
}

/**
 * Tracks a mouse moving over a chart: [onHover] gets the index under the
 * pointer from [indexAt], and null when it leaves. Touch keeps using taps.
 */
fun Modifier.hoverIndex(
    key: Any?,
    indexAt: (Offset, IntSize) -> Int?,
    onHover: (Int?) -> Unit
): Modifier = pointerInput(key) {
    awaitPointerEventScope {
        var last: Int? = null
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: continue
            if (change.type != PointerType.Mouse) continue
            val next = when (event.type) {
                PointerEventType.Move, PointerEventType.Enter -> indexAt(change.position, size)
                PointerEventType.Exit -> null
                else -> continue
            }
            if (next != last) {
                last = next
                onHover(next)
            }
        }
    }
}
