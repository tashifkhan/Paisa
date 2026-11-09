package codes.tashif.paisa

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import codes.tashif.paisa.sync.SyncManager
import codes.tashif.paisa.ui.app.AppViewModel
import codes.tashif.paisa.ui.app.PaisaApp
import codes.tashif.paisa.ui.sms.SmsViewModel
import codes.tashif.paisa.widget.WidgetDeepLink
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    // Same instances PaisaApp gets from hiltViewModel(), since both use this activity's store.
    private val appViewModel: AppViewModel by viewModels()
    private val smsViewModel: SmsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleWidgetIntent(intent)
        setContent { PaisaApp() }
    }

    // launchMode is singleTop, so a widget tap on an already-running app arrives
    // here rather than through onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        appViewModel.requestWidgetDestination(
            intent?.getStringExtra(WidgetDeepLink.EXTRA_DESTINATION)
        )
    }

    override fun onStart() {
        super.onStart()
        // Applies a lock deferred for a picker/export detour that ran too long.
        appViewModel.onReturnToForeground()
        // Catch messages delivered while the app was closed (or if the live SMS
        // broadcast was delayed by the device). The worker deduplicates rows.
        smsViewModel.scanSmsOnAppForeground()
        // Pick up what other devices changed while this one was closed.
        SyncManager.requestSoon(this, delaySeconds = 1)
    }

    override fun onStop() {
        super.onStop()
        // Lock when leaving the app so the next resume requires biometrics.
        // No-op while an in-app picker or export sheet is on screen.
        appViewModel.lockApp()
    }
}
