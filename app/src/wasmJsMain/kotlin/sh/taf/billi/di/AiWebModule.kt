package sh.taf.billi.di

import sh.taf.billi.ai.AiCredentialsStore
import sh.taf.billi.ai.AiRepository
import sh.taf.billi.statement.StatementExtractionService
import sh.taf.billi.statement.StatementImportUseCase
import sh.taf.billi.statement.StatementNotifier
import sh.taf.billi.statement.WebStatementNotifier
import sh.taf.billi.ai.catalog.AiCacheStore
import sh.taf.billi.ai.catalog.LiveModelLister
import sh.taf.billi.ai.catalog.LocalStorageAiCacheStore
import sh.taf.billi.ai.catalog.ModelCatalog
import sh.taf.billi.ai.ladder.AiLadderRunner
import sh.taf.billi.ai.local.LocalAiEngine
import sh.taf.billi.ai.local.UnsupportedLocalAiEngine
import sh.taf.billi.ai.provider.ProviderClient
import sh.taf.billi.ai.store.AiConfigStore
import sh.taf.billi.ai.store.AiKeyStore
import sh.taf.billi.ai.store.LegacyMigrator
import sh.taf.billi.ai.store.LocalStorageAiStore
import sh.taf.billi.ui.ai.AiLadderViewModel
import sh.taf.billi.ui.ai.AiProviderViewModel
import sh.taf.billi.ui.ai.AiViewModel
import sh.taf.billi.ui.statement.StatementImportViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The whole AI layer for the web: stores in localStorage (keys unencrypted,
 * see [LocalStorageAiStore]), the services Android declares in appModule,
 * and the AI and statement import viewmodels. The statement viewmodel also
 * needs the repositories, SnackbarManager and AppLockGuard from the web's
 * other modules.
 */
val aiWebModule = module {
    single { AiConfigStore(LocalStorageAiStore(AiConfigStore.PREFS_NAME)) }
    single { AiCredentialsStore(LocalStorageAiStore(AiCredentialsStore.PREFS_NAME)) }
    single<AiCacheStore> { LocalStorageAiCacheStore() }
    single<LocalAiEngine> {
        UnsupportedLocalAiEngine(
            "Local AI runs in the Android app. This web build has no on-device model yet."
        )
    }
    single<StatementNotifier> { WebStatementNotifier() }

}
