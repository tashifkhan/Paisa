package codes.tashif.paisa.ui.platform

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
private fun scrollbarStyle() = LocalScrollbarStyle.current.copy(
    thickness = 8.dp,
    shape = RoundedCornerShape(4.dp),
    unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
    hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
)

@Composable
actual fun VerticalScrollbarFor(state: LazyListState, modifier: Modifier) =
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier, style = scrollbarStyle())

@Composable
actual fun VerticalScrollbarFor(state: LazyGridState, modifier: Modifier) =
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier, style = scrollbarStyle())

@Composable
actual fun VerticalScrollbarFor(state: ScrollState, modifier: Modifier) =
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier, style = scrollbarStyle())

actual fun setWindowTitle(title: String) = jsSetTitle(title)

private fun jsSetTitle(title: String): Unit = js("document.title = title")

@Composable
actual fun PageKeys(onKey: (key: String, command: Boolean) -> Boolean) {
    val current = rememberUpdatedState(onKey)
    DisposableEffect(Unit) {
        val id = jsAddPageKeys { key, command -> current.value(key, command) }
        onDispose { jsRemovePageKeys(id) }
    }
}

// Only keys aimed at the page body: once the canvas or a text field has
// focus, Compose gets the key itself and this stays out of the way.
private fun jsAddPageKeys(callback: (String, Boolean) -> Boolean): Int = js("""{
    const handler = (e) => {
        // The canvas and text inputs sit in the body's shadow root, so the
        // real target, not activeElement, tells whether Compose has the key.
        const target = e.composedPath ? e.composedPath()[0] : e.target;
        if (target !== document.body && target !== document.documentElement) return;
        if (e.altKey) return;
        if (callback(e.key, e.ctrlKey || e.metaKey)) e.preventDefault();
    };
    window.addEventListener('keydown', handler);
    window.__paisaKeys = window.__paisaKeys || [];
    window.__paisaKeys.push(handler);
    return window.__paisaKeys.length - 1;
}""")

private fun jsRemovePageKeys(id: Int): Unit = js("""{
    const h = window.__paisaKeys && window.__paisaKeys[id];
    if (h) { window.removeEventListener('keydown', h); window.__paisaKeys[id] = null; }
}""")
