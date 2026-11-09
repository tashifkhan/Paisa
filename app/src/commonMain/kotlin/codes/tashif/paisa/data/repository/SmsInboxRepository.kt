package codes.tashif.paisa.data.repository

import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.UnrecognizedSms
import kotlinx.coroutines.flow.Flow

/** Bank-looking SMS from senders no parser knows, kept for the user to review. */
class SmsInboxRepository(db: AppDatabase) {

    private val unrecognizedSmsDao = db.unrecognizedSmsDao()

    val pendingUnrecognizedSms: Flow<List<UnrecognizedSms>> = unrecognizedSmsDao.getPending()
    val pendingUnrecognizedCount: Flow<Int> = unrecognizedSmsDao.getPendingCount()

    suspend fun insertUnrecognizedSms(sms: UnrecognizedSms): Long =
        unrecognizedSmsDao.insert(sms)

    suspend fun markUnrecognizedReviewed(id: Int) = unrecognizedSmsDao.markReviewed(id)

    suspend fun deleteUnrecognized(id: Int) = unrecognizedSmsDao.delete(id)
}
