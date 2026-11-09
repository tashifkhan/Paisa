package codes.tashif.paisa.di

import codes.tashif.paisa.ui.sms.SmsScanService
import codes.tashif.paisa.ui.sms.SmsViewModel
import codes.tashif.paisa.ui.sms.WebSmsScanService
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** SMS on the web: no scanning. Needs the repositories and SnackbarManager bound elsewhere. */
val smsWebModule = module {
    single<SmsScanService> { WebSmsScanService() }
}
