package codes.tashif.paisa.di

import codes.tashif.paisa.notifications.AndroidNotificationImportService
import codes.tashif.paisa.notifications.NotificationImportService
import codes.tashif.paisa.ingest.PendingAiProcessor
import codes.tashif.paisa.ingest.PendingAiQueue
import codes.tashif.paisa.sms.AndroidSmsScanService
import codes.tashif.paisa.sms.SmsScanService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** SMS scanning for SmsViewModel, backed by the SMS reader worker, and the notification switches. */
val smsModule = module {
    single<SmsScanService> { AndroidSmsScanService(androidContext(), get()) }
    single<NotificationImportService> { AndroidNotificationImportService(androidContext()) }
    single { PendingAiProcessor(androidContext(), get(), get(), get()) }
    single<PendingAiQueue> { get<PendingAiProcessor>() }
}
