package codes.tashif.paisa.di

import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.createWebDatabase
import codes.tashif.paisa.ui.app.AppPlatform
import codes.tashif.paisa.ui.app.WebAppPlatform
import codes.tashif.paisa.ui.settings.TransactionExporter
import org.koin.dsl.module

/** The web's database and shell services. PDF export has no web renderer yet. */
val webPlatformModule = module {
    single<AppDatabase> { createWebDatabase() }
    single<AppPlatform> { WebAppPlatform(versionName = "web") }
    factory { TransactionExporter(get(), get(), null) }
}

val webModules = listOf(
    webPlatformModule, daoModule, coreModule, aiServicesModule,
    aiWebModule, backupWebModule, smsWebModule, viewModelModule
)
