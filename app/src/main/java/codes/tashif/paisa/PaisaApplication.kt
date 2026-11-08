package codes.tashif.paisa

import android.app.Application
import codes.tashif.paisa.sync.SyncManager

class PaisaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Local edits from anywhere in the process (UI, SMS receiver, workers)
        // schedule an upload when sync is on.
        SyncManager.watchLocalChanges(this)
    }
}
