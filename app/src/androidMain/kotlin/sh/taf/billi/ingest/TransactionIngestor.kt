package sh.taf.billi.ingest

import android.util.Log
import sh.taf.billi.data.UnrecognizedSms
import sh.taf.billi.data.repository.SmsInboxRepository
import sh.taf.billi.ingest.IngestResult.Outcome
import sh.taf.billi.ingest.TransactionWriter.Companion.SOURCE_NOTIFICATION
import sh.taf.billi.ingest.TransactionWriter.Companion.SOURCE_SMS
import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.bank.BankParserFactory
import com.pennywiseai.parser.core.md5Hex
import com.pennywiseai.parser.notification.NotificationApps
import com.pennywiseai.parser.notification.PaymentNotificationParser
import com.pennywiseai.parser.notification.PaymentText
import kotlinx.coroutines.CancellationException
import java.util.Locale

/**
 * Turns a bank SMS or a bank/UPI app notification into a transaction. Shared by
 * the SMS receiver, the bulk SMS scan, the notification listener and the
 * AI queue.
 *
 * Each message goes through the rules first: the bank's SMS parser, then, for
 * notifications, [PaymentNotificationParser]. What the rules can't read goes
 * to the model the user picked, on-device or cloud. When it can't run right
 * now, the message is queued for [PendingAiProcessor].
 */
