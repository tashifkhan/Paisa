package codes.tashif.paisa.ui.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** True where the system offers Material You colors (Android 12 and later). */
expect val dynamicColorSupported: Boolean

/** The system's Material You scheme, or null where there is none. */
@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** Whether the on-screen keyboard is showing. */
@Composable
expect fun isImeVisible(): Boolean

/** Handles the system back gesture while [enabled]. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
