package codes.tashif.paisa.backup

import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.sql.sqlRead
import codes.tashif.paisa.sync.SyncEngine
import codes.tashif.paisa.sync.SyncManager
import codes.tashif.paisa.sync.SyncSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sync while the page is open. Browsers have no reliable background work, so
 * this syncs when the page opens, every 15 minutes, when the network comes
 * back or the tab becomes visible, and 20 seconds after a local edit. The
 * Sync now button covers everything else.
 */
object WebSyncScheduler {
    private val scope = CoroutineScope(SupervisorJob())
    private var sync: SyncManager? = null
    private var periodic: Job? = null
    private var soon: Job? = null

    fun start(sync: SyncManager, db: AppDatabase) {
        this.sync = sync
        jsOnWake { requestSoon(1_000) }
        scope.launch {
            db.invalidationTracker.createFlow(*SyncSchema.TABLE_NAMES, emitInitialState = false).collect {
                if (sync.isEnabled() && db.sqlRead { SyncEngine.hasPendingChanges(it) }) requestSoon(20_000)
            }
        }
        reschedule()
        requestSoon(2_000)
    }

    fun reschedule() {
        periodic?.cancel()
        val s = sync ?: return
        if (!s.isEnabled()) return
        periodic = scope.launch {
            while (true) {
                delay(15 * 60_000L)
                runOnce()
            }
        }
    }

    /** Debounced like Android's: each call restarts the wait. */
    fun requestSoon(delayMs: Long) {
        val s = sync ?: return
        if (!s.isEnabled()) return
        soon?.cancel()
        soon = scope.launch {
            delay(delayMs)
            runOnce()
        }
    }

    private suspend fun runOnce() {
        val s = sync ?: return
        if (!s.isEnabled() || !jsOnline()) return
        try {
            s.syncNow()
        } catch (e: Exception) {
            // syncNow records the error for the backup screen; keep a trace for debugging.
            println("Paisa sync failed: ${e.message}")
        }
    }
}

private fun jsOnline(): Boolean = js("navigator.onLine")

private fun jsOnWake(callback: () -> Unit): Unit = js(
    """{
    window.addEventListener('online', () => callback());
    document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') callback(); });
    }"""
)
