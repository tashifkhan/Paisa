package sh.taf.billi.ingest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import sh.taf.billi.MainActivity
import sh.taf.billi.R

/**
 * The alerts Billi posts about imported transactions: one per saved charge,
 * and one standing prompt while messages wait for the on-device model.
 */
object TransactionAlerts {
    const val CHANNEL_ID = "transaction_notifications"
    private const val CHANNEL_NAME = "Transaction Notifications"

    /** A Long transaction id on the alert's intent. */
    const val EXTRA_TRANSACTION_ID = "transaction_id"

    /** Set on the "waiting for AI" prompt, so the app reads the queue as it opens. */
    const val EXTRA_READ_PENDING_AI = "read_pending_ai"

    /** Fixed ids so a newer prompt or summary replaces the older one. */
    private const val PENDING_AI_ID = 0x5041_0001
    private const val AI_SUMMARY_ID = 0x5041_0002

    fun transactionSaved(context: Context, transactionId: Long, title: String, body: String) {
        val intent = openApp(context).putExtra(EXTRA_TRANSACTION_ID, transactionId)
        post(context, transactionId.toInt(), title, body, intent)
    }

    /** Several queued messages were saved at once: one alert that opens the app. */
    fun aiSummary(context: Context, saved: Int) {
        post(
            context,
            AI_SUMMARY_ID,
            "Read with AI",
            "Saved $saved payments Billi couldn't read when they arrived.",
            openApp(context)
        )
    }

    /**
     * A message the rules couldn't read is waiting, and Gemini Nano only runs
     * while Billi is open. Tapping opens Billi, which reads the queue.
     */
    fun pendingAi(context: Context, waiting: Int, from: String) {
        val body = if (waiting <= 1) {
            "A payment from $from needs a read. Tap to read it."
        } else {
            "$waiting payments need a read. Tap to read them."
        }
        val intent = openApp(context).putExtra(EXTRA_READ_PENDING_AI, true)
        post(context, PENDING_AI_ID, "Payment waiting for AI", body, intent, action = "Read now")
    }

    fun clearPendingAi(context: Context) {
        NotificationManagerCompat.from(context).cancel(PENDING_AI_ID)
    }

    private fun openApp(context: Context) = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private fun post(
        context: Context,
        id: Int,
        title: String,
        body: String,
        intent: Intent,
        action: String? = null
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_billi)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (action != null) builder.addAction(0, action, pending)
        try {
            manager.notify(id, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS was revoked between the check and the post.
            Log.w("TransactionAlerts", "Can't post notification", e)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Notifications for new bank transactions" }
        )
    }
}
