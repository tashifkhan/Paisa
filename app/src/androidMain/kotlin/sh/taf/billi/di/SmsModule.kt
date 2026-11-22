package sh.taf.billi.di

import sh.taf.billi.notifications.AndroidNotificationImportService
import sh.taf.billi.notifications.NotificationImportService
import sh.taf.billi.ingest.PendingAiProcessor
import sh.taf.billi.ingest.PendingAiQueue
import sh.taf.billi.sms.AndroidSmsScanService
import sh.taf.billi.sms.SmsScanService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** SMS scanning for SmsViewModel, backed by the SMS reader worker, and the notification switches. */
val smsModule = module {
    single<SmsScanService> { AndroidSmsScanService(androidContext(), get()) }
    single<NotificationImportService> { AndroidNotificationImportService(androidContext()) }
    single { PendingAiProcessor(androidContext(), get(), get(), get()) }
    single<PendingAiQueue> { get<PendingAiProcessor>() }
}
