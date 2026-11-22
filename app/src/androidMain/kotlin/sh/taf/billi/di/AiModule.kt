package sh.taf.billi.di

import sh.taf.billi.statement.AndroidStatementNotifier
import sh.taf.billi.statement.StatementNotifier
import sh.taf.billi.ai.catalog.AiCacheStore
import sh.taf.billi.ai.catalog.FileAiCacheStore
import sh.taf.billi.ai.local.LocalAiEngine
import sh.taf.billi.ai.local.provideLocalAiEngine
import sh.taf.billi.ingest.AiMessageReader
import sh.taf.billi.ingest.LadderMessageCloud
import sh.taf.billi.ingest.MessageCloud
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
