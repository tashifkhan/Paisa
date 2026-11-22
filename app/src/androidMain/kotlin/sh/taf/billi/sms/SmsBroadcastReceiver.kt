package sh.taf.billi.sms

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import sh.taf.billi.ingest.IngestResult
import sh.taf.billi.ingest.PendingAiProcessor
import sh.taf.billi.ingest.TransactionAlerts
import sh.taf.billi.ingest.TransactionIngestor
import sh.taf.billi.widget.BilliWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Real-time SMS interceptor — parses bank alerts as they arrive.
 */
class SmsBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val ingestor: TransactionIngestor by inject()
    private val pendingAi: PendingAiProcessor by inject()

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        data class SmsData(val body: StringBuilder, var timestamp: Long)
        val smsMap = mutableMapOf<String, SmsData>()
        for (message in messages) {
            val sender = message.originatingAddress ?: continue
            val body = message.messageBody ?: continue
            val timestamp = message.timestampMillis
            val existing = smsMap.getOrPut(sender) { SmsData(StringBuilder(), timestamp) }
            existing.body.append(body)
            if (timestamp < existing.timestamp) existing.timestamp = timestamp
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                for ((sender, data) in smsMap) {
                    val result = ingestor.processSms(sender, data.body.toString(), data.timestamp)
                    if (result.changedData) BilliWidgets.refresh(context.applicationContext)
                    val id = result.transactionId
                    when {
                        result.saved && id != null -> {
                            Log.d(TAG, "Saved live SMS transaction $id")
                            TransactionAlerts.transactionSaved(context, id, "New transaction", "Saved from $sender")
                        }
                        result.outcome == IngestResult.Outcome.QUEUED_FOR_AI -> pendingAi.promptForQueued(sender)
                        else -> Log.d(TAG, "SMS not saved: ${result.outcome} ${result.detail.orEmpty()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
