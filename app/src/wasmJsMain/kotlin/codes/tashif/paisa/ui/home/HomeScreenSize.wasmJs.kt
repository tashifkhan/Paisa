package codes.tashif.paisa.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/** The browser viewport's height, converted from pixels to dp. */
@Composable
internal actual fun screenHeightDp(): Int {
    val px = LocalWindowInfo.current.containerSize.height
    return (px / LocalDensity.current.density).toInt()
}
