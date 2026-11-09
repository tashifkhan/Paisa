package codes.tashif.paisa.ui.platform

/**
 * A stack of back handlers driven by the browser's history and the Escape
 * key. The newest handler wins, like Android's back dispatcher.
 */
internal object BrowserBack {
    private val handlers = mutableListOf<Pair<Int, () -> Unit>>()
    private var next = 0
    private var installed = false
    private var ignorePops = 0

    fun push(handler: () -> Unit): Int {
        install()
        val token = next++
        handlers += token to handler
        jsPushState()
        return token
    }

    fun remove(token: Int) {
        val index = handlers.indexOfFirst { it.first == token }
        if (index < 0) return
        handlers.removeAt(index)
        // Drop the history entry this handler added, unless back already did.
        if (!handlingPop) {
            ignorePops++
            jsBack()
        }
    }

    private var handlingPop = false

    private fun onBack() {
        if (ignorePops > 0) {
            ignorePops--
            return
        }
        val top = handlers.lastOrNull() ?: return
        handlingPop = true
        try {
            top.second()
        } finally {
            handlingPop = false
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

private fun jsPushState(): Unit = js("history.pushState({ paisa: true }, '')")

private fun jsBack(): Unit = js("history.back()")

private fun jsListenPop(callback: () -> Unit): Unit = js("window.addEventListener('popstate', () => callback())")

private fun jsListenEscape(callback: () -> Unit): Unit =
    js("window.addEventListener('keydown', (e) => { if (e.key === 'Escape') callback(); })")
