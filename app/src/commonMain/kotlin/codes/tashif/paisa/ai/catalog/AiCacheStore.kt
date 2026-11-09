package codes.tashif.paisa.ai.catalog

/**
 * Where the model catalogue keeps its small cache files, each replaced whole.
 * Android uses files in noBackupFilesDir/ai; the web uses localStorage.
 */
interface AiCacheStore {
    /** The text saved under [name], or null when there is none. */
    fun read(name: String): String?

    /** Replaces [name] with [text]. A failed write keeps the old text. Returns whether the new text landed. */
    fun write(name: String, text: String): Boolean
}
