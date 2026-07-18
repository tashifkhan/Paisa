package codes.tashif.paisa.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import codes.tashif.paisa.MainActivity
import codes.tashif.paisa.R

/**
 * Posts completion notifications for statement extraction so the user can leave
 * the app while the LLM works and still know when results are ready.
 */
object StatementNotifications {

    private const val CHANNEL_ID = "statement_import"
    private const val CHANNEL_NAME = "Statement import"
    private const val NOTIFICATION_ID = 9201

    fun notifyExtractionDone(context: Context, found: Int, duplicates: Int) {
        val text = buildString {
            append(
                if (found == 1) "1 transaction ready for review" else "$found transactions ready for review"
            )
            if (duplicates > 0) {
                append(" · $duplicates possible duplicate${if (duplicates == 1) "" else "s"}")
            }
        }
        post(context, title = "Statement processed", text = text)
    }

    fun notifyExtractionFailed(context: Context, message: String) {
        post(context, title = "Statement import failed", text = message.take(200))
    }

    private fun post(context: Context, title: String, text: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Statement extraction progress and results" }
            )
        }

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { nm.notify(NOTIFICATION_ID, notification) }
    }
}
