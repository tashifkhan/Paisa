package codes.tashif.paisa.di

import codes.tashif.paisa.ai.AiCredentialsStore
import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ai.StatementExtractionService
import codes.tashif.paisa.ai.StatementImportUseCase
import codes.tashif.paisa.ai.StatementNotifier
import codes.tashif.paisa.ai.WebStatementNotifier
import codes.tashif.paisa.ai.catalog.AiCacheStore
import codes.tashif.paisa.ai.catalog.LiveModelLister
import codes.tashif.paisa.ai.catalog.LocalStorageAiCacheStore
import codes.tashif.paisa.ai.catalog.ModelCatalog
import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.provider.ProviderClient
import codes.tashif.paisa.ai.store.AiConfigStore
import codes.tashif.paisa.ai.store.AiKeyStore
import codes.tashif.paisa.ai.store.LegacyMigrator
import codes.tashif.paisa.ai.store.LocalStorageAiStore
import codes.tashif.paisa.ui.ai.AiLadderViewModel
import codes.tashif.paisa.ui.ai.AiProviderViewModel
import codes.tashif.paisa.ui.ai.AiViewModel
import codes.tashif.paisa.ui.statement.StatementImportViewModel
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
    single<StatementNotifier> { WebStatementNotifier() }

}
