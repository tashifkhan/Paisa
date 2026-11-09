package codes.tashif.paisa.ui.sms

import androidx.work.WorkInfo
import androidx.work.WorkManager
import codes.tashif.paisa.sms.SmsReaderWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SmsScanProgress(
    val isRunning: Boolean = false,
    val total: Int = 0,
    val processed: Int = 0,
    val saved: Int = 0,
    val duplicates: Int = 0,
    val unrecognized: Int = 0
)

/** Progress of the SMS reader worker, read from WorkManager. */
fun WorkManager.smsScanProgress(): Flow<SmsScanProgress> =
    getWorkInfosForUniqueWorkFlow(SmsReaderWorker.WORK_NAME)
        .map { infos ->
            // REPLACE creates a new WorkSpec. Prefer active work so an older
            // completed generation cannot make the home rescan button look idle.
            val info = infos.firstOrNull {
                it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
            } ?: infos.firstOrNull()
            val running = info?.state == WorkInfo.State.RUNNING ||
                info?.state == WorkInfo.State.ENQUEUED
            val progress = info?.progress
            val output = info?.outputData
            val data = if (info?.state == WorkInfo.State.SUCCEEDED) output else progress
            SmsScanProgress(
                isRunning = running,
                total = data?.getInt(SmsReaderWorker.PROGRESS_TOTAL, 0) ?: 0,
                processed = data?.getInt(SmsReaderWorker.PROGRESS_PROCESSED, 0) ?: 0,
                saved = data?.getInt(SmsReaderWorker.PROGRESS_SAVED, 0) ?: 0,
                duplicates = data?.getInt(SmsReaderWorker.PROGRESS_DUPLICATES, 0) ?: 0,
                unrecognized = data?.getInt(SmsReaderWorker.PROGRESS_UNRECOGNIZED, 0) ?: 0
            )
        }
