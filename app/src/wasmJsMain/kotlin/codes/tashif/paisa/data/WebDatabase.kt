package codes.tashif.paisa.data

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

// webpack sees `new Worker(new URL(...))` and bundles the worker with its
// SQLite Wasm file. webpack.config.d/sqlite-worker.js resolves the path.
private fun createSqliteWorker(): Worker =
    js("""new Worker(new URL("paisa-sqlite-worker/worker.js", import.meta.url), { type: "module" })""")

/**
 * The web app's database: the same schema, seed and migrations as Android,
 * stored in the browser's origin private file system by sqlite-worker.
 */
fun createWebDatabase(): AppDatabase =
    Room.databaseBuilder<AppDatabase>(name = DATABASE_NAME, factory = { AppDatabaseConstructor.initialize() })
        .setDriver(WebWorkerSQLiteDriver(createSqliteWorker()))
        .withPaisaDefaults()
        .build()
