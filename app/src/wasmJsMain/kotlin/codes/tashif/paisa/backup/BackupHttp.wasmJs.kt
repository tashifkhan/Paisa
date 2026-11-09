package codes.tashif.paisa.backup

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

internal actual fun backupHttpEngine(): HttpClientEngine = Js.create()
