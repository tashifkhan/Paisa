package codes.tashif.paisa

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import codes.tashif.paisa.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PaisaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // The default initializer is removed in the manifest so workers get Hilt injection.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Local edits from anywhere in the process (UI, SMS receiver, workers)
        // schedule an upload when sync is on.
        SyncManager.watchLocalChanges(this)
    }
}
