package codes.tashif.paisa.ui.platform

/**
 * A stack of back handlers driven by the browser's history and the Escape
 * key. The newest handler wins, like Android's back dispatcher.
 */
internal object BrowserBack {
    private class Entry(val token: Int, val handler: () -> Unit) {
        /** False once back has used up this entry's history state. */
        var hasHistory = true
    }

    private val handlers = mutableListOf<Entry>()
    private var next = 0
    private var installed = false
    private var ignorePops = 0

    fun push(handler: () -> Unit): Int {
        install()
        val token = next++
        handlers += Entry(token, handler)
        jsPushState()
        return token
    }

    fun remove(token: Int) {
        val index = handlers.indexOfFirst { it.token == token }
        if (index < 0) return
        val entry = handlers.removeAt(index)
        // Drop the history entry this handler added, unless back already did.
        // The screen usually leaves a frame after back runs, so this can't
        // rely on being inside the popstate event.
        if (entry.hasHistory) {
            ignorePops++
            jsBack()
        }
    }

    private fun onBack() {
        if (ignorePops > 0) {
            ignorePops--
            return
        }
        val top = handlers.lastOrNull() ?: return
        top.hasHistory = false
        top.handler()
        // A handler that kept its screen open still needs a history entry,
        // or the next back would leave the app.
        jsAfter(300) {
            if (handlers.contains(top) && !top.hasHistory) {
                top.hasHistory = true
                jsPushState()
            }
        }
    }

    private fun install() {
        if (installed) return
        installed = true
        jsListenPop { onBack() }
        jsListenEscape {
            if (handlers.isNotEmpty()) {
                // Escape goes through history too, so the stack stays in step.
                jsBack()
            }
        }
    }
}

private fun jsAfter(ms: Int, callback: () -> Unit): Unit = js("setTimeout(() => callback(), ms)")

private fun jsPushState(): Unit = js("history.pushState({ paisa: true }, '')")

private fun jsBack(): Unit = js("history.back()")

private fun jsListenPop(callback: () -> Unit): Unit = js("window.addEventListener('popstate', () => callback())")

private fun jsListenEscape(callback: () -> Unit): Unit =
    js("window.addEventListener('keydown', (e) => { if (e.key === 'Escape') callback(); })")
