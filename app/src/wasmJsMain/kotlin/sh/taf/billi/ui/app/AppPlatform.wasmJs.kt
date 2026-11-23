package sh.taf.billi.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.painterResource
import sh.taf.billi.resources.Res
import sh.taf.billi.resources.ic_launcher_legacy

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

/** billi.taf.sh/'s landing page sends people to /app/ when this is "1". */
actual fun rememberSetupDone(done: Boolean) = jsSetSetupDone(if (done) "1" else "0")

private fun jsSetSetupDone(value: String): Unit =
    js("{ try { localStorage.setItem('billi.web.used', value); } catch (e) {} }")
