package codes.tashif.paisa.ai.catalog

import kotlinx.browser.localStorage

/** The catalogue cache in localStorage, each file under "paisa_ai_cache/". */
class LocalStorageAiCacheStore : AiCacheStore {

    override fun read(name: String): String? = localStorage.getItem(PREFIX + name)

    /** False when the browser refuses the write, for example when storage is full. */
    override fun write(name: String, text: String): Boolean = try {
        localStorage.setItem(PREFIX + name, text)
        true
    } catch (e: Throwable) {
        false
    }

    private companion object {
        const val PREFIX = "paisa_ai_cache/"
    }
}
