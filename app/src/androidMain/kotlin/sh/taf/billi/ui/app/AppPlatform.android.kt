package sh.taf.billi.ui.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import sh.taf.billi.R
import sh.taf.billi.share.ShareActivity
import sh.taf.billi.widget.BilliWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidAppPlatform(private val context: Context) : AppPlatform {
    override val versionName: String by lazy {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    override suspend fun refreshWidgets() = BilliWidgets.refresh(context)

    override suspend fun applyAiEnabled(enabled: Boolean) {
        // Share to Billi only feeds Add with AI, so it leaves the share sheet with AI.
        val share = ComponentName(context, ShareActivity::class.java)
        val pm = context.packageManager
        val wanted = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        withContext(Dispatchers.IO) {
            if (pm.getComponentEnabledSetting(share) != wanted) {
                pm.setComponentEnabledSetting(share, wanted, PackageManager.DONT_KILL_APP)
            }
        }
        BilliWidgets.refresh(context)
        sh.taf.billi.assistant.AppShortcuts.publish(context, enabled)
    }
}

@Composable
actual fun windowWidthDp(): Int = LocalConfiguration.current.screenWidthDp

@Composable
actual fun appIconPainter(): Painter = painterResource(R.drawable.ic_launcher_legacy)
