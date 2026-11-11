package codes.tashif.paisa.ai.catalog

import android.content.Context
import java.io.File

/** Cache files in [dir], written atomically by [CacheFiles]. */
class FileAiCacheStore(private val dir: File) : AiCacheStore {

    /** noBackupFilesDir/ai, so the cache survives restarts and never goes into a backup. */
    constructor(context: Context) : this(File(context.noBackupFilesDir, "ai"))

    override fun read(name: String): String? =
        runCatching { File(dir, name).takeIf { it.exists() }?.readText() }.getOrNull()

    override fun write(name: String, text: String): Boolean = CacheFiles.writeAtomically(File(dir, name), text)
}