class TransactionIngestor(
    private val writer: TransactionWriter,
    private val inbox: SmsInboxRepository,
    /** The AI fallback, on-device or cloud. Null in tests. */
    private val localAi: AiMessageReader? = null
) {

    /**
     * Reads one SMS. [askModelNow] is false for the bulk inbox scan, which may
     * hold hundreds of messages: those are queued for the model instead of
     * holding the scan up one model call at a time.
     */
    suspend fun processSms(sender: String, body: String, timestamp: Long, askModelNow: Boolean = true): IngestResult =
        guarded("SMS from $sender") {
            val parsers = BankParserFactory.getParsers(sender)
            if (parsers.isEmpty()) {
                // Unknown sender: keep transaction-looking text for the user to review.
                return@guarded if (looksLikeTransaction(body)) {
                    // No bank to name, so the model never reads these.
                    keep(sender, body, timestamp, SOURCE_SMS, null, Outcome.NEEDS_REVIEW, aiAttempted = true)
                } else {
                    IngestResult.of(Outcome.IGNORED, "no parser for $sender")
                }
            }
            val parsed = parsers.firstNotNullOfOrNull { it.parse(body, sender, timestamp) }
            if (parsed != null) return@guarded writer.save(parsed, SOURCE_SMS)

            // A known bank, but the rules couldn't read it: OTP, promo, or a format they miss.
            fallback(
                body = body,
                sender = sender,
                bankName = parsers.first().getBankName(),
                timestamp = timestamp,
                source = SOURCE_SMS,
                senderName = null,
                askModelNow = askModelNow,
                // Without the model, an unreadable message from a known bank was always dropped.
                reviewIfUnread = false,
                save = { writer.save(it, SOURCE_SMS) }
            )
        }

    /**
     * Reads a notification from an app in [NotificationApps]. [postedAt] is when
     * the app first built it, which stays the same when the app reposts it.
     */
    suspend fun processNotification(packageName: String, message: String, postedAt: Long): IngestResult {
        val app = NotificationApps.find(packageName)
            ?: return IngestResult.of(Outcome.IGNORED, "not a supported app: $packageName")
        return guarded("notification from $packageName") {
            val parsed = app.parserSender?.let { sender ->
                BankParserFactory.getParsers(sender).firstNotNullOfOrNull { it.parse(message, sender, postedAt) }
            } ?: PaymentNotificationParser.parse(app, message, postedAt)
            val save: suspend (ParsedTransaction) -> IngestResult = { saveNotification(it, packageName, message, postedAt) }
            if (parsed != null) return@guarded save(parsed)

            fallback(
                body = message,
                sender = packageName,
                bankName = app.name,
                timestamp = postedAt,
                source = SOURCE_NOTIFICATION,
                senderName = app.name,
                askModelNow = true,
                reviewIfUnread = true,
                save = save
            )
        }
    }

    /** Saves what the on-device model read from a queued message. */
    internal suspend fun saveFromQueue(parsed: ParsedTransaction, row: UnrecognizedSms): IngestResult =
        if (row.source == SOURCE_NOTIFICATION) {
            saveNotification(parsed, row.sender, row.body, row.timestamp)
        } else {
            writer.save(parsed.copy(sender = row.sender, smsBody = row.body), SOURCE_SMS)
        }

    /**
     * A bank's parser names the bank ("HDFC Bank"), so the charge lands on the
     * same account its SMS uses. The package stays as the sender so the row's
     * channel is known later. A repost has the same text and time, so it
     * hashes the same and is skipped.
     */
    private suspend fun saveNotification(
        parsed: ParsedTransaction,
        packageName: String,
        message: String,
        postedAt: Long
    ): IngestResult = writer.save(
        parsed.copy(sender = packageName, smsBody = message, transactionHash = notificationHash(packageName, postedAt, message)),
        SOURCE_NOTIFICATION
    )

    private suspend fun fallback(
        body: String,
        sender: String,
        bankName: String,
        timestamp: Long,
        source: String,
        senderName: String?,
        askModelNow: Boolean,
        reviewIfUnread: Boolean,
        save: suspend (ParsedTransaction) -> IngestResult
    ): IngestResult {
        val ai = localAi
        var modelSaidNo = false
        if (ai != null && ai.worthQueueing(body)) {
            if (askModelNow) {
                when (val read = ai.read(body, sender, bankName, timestamp)) {
                    is AiMessageReader.Result.Parsed -> {
                        Log.d(TAG, "AI (${ai.mode}) read a $source from $sender")
                        return save(read.transaction)
                    }
                    AiMessageReader.Result.NotPayment -> modelSaidNo = true
                    AiMessageReader.Result.Unavailable ->
                        return keep(sender, body, timestamp, source, senderName, Outcome.QUEUED_FOR_AI)
                }
            } else {
                return keep(sender, body, timestamp, source, senderName, Outcome.QUEUED_FOR_AI)
            }
        }
        return if (reviewIfUnread && PaymentText.looksLikePayment(body)) {
            // Unless the model already said no, it gets a try once the user turns it on.
            keep(sender, body, timestamp, source, senderName, Outcome.NEEDS_REVIEW, aiAttempted = modelSaidNo)
        } else {
            IngestResult.of(Outcome.IGNORED, "not a readable transaction")
        }
    }

    /** Keeps [body] in the review list. Rows with [aiAttempted] false get a try from the on-device model. */
    private suspend fun keep(
        sender: String,
        body: String,
        timestamp: Long,
        source: String,
        senderName: String?,
        outcome: Outcome,
        aiAttempted: Boolean = false
    ): IngestResult {
        inbox.insertUnrecognizedSms(
            UnrecognizedSms(
                sender = sender,
                body = body,
                bodyHash = md5Hex(body).take(16),
                timestamp = timestamp,
                createdAt = TransactionWriter.nowIso(),
                source = source,
                senderName = senderName,
                aiAttempted = aiAttempted
            )
        )
        return IngestResult.of(outcome)
    }

    private inline fun guarded(what: String, block: () -> IngestResult): IngestResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Error processing $what", e)
        IngestResult.of(Outcome.FAILED, e.message)
    }

    private fun looksLikeTransaction(body: String): Boolean {
        val lower = body.lowercase(Locale.getDefault())
        return transactionWords.any { lower.contains(it) }
    }

    companion object {
        private const val TAG = "TransactionIngestor"

        private val transactionWords = listOf(
            "debited", "credited", "withdrawn", "deposited", "spent",
            "received", "transferred", "paid", "rs.", "inr", "₹", "a/c"
        )

        fun notificationHash(packageName: String, postedAt: Long, message: String): String =
            md5Hex("notification|$packageName|$postedAt|$message")
    }
}
