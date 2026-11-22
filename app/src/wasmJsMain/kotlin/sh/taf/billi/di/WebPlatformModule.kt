package sh.taf.billi.di

import sh.taf.billi.data.AppDatabase
import sh.taf.billi.data.createWebDatabase
import sh.taf.billi.ui.app.AppPlatform
import sh.taf.billi.ui.app.WebAppPlatform
import sh.taf.billi.export.TransactionExporter
import org.koin.dsl.module

/** The web's database and shell services. PDF export has no web renderer yet. */
val webPlatformModule = module {
    single<AppDatabase> { createWebDatabase() }
    single<AppPlatform> { WebAppPlatform(versionName = sh.taf.billi.WEB_VERSION_NAME) }
    factory { TransactionExporter(get(), get(), null) }
    single<sh.taf.billi.assistant.AssistantAccess> { sh.taf.billi.assistant.NoAssistantAccess }
}

val webModules = listOf(
    webPlatformModule, daoModule, coreModule, aiServicesModule, cloudModule,
    aiWebModule, backupWebModule, smsWebModule, viewModelModule
)
