package sh.taf.billi.backup

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

internal actual fun backupHttpEngine(): HttpClientEngine = Js.create()
