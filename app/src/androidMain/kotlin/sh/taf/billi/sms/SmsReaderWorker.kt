package sh.taf.billi.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import sh.taf.billi.R
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.ingest.IngestResult
import sh.taf.billi.ingest.PendingAiProcessor
import sh.taf.billi.ingest.TransactionIngestor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bulk inbox scan: reads SMS via ContentProvider, parses with BankParserFactory,
 * and stores transactions through [TransactionIngestor].
 */
class SmsReaderWorker(
    appContext: Context,
    params: WorkerParameters,
    private val ingestor: TransactionIngestor,
    private val settingsRepository: SettingsRepository,
    private val pendingAi: PendingAiProcessor
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "SmsReaderWorker"
        const val WORK_NAME = "paisa_sms_reader_work"
        const val PROGRESS_TOTAL = "progress_total"
        const val PROGRESS_PROCESSED = "progress_processed"
        const val PROGRESS_SAVED = "progress_saved"
        const val PROGRESS_PARSED = "progress_parsed"
        const val PROGRESS_DUPLICATES = "progress_duplicates"
        const val PROGRESS_UNRECOGNIZED = "progress_unrecognized"
        const val KEY_FORCE_FULL = "force_full"
        private const val NOTIFICATION_ID = 9101
        private const val CHANNEL_ID = "sms_scan_channel"

        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE
        )

        fun enqueue(
            context: Context,
            forceFull: Boolean = false,
            existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE
        ) {
            val request = OneTimeWorkRequestBuilder<SmsReaderWorker>()
                .setInputData(workDataOf(KEY_FORCE_FULL to forceFull))
                .addTag("sms_scan")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                existingWorkPolicy,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    /** Counts for the scan's progress notification. */
    private data class ScanStats(
        val total: Int,
        var parsed: Int = 0,
        var saved: Int = 0,
        var duplicates: Int = 0,
        var unrecognized: Int = 0,
        var skipped: Int = 0
    )

    private data class SmsMessage(
        val id: Long,
        val sender: String,
        val timestamp: Long,
        val body: String
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "READ_SMS not granted")
            return@withContext Result.failure(
                workDataOf("error" to "READ_SMS permission not granted")
            )
        }

        trySetForeground(createForegroundInfo(0, 0))

        val settings = settingsRepository.getSettings()
        val forceFull = inputData.getBoolean(KEY_FORCE_FULL, false)
        val scanStartedAt = System.currentTimeMillis()
        val since = SmsScanWindow.startTime(
            lastScanAt = settings?.lastSmsScanAt ?: 0L,
            forceFull = forceFull,
            now = scanStartedAt
        )

        val messages = readSmsMessages(since)
        val stats = ScanStats(total = messages.size)

        Log.d(TAG, "Scanning ${messages.size} SMS messages (since=$since forceFull=$forceFull)")

        setProgress(progressData(stats))
        trySetForeground(createForegroundInfo(0, messages.size))

        var newestTimestamp = since
        messages.forEachIndexed { index, sms ->
            if (sms.timestamp > newestTimestamp) newestTimestamp = sms.timestamp

            // The model is not asked inline: a full scan can hold hundreds of
            // messages. What the rules miss is queued for PendingAiProcessor.
            val result = ingestor.processSms(sms.sender, sms.body, sms.timestamp, askModelNow = false)
            when (result.outcome) {
                IngestResult.Outcome.SAVED -> {
                    stats.parsed++
                    stats.saved++
                }
                IngestResult.Outcome.DUPLICATE, IngestResult.Outcome.DELETED, IngestResult.Outcome.MERGED ->
                    stats.duplicates++
                IngestResult.Outcome.NEEDS_REVIEW, IngestResult.Outcome.QUEUED_FOR_AI -> stats.unrecognized++
                else -> stats.skipped++
            }
            if (index % 25 == 0 || index == messages.lastIndex) {
                val processed = index + 1
                setProgress(progressData(stats, processed))
                trySetForeground(createForegroundInfo(processed, messages.size))
            }
        }

        // Persist last scan watermark
        settings?.let {
            settingsRepository.update(
                it.copy(lastSmsScanAt = maxOf(newestTimestamp, scanStartedAt))
            )
        }

        Log.d(
            TAG,
            "Scan done: total=${messages.size} saved=${stats.saved} " +
                "dup=${stats.duplicates} unrecognized=${stats.unrecognized}"
        )

        // A widget-triggered scan runs with no ViewModel observing accounts, so
        // refresh the home screen widgets from here once new rows are in.
        sh.taf.billi.widget.BilliWidgets.refresh(applicationContext)

        // Most scans start from the open app, where Gemini Nano can run. Read
        // what was queued now; whatever is left gets the "waiting" prompt.
        runCatching {
            pendingAi.process()
            pendingAi.promptForQueued(from = "your bank")
        }

        Result.success(progressData(stats, messages.size))
    }

    private fun progressData(
        stats: ScanStats,
        processed: Int = 0
    ): Data {
        return workDataOf(
            PROGRESS_TOTAL to stats.total,
            PROGRESS_PROCESSED to processed,
            PROGRESS_SAVED to stats.saved,
            PROGRESS_PARSED to stats.parsed,
            PROGRESS_DUPLICATES to stats.duplicates,
            PROGRESS_UNRECOGNIZED to stats.unrecognized
        )
    }

    private fun readSmsMessages(sinceMillis: Long): List<SmsMessage> {
        val results = mutableListOf<SmsMessage>()
        val selection: String?
        val selectionArgs: Array<String>?
        if (sinceMillis > 0L) {
            selection = "${Telephony.Sms.DATE} > ? AND ${Telephony.Sms.TYPE} = ?"
            selectionArgs = arrayOf(
                sinceMillis.toString(),
                Telephony.Sms.MESSAGE_TYPE_INBOX.toString()
            )
        } else {
            selection = "${Telephony.Sms.TYPE} = ?"
            selectionArgs = arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())
        }

        applicationContext.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            while (cursor.moveToNext()) {
                val sender = cursor.getString(addrIdx) ?: continue
                val body = cursor.getString(bodyIdx) ?: continue
                results += SmsMessage(
                    id = cursor.getLong(idIdx),
                    sender = sender,
                    timestamp = cursor.getLong(dateIdx),
                    body = body
                )
            }
        }
        return results
    }

    // The scan must survive even when the foreground promotion is rejected
    // (FGS restrictions, notifications off) — it just runs without a notification.
    private suspend fun trySetForeground(info: ForegroundInfo) {
        try {
            setForeground(info)
        } catch (e: Exception) {
            Log.w(TAG, "setForeground failed, continuing as background work", e)
        }
    }

    private fun createForegroundInfo(processed: Int, total: Int): ForegroundInfo {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "SMS Scan",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sync)
            .setContentTitle("Scanning bank SMS…")
            .setContentText(if (total > 0) "Processed $processed / $total" else "Reading inbox…")
            .setProgress(total.coerceAtLeast(0), processed, total == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
