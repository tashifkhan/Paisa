package sh.taf.billi

import android.app.Application
import androidx.work.Configuration
import sh.taf.billi.di.aiModule
import sh.taf.billi.di.backupModule
import sh.taf.billi.di.billiModules
import sh.taf.billi.di.smsModule
import sh.taf.billi.sync.SyncScheduling
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class BilliApplication : Application(), Configuration.Provider {

    // The default initializer is removed in the manifest so workers get their
    // dependencies from Koin.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()
        // Only Robolectric creates a second Application in one process, once
        // per test. Start from a clean graph each time.
        if (GlobalContext.getOrNull() != null) stopKoin()
        startKoin {
            androidContext(this@BilliApplication)
            modules(billiModules + smsModule + backupModule + aiModule)
        }
        // Local edits from anywhere in the process (UI, SMS receiver, workers)
        // schedule an upload when sync is on.
        SyncScheduling.watchLocalChanges(this)
        sh.taf.billi.ai.local.DebugLocalAi.install(this)
    }
}
