package codes.tashif.paisa.ai.catalog

import java.io.File

/** Small cache files that are replaced whole. */
internal object CacheFiles {

    /**
     * Writes [text] to a temp file of its own next to [file], then renames it
     * over [file]. A failed write leaves the old [file] as it was. Returns
     * whether the new text landed.
     */
    fun writeAtomically(file: File, text: String): Boolean {
        val dir = file.absoluteFile.parentFile ?: return false
        var tmp: File? = null
        return try {
            dir.mkdirs()
            tmp = File.createTempFile("${file.name}.", ".tmp", dir)
            tmp.writeText(text)
            tmp.renameTo(file)
        } catch (_: Exception) {
            false
        } finally {
            tmp?.takeIf { it.exists() }?.delete()
        }
    }
}
