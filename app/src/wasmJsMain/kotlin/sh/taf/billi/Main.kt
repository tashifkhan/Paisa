package sh.taf.billi

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import sh.taf.billi.sync.WebSyncScheduler
import sh.taf.billi.data.AppDatabase
import sh.taf.billi.sync.SyncManager
import sh.taf.billi.data.demo.DemoData
import sh.taf.billi.di.webModules
import sh.taf.billi.ui.app.BilliApp
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

/** `?demo` in the URL fills an empty database with sample data. */
private fun wantsDemo(): Boolean = js("new URLSearchParams(location.search).has('demo')")

/** Asks the browser not to evict the database under storage pressure. Best effort. */
private fun requestPersistentStorage(): Unit =
    js("{ if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {}); }")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin { modules(webModules) }
    requestPersistentStorage()
    val start = {
        val koin = KoinPlatform.getKoin()
        WebSyncScheduler.start(koin.get<SyncManager>(), koin.get<AppDatabase>())
        ComposeViewport(document.body!!) { BilliApp() }
    }
    if (wantsDemo()) {
        MainScope().launch {
            val db = KoinPlatform.getKoin().get<AppDatabase>()
            if (DemoData.isEmpty(db)) DemoData.seed(db)
            start()
        }
    } else {
        start()
    }
}
