package codes.tashif.paisa.ui.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

actual val dynamicColorSupported: Boolean = false

@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

@Composable
actual fun isImeVisible(): Boolean = false

/**
 * The browser's back button and Escape both act as back. Each enabled
 * handler pushes a history entry, so back pops it instead of leaving Paisa.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val current = rememberUpdatedState(onBack)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val token = BrowserBack.push { current.value() }
        onDispose { BrowserBack.remove(token) }
    }
}
