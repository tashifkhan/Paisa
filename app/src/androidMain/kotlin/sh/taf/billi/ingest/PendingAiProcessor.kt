package sh.taf.billi.ingest

import android.content.Context
import android.util.Log
import sh.taf.billi.data.UnrecognizedSms
import sh.taf.billi.data.repository.SmsInboxRepository
import sh.taf.billi.ingest.IngestResult.Outcome
import sh.taf.billi.widget.BilliWidgets
import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.bank.BankParserFactory
import com.pennywiseai.parser.notification.NotificationApps
import com.pennywiseai.parser.notification.PaymentText
import kotlinx.coroutines.sync.Mutex

/**
 * Reads the messages kept for the AI fallback: Gemini Nano only runs while
 * Billi is in front, and a cloud model needs the network. Runs when the app
 * opens, when the user taps the "waiting" prompt and when the setting changes.
 */
class PendingAiProcessor(
    private val context: Context,
    private val ingestor: TransactionIngestor,
    private val inbox: SmsInboxRepository,
    private val reader: AiMessageReader,
) : PendingAiQueue {

    private val running = Mutex()

    override suspend fun process(): Int {
        // A second caller while a pass runs has nothing to add.
        if (!running.tryLock()) return 0
        try {
            return processLocked()
        } finally {
            running.unlock()
        }
    }

    /** Posts or clears the "waiting for AI" prompt after a message was queued. */
    suspend fun promptForQueued(from: String) {
        if (!reader.active()) return
        val waiting = inbox.countPendingAi()
        if (waiting > 0) TransactionAlerts.pendingAi(context, waiting, from)
    }

    private suspend fun processLocked(): Int {
        if (!reader.active()) return 0

        val rows = inbox.pendingForAi(BATCH)
        if (rows.isEmpty()) {
            TransactionAlerts.clearPendingAi(context)
            return 0
        }
        val saved = mutableListOf<Step.Saved>()
        var stillWaiting = false
        for (row in rows) {
            when (val step = readOne(row)) {
                Step.Waiting -> stillWaiting = true
                Step.Done -> Unit
                is Step.Saved -> saved += step
            }
        }
        if (saved.isNotEmpty()) BilliWidgets.refresh(context)
        when {
            saved.size == 1 -> TransactionAlerts.transactionSaved(
                context, saved.single().transactionId, "Read with AI", saved.single().summary
            )
            saved.size > 1 -> TransactionAlerts.aiSummary(context, saved.size)
        }
        if (!stillWaiting && inbox.countPendingAi() == 0) TransactionAlerts.clearPendingAi(context)
        Log.d(TAG, "Read ${rows.size} queued messages, saved ${saved.size}")
        return saved.size
    }

    private sealed interface Step {
        /** The model couldn't run. The row stays queued. */
        data object Waiting : Step

        /** The row is handled and nothing new was saved. */
        data object Done : Step

        data class Saved(val transactionId: Long, val summary: String) : Step
    }

    private suspend fun readOne(row: UnrecognizedSms): Step {
        val bankName = bankNameFor(row) ?: run {
            inbox.markAiAttempted(row.id)
            return Step.Done
        }
        return when (val result = reader.read(row.body, row.sender, bankName, row.timestamp)) {
            AiMessageReader.Result.Unavailable -> Step.Waiting
            AiMessageReader.Result.NotPayment -> {
                // Keep it for review only if it still looks like money moved.
                if (PaymentText.looksLikePayment(row.body)) inbox.markAiAttempted(row.id)
                else inbox.deleteUnrecognized(row.id)
                Step.Done
            }
            is AiMessageReader.Result.Parsed -> {
                val save = ingestor.saveFromQueue(result.transaction, row)
                when (save.outcome) {
                    Outcome.SAVED, Outcome.MERGED, Outcome.DUPLICATE, Outcome.DELETED, Outcome.BALANCE_UPDATED ->
                        inbox.deleteUnrecognized(row.id)
                    else -> inbox.markAiAttempted(row.id)
                }
                val id = save.transactionId
                if (save.saved && id != null) Step.Saved(id, describe(result.transaction, bankName)) else Step.Done
            }
        }
    }

    /** The bank or app a queued row came from, or null when nothing reads that sender any more. */
    private fun bankNameFor(row: UnrecognizedSms): String? =
        if (row.source == TransactionWriter.SOURCE_NOTIFICATION) {
            NotificationApps.find(row.sender)?.name
        } else {
            BankParserFactory.getParsers(row.sender).firstOrNull()?.getBankName()
        }

    private fun describe(transaction: ParsedTransaction, bankName: String): String {
        // Indian grouping, as everywhere else in Billi: ₹1,00,000.
        val amount = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN"))
            .apply { maximumFractionDigits = 2 }
            .format(transaction.amount)
        return "₹$amount · ${transaction.merchant?.takeIf { it.isNotBlank() } ?: bankName}"
    }

    private companion object {
        const val TAG = "PendingAiProcessor"

        /** Per pass, so one app open never holds Gemini Nano for minutes. The rest waits for the next. */
        const val BATCH = 20
    }
}
