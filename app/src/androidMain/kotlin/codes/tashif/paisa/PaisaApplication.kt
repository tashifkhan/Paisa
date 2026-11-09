package codes.tashif.paisa

import android.app.Application
import androidx.work.Configuration
import codes.tashif.paisa.di.aiModule
import codes.tashif.paisa.di.paisaModules
import codes.tashif.paisa.sync.SyncManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class PaisaApplication : Application(), Configuration.Provider {

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
            androidContext(this@PaisaApplication)
            modules(paisaModules + aiModule)
        }
        // Local edits from anywhere in the process (UI, SMS receiver, workers)
        // schedule an upload when sync is on.
        SyncManager.watchLocalChanges(this)
    }
}
