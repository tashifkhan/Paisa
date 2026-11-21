package codes.tashif.paisa.di

import codes.tashif.paisa.ingest.NoPendingAiQueue
import codes.tashif.paisa.notifications.NotificationImportService
import codes.tashif.paisa.ingest.PendingAiQueue
import codes.tashif.paisa.notifications.WebNotificationImportService
import codes.tashif.paisa.sms.SmsScanService
import codes.tashif.paisa.ui.sms.SmsViewModel
import codes.tashif.paisa.sms.WebSmsScanService
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** SMS on the web: no scanning. Needs the repositories and SnackbarManager bound elsewhere. */
val smsWebModule = module {
    single<SmsScanService> { WebSmsScanService() }
    single<NotificationImportService> { WebNotificationImportService() }
    single<PendingAiQueue> { NoPendingAiQueue }
}
