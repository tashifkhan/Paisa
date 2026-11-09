package codes.tashif.paisa.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.painterResource
import codes.tashif.paisa.resources.Res
import codes.tashif.paisa.resources.ic_launcher_legacy

class WebAppPlatform(override val versionName: String) : AppPlatform {
    override suspend fun refreshWidgets() = Unit
}

@Composable
actual fun windowWidthDp(): Int {
    val px = LocalWindowInfo.current.containerSize.width
    return (px / LocalDensity.current.density).toInt()
}

@Composable
actual fun appIconPainter(): Painter = painterResource(Res.drawable.ic_launcher_legacy)
