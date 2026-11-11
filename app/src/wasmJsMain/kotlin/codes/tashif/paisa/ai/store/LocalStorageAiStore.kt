package codes.tashif.paisa.ai.store

import kotlinx.browser.localStorage

/**
 * [AiKeyValueStore] in the browser's localStorage, each key under "[prefix]/".
 *
 * Not encrypted: API keys saved here sit in plain text in this browser's
 * storage for this site, readable by anything that runs on the page or has
 * access to the browser profile.
 */
class LocalStorageAiStore(private val prefix: String) : AiKeyValueStore {

    private fun item(key: String) = "$prefix/$key"

    override fun getString(key: String): String? = localStorage.getItem(item(key))

    override fun contains(key: String): Boolean = localStorage.getItem(item(key)) != null

    override fun strings(): Map<String, String> {
        val start = "$prefix/"
        val out = LinkedHashMap<String, String>()
        for (i in 0 until localStorage.length) {
            val name = localStorage.key(i) ?: continue
            if (!name.startsWith(start)) continue
            localStorage.getItem(name)?.let { out[name.removePrefix(start)] = it }
        }
        return out
    }

    /** False when the browser refuses the write, for example when storage is full or blocked. */
    override fun commit(changes: Map<String, String?>): Boolean = try {
        changes.forEach { (key, value) ->
            if (value == null) localStorage.removeItem(item(key)) else localStorage.setItem(item(key), value)
        }
        true
    } catch (e: Throwable) {
        false
    }
}
