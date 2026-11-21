package codes.tashif.paisa.di

import codes.tashif.paisa.statement.AndroidStatementNotifier
import codes.tashif.paisa.statement.StatementNotifier
import codes.tashif.paisa.ai.catalog.AiCacheStore
import codes.tashif.paisa.ai.catalog.FileAiCacheStore
import codes.tashif.paisa.ai.local.LocalAiEngine
import codes.tashif.paisa.ai.local.provideLocalAiEngine
import codes.tashif.paisa.ingest.AiMessageReader
import codes.tashif.paisa.ingest.LadderMessageCloud
import codes.tashif.paisa.ingest.MessageCloud
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Android pieces of the AI layer: the catalogue cache files and statement notifications. */
val aiModule = module {
    single<AiCacheStore> { FileAiCacheStore(androidContext()) }
    single<LocalAiEngine> { provideLocalAiEngine(androidContext()) }
    single<MessageCloud> { LadderMessageCloud(get(), get()) }
    single { AiMessageReader(get(), get(), get()) }
    single<StatementNotifier> { AndroidStatementNotifier(androidContext()) }
}
