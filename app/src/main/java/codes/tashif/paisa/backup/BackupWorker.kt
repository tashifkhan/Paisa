package codes.tashif.paisa.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import codes.tashif.paisa.MainActivity
import codes.tashif.paisa.R
import java.io.IOException

/** Scheduled backup. Runs silently; only failures that need the user post a notification. */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val manager = BackupManager(applicationContext)
        if (manager.prefs.load().provider == BackupProvider.NONE) return Result.success()
        return try {
            manager.backupNow()
            Result.success()
        } catch (e: BackupAuthException) {
            notifyFailed(applicationContext, e.message ?: "Sign in again to keep backing up")
            Result.failure()
        } catch (e: IOException) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                notifyFailed(applicationContext, e.message ?: "Backup failed")
                Result.failure()
            }
        }
    }

    private fun notifyFailed(context: Context, message: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Backups", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Problems with automatic backups" }
            )
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Backup didn't run")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check and the call.
        }
    }

    companion object {
        private const val CHANNEL_ID = "backups"
        private const val NOTIFICATION_ID = 9301
    }
}
