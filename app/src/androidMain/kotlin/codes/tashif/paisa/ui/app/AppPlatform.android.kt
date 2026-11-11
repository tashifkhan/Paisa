package codes.tashif.paisa.ui.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import codes.tashif.paisa.R
import codes.tashif.paisa.widget.PaisaWidgets

class AndroidAppPlatform(private val context: Context) : AppPlatform {
    override val versionName: String by lazy {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    override suspend fun refreshWidgets() = PaisaWidgets.refresh(context)
}

@Composable
actual fun windowWidthDp(): Int = LocalConfiguration.current.screenWidthDp

@Composable
actual fun appIconPainter(): Painter = painterResource(R.drawable.ic_launcher_legacy)
