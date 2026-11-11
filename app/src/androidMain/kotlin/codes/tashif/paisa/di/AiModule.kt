package codes.tashif.paisa.di

import codes.tashif.paisa.ai.AndroidStatementNotifier
import codes.tashif.paisa.ai.StatementNotifier
import codes.tashif.paisa.ai.catalog.AiCacheStore
import codes.tashif.paisa.ai.catalog.FileAiCacheStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Android pieces of the AI layer: the catalogue cache files and statement notifications. */
val aiModule = module {
    single<AiCacheStore> { FileAiCacheStore(androidContext()) }
    single<StatementNotifier> { AndroidStatementNotifier(androidContext()) }
}
