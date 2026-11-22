package sh.taf.billi.di

import sh.taf.billi.ingest.NoPendingAiQueue
import sh.taf.billi.notifications.NotificationImportService
import sh.taf.billi.ingest.PendingAiQueue
import sh.taf.billi.notifications.WebNotificationImportService
import sh.taf.billi.sms.SmsScanService
import sh.taf.billi.ui.sms.SmsViewModel
import sh.taf.billi.sms.WebSmsScanService
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** SMS on the web: no scanning. Needs the repositories and SnackbarManager bound elsewhere. */
val smsWebModule = module {
    single<SmsScanService> { WebSmsScanService() }
    single<NotificationImportService> { WebNotificationImportService() }
    single<PendingAiQueue> { NoPendingAiQueue }
}
