package codes.tashif.paisa.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import codes.tashif.paisa.ui.theme.PaisaPalette
import codes.tashif.paisa.ui.theme.colorScheme

/**
 * Glance colors mirroring the in-app [codes.tashif.paisa.ui.theme.PaisaTheme].
 *
 * Widgets can't observe the app's theme composition, so the palette id is read
 * from Settings at load time and expanded here into the same Material 3 tonal
 * schemes the app uses. "dynamic" defers to the launcher's wallpaper colors on
 * Android 12+, which is what a home screen widget should follow anyway.
 */
internal fun paisaWidgetColors(
    context: Context,
    palette: PaisaPalette
): androidx.glance.color.ColorProviders {
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return if (palette == PaisaPalette.Dynamic && dynamicAvailable) {
        ColorProviders(
            light = dynamicLightColorScheme(context),
            dark = dynamicDarkColorScheme(context)
        )
    } else {
        ColorProviders(
            light = palette.colorScheme(darkTheme = false),
            dark = palette.colorScheme(darkTheme = true)
        )
    }
}

/** Wraps [content] in the app's palette. Glance picks light/dark by system config. */
@androidx.compose.runtime.Composable
internal fun PaisaGlanceTheme(
    context: Context,
    palette: PaisaPalette,
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    GlanceTheme(colors = paisaWidgetColors(context, palette), content = content)
}
