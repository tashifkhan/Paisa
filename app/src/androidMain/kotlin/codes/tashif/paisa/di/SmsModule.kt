package codes.tashif.paisa.di

import codes.tashif.paisa.ui.sms.AndroidSmsScanService
import codes.tashif.paisa.ui.sms.SmsScanService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** SMS scanning for SmsViewModel, backed by the SMS reader worker. */
val smsModule = module {
    single<SmsScanService> { AndroidSmsScanService(androidContext(), get()) }
}
