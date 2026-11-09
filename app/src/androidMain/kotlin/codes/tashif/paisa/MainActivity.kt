package codes.tashif.paisa

import android.content.Intent
import org.koin.compose.viewmodel.koinViewModel
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.fragment.app.FragmentActivity
import codes.tashif.paisa.data.PaisaDatabase
import codes.tashif.paisa.data.demo.DemoData
import codes.tashif.paisa.sync.SyncManager
import kotlinx.coroutines.runBlocking
import codes.tashif.paisa.ui.app.AppViewModel
import codes.tashif.paisa.ui.app.PaisaApp
import codes.tashif.paisa.ui.sms.SmsViewModel
import codes.tashif.paisa.widget.WidgetDeepLink

class MainActivity : FragmentActivity() {

    // Same instances PaisaApp gets from koinViewModel(), since both use this activity's store.
    private val appViewModel: AppViewModel by viewModel()
    private val smsViewModel: SmsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        seedDemoDataIfAsked()
        handleWidgetIntent(intent)
        setContent { PaisaApp() }
    }

    /**
     * Debug builds only: `adb shell am start -n codes.tashif.paisa/.MainActivity --ez seed_demo true`
     * fills an empty database with DemoData, for screenshots.
     */
    private fun seedDemoDataIfAsked() {
        if (!BuildConfig.DEBUG || !intent.getBooleanExtra(EXTRA_SEED_DEMO, false)) return
        runBlocking {
            val db = PaisaDatabase.get(this@MainActivity)
            if (DemoData.isEmpty(db)) DemoData.seed(db)
        }
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

    private companion object {
        const val EXTRA_SEED_DEMO = "seed_demo"
    }
}
