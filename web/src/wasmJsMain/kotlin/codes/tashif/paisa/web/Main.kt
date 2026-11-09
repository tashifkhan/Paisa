package codes.tashif.paisa.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import kotlinx.browser.document
import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.Worker
import kotlin.js.Promise

// webpack sees `new Worker(new URL(...))` and bundles the worker with its
// SQLite Wasm file.
private fun createSqliteWorker(): Worker =
    js("""new Worker(new URL("paisa-sqlite-worker/worker.js", import.meta.url), { type: "module" })""")

private fun browserHasOpfs(): Boolean =
    js("""!!(globalThis.navigator && navigator.storage && navigator.storage.getDirectory)""")

private fun prefersDark(): Boolean =
    js("""!!(globalThis.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches)""")

/** Asks the browser not to evict our storage under pressure. Best effort. */
private fun requestPersistentStorage(): Unit =
    js("""{ if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {}); }""")

private external interface JsPickedFile : JsAny {
    val name: String
    val bytes: Int8Array
}

private fun jsPickFile(): Promise<JsPickedFile?> = js(
    """
    new Promise(resolve => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = '.paisa,.chg,application/octet-stream';
      input.onchange = () => {
        const f = input.files && input.files[0];
        if (!f) { resolve(null); return; }
        f.arrayBuffer().then(b => resolve({ name: f.name, bytes: new Int8Array(b) }));
      };
      input.oncancel = () => resolve(null);
      input.click();
    })
    """
)

private object BrowserPlatform : WebPlatform {
    private var database: WebDatabase? = null

    override val hasOpfs: Boolean = browserHasOpfs()

    override suspend fun openDatabase(): WebDatabase =
        database ?: Room.databaseBuilder<WebDatabase>(name = "paisa-web.db")
            .setDriver(WebWorkerSQLiteDriver(createSqliteWorker()))
            .build()
            .also { database = it }

    override suspend fun pickFile(): PickedFile? {
        val picked = jsPickFile().await<JsPickedFile?>() ?: return null
        val bytes = picked.bytes
        return PickedFile(picked.name, ByteArray(bytes.length) { bytes[it] })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    requestPersistentStorage()
    ComposeViewport(document.body!!) {
        SpikeApp(BrowserPlatform, dark = prefersDark())
    }
}
