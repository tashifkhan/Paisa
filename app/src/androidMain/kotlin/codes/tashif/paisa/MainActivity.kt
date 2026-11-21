package codes.tashif.paisa

import android.content.Intent
import org.koin.compose.viewmodel.koinViewModel
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.fragment.app.FragmentActivity
import codes.tashif.paisa.data.PaisaDatabase
import codes.tashif.paisa.data.demo.DemoData
import codes.tashif.paisa.ai.local.DebugLocalAi
import codes.tashif.paisa.ingest.PendingAiQueue
import codes.tashif.paisa.ingest.TransactionAlerts
import codes.tashif.paisa.sync.SyncScheduling
import kotlinx.coroutines.runBlocking
import codes.tashif.paisa.core.AppNavigator
import codes.tashif.paisa.ui.app.AppViewModel
import codes.tashif.paisa.ui.app.PaisaApp
import codes.tashif.paisa.ui.sms.SmsViewModel
import codes.tashif.paisa.widget.WidgetDeepLink
import codes.tashif.paisa.core.SharedInput
import codes.tashif.paisa.share.SharedFiles

class MainActivity : FragmentActivity(), KoinComponent {

    // Same instances PaisaApp gets from koinViewModel(), since both use this activity's store.
    private val appViewModel: AppViewModel by viewModel()
    private val smsViewModel: SmsViewModel by viewModel()
    private val pendingAi: PendingAiQueue by inject()
    private val appNavigator: AppNavigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DebugLocalAi.handle(this, intent)
        seedDemoDataIfAsked()
        handleWidgetIntent(intent)
        handleTransactionIntent(intent)
        // A recreated activity still holds the share intent it already handed over.
        if (savedInstanceState == null) handleShareIntent(intent)
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
        DebugLocalAi.handle(this, intent)
        handleWidgetIntent(intent)
        handleTransactionIntent(intent)
        handleShareIntent(intent)
    }

    /** A transaction alert tapped while the app was open. */
    private fun handleTransactionIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        // Alerts put a Long; an Int from older alerts or adb still works.
        val id = when (val raw = extras.get(TransactionAlerts.EXTRA_TRANSACTION_ID)) {
            is Long -> raw
            is Int -> raw.toLong()
            else -> return
        }
        // Consumed once, so rotating the screen doesn't reopen it.
        intent.removeExtra(TransactionAlerts.EXTRA_TRANSACTION_ID)
        if (id in 1..Int.MAX_VALUE) appNavigator.openTransaction(id.toInt())
    }

    /** Files and text from [codes.tashif.paisa.share.ShareActivity], for "Add with AI". */
    private fun handleShareIntent(intent: Intent?) {
        val (files, text) = SharedFiles.read(this, intent) ?: return
        appViewModel.requestShare(SharedInput(files, text))
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
        SyncScheduling.requestSoon(this, delaySeconds = 1)
    }

    override fun onResume() {
        super.onResume()
        // Gemini Nano may refuse to run from the notification service. Retry the
        // messages kept for it now that Paisa is in front, then alert the user.
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { pendingAi.process() }
        }
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
