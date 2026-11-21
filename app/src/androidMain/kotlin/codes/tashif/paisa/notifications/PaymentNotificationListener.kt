package codes.tashif.paisa.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import codes.tashif.paisa.ingest.IngestResult
import codes.tashif.paisa.ingest.PendingAiProcessor
import codes.tashif.paisa.ingest.TransactionAlerts
import codes.tashif.paisa.ingest.TransactionIngestor
import codes.tashif.paisa.widget.PaisaWidgets
import com.pennywiseai.parser.notification.NotificationApps
import com.pennywiseai.parser.notification.NotificationText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Reads payment notifications from the bank and UPI apps in [NotificationApps],
 * once the user turns this on in settings and grants notification access.
 * Every other app's notifications are dropped on arrival, before their text
 * is read.
 */
class PaymentNotificationListener : NotificationListenerService(), KoinComponent {

    private val ingestor: TransactionIngestor by inject()
    private val pendingAi: PendingAiProcessor by inject()
    private val settings: NotificationImportService by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        live = this
        settings.refreshAccess()
        // After a boot or a rebind the system connects the service even when the
        // switch is off. Stop the callbacks again instead of reading and dropping.
        if (!settings.state.value.enabled) stopReading()
    }

    override fun onListenerDisconnected() {
        if (live === this) live = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            read(sbn)
        } catch (e: Exception) {
            // Another app's malformed extras must never take Paisa down with it.
            Log.e(TAG, "Error reading notification", e)
        }
    }

    private fun read(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        // Paisa's own "New transaction" alerts must never feed back in.
        if (packageName == this.packageName) return
        // Touch the settings first: building it restores user-added apps into
        // NotificationApps before the first notification of a fresh process.
        val service = settings as? AndroidNotificationImportService ?: return
        if (!service.isReading(packageName)) return
        val app = NotificationApps.find(packageName) ?: return

        val notification = sbn.notification ?: return
        if (NotificationText.skips(notification.flags)) return
        val extras = notification.extras ?: return
        val message = NotificationText.message(
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.map { it.toString() }.orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            summary = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        )
        if (message.isBlank()) return
        // The app's own timestamp survives reposts of the same notification; postTime doesn't.
        val postedAt = notification.`when`.takeIf { it > 0 } ?: sbn.postTime
        val appName = app.name

        scope.launch {
            try {
                val result = ingestor.processNotification(packageName, message, postedAt)
                if (result.changedData) PaisaWidgets.refresh(applicationContext)
                val id = result.transactionId
                when {
                    result.saved && id != null && result.needsAccount ->
                        TransactionAlerts.transactionSaved(
                            applicationContext, id, "Which account was this?",
                            "Saved from $appName. Tap to pick the account."
                        )
                    result.saved && id != null ->
                        TransactionAlerts.transactionSaved(applicationContext, id, "New transaction", "Saved from $appName")
                    result.outcome == IngestResult.Outcome.QUEUED_FOR_AI -> pendingAi.promptForQueued(appName)
                    else -> Log.d(TAG, "Notification from $packageName not saved: ${result.outcome} ${result.detail.orEmpty()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification from $packageName", e)
            }
        }
    }

    override fun onDestroy() {
        if (live === this) live = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PaymentNotifListener"

        /** The connected service, so settings can unbind it when the master switch goes off. */
        @Volatile
        private var live: PaymentNotificationListener? = null

        /** Best-effort stop of the system callbacks while the master switch is off. */
        fun stopReading() {
            runCatching { live?.requestUnbind() }
        }
    }
}
