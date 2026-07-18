package codes.tashif.paisa.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * App theme wrapper.
 *
 * Important: always call a single [MaterialTheme] composable (with different
 * [MotionScheme] / contrast) so toggling expressive does **not** dispose the
 * composition tree — which would wipe navigation state like the Appearance screen.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PaisaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: PaisaPalette = PaisaPalette.Dynamic,
    expressive: Boolean = true,
    amoledDark: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = if (
        palette == PaisaPalette.Dynamic &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        palette.colorScheme(darkTheme)
    }
    val colorScheme = baseScheme
        .let { if (darkTheme && amoledDark) it.toAmoled() else it }
        .let { if (highContrast) it.withHighContrast(darkTheme) else it }

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            // Same MaterialTheme call site for both modes — only motion changes.
            motionScheme = if (expressive) {
                MotionScheme.expressive()
            } else {
                MotionScheme.standard()
            },
            typography = Typography,
            content = content
        )
    }
}

/** Stronger text / outline contrast for accessibility or “flat” look preference. */
fun androidx.compose.material3.ColorScheme.withHighContrast(
    darkTheme: Boolean
): androidx.compose.material3.ColorScheme {
    return if (darkTheme) {
        copy(
            onSurface = Color(0xFFFFFFFF),
            onBackground = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFFE8E0EC),
            outline = Color(0xFFD0C4D8),
            outlineVariant = Color(0xFF9A8FA3),
            surfaceVariant = surfaceVariant.copy(
                red = (surfaceVariant.red * 0.85f + 0.08f).coerceIn(0f, 1f),
                green = (surfaceVariant.green * 0.85f + 0.08f).coerceIn(0f, 1f),
                blue = (surfaceVariant.blue * 0.85f + 0.08f).coerceIn(0f, 1f)
            )
        )
    } else {
        copy(
            onSurface = Color(0xFF000000),
            onBackground = Color(0xFF000000),
            onSurfaceVariant = Color(0xFF2C2C2C),
            outline = Color(0xFF424242),
            outlineVariant = Color(0xFF6B6B6B)
        )
    }
}
