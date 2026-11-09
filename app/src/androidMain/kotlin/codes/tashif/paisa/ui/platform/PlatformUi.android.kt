package codes.tashif.paisa.ui.platform

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual val isWebPlatform: Boolean = false

actual val dynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (!dynamicColorSupported) return null
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun isImeVisible(): Boolean = WindowInsets.isImeVisible

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)

@Composable
actual fun dynamicAccentColor(): androidx.compose.ui.graphics.Color? =
    if (dynamicColorSupported) {
        androidx.compose.ui.res.colorResource(android.R.color.system_accent1_400)
    } else {
        null
    }
