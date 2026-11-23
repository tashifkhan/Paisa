package sh.taf.billi.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Where the app lives on the site: `/app/` on billi.taf.sh, `/` on the dev
 * server. index.html sets the same base for the scripts.
 */
private val basePath: String by lazy { if (jsPathname().startsWith("/app")) "/app/" else "/" }

/** The path the page loaded with, read before the first route rewrites it. */
private val startPath: String by lazy { jsPathname().removePrefix(basePath.trimEnd('/')).trim('/') }

@Composable
actual fun RouteSync(route: AppRoute, onNavigate: (AppRoute) -> Unit) {
    val navigate by rememberUpdatedState(onNavigate)
    var started by remember { mutableStateOf(false) }
    // A reload or a pasted link opens where the address bar points.
    LaunchedEffect(Unit) {
        val wanted = AppRoute.parse(startPath)
        if (wanted != null && wanted != route) navigate(wanted)
        started = true
    }
    // After that the address bar follows the app. This replaces the current
    // history entry rather than adding one: BrowserBack already adds an entry
    // for every screen that back can close, so back keeps working as before.
    LaunchedEffect(route, started) {
        if (started) jsReplacePath(basePath + route.path)
    }
}

private fun jsPathname(): String = js("location.pathname")

private fun jsReplacePath(path: String): Unit =
    js("{ if (location.pathname !== path) history.replaceState(history.state, '', path + location.search + location.hash); }")
