package sh.taf.billi.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import sh.taf.billi.MainActivity
import sh.taf.billi.statement.AndroidStatementSource
import sh.taf.billi.statement.StatementSource
import java.io.File

/** Copies of files shared to Billi, kept in the cache until the AI has read them. */
internal object SharedFiles {
    const val EXTRA_FILES = "sh.taf.billi.share.FILES"
    const val EXTRA_TEXT = "sh.taf.billi.share.TEXT"

    private const val DIR = "shared"

    /** Same 15 MB cap the AI path puts on a PDF. */
    private const val MAX_BYTES = 15L * 1024 * 1024
    private const val KEEP_MILLIS = 24L * 60 * 60 * 1000

    fun mainIntent(context: Context, files: List<File>, text: String?): Intent =
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_FILES, files.map { it.absolutePath }.toTypedArray())
            text?.let { putExtra(EXTRA_TEXT, it) }
        }

    /**
     * The files and text [intent] carries, or null when it isn't a share.
     * Paths outside the shared folder are ignored, so another app can't
     * point MainActivity at Billi's own files.
     */
    fun read(context: Context, intent: Intent?): Pair<List<StatementSource>, String?>? {
        if (intent == null) return null
        val paths = intent.getStringArrayExtra(EXTRA_FILES)
        val text = intent.getStringExtra(EXTRA_TEXT)
        if (paths == null && text == null) return null
        val root = dir(context).canonicalFile
        val sources = paths.orEmpty()
            .map { File(it).canonicalFile }
            .filter { it.isFile && it.parentFile?.parentFile == root }
            .map { AndroidStatementSource(context.applicationContext, uriFor(context, it)) }
        return sources to text
    }

    /** Copies [uri] under a name that keeps its extension. Null when it can't be read or is too big. */
    fun copy(context: Context, uri: Uri): File? = runCatching {
        val resolver = context.contentResolver
        val name = fileName(context, uri)
        // One folder per file keeps the original name and still avoids clashes.
        val folder = File(dir(context), "${System.currentTimeMillis()}-${uri.hashCode().toUInt()}").apply { mkdirs() }
        val out = File(folder, name)
        val ok = resolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_BYTES) return@use false
                    output.write(buffer, 0, read)
                }
                true
            }
        } ?: false
        if (!ok || out.length() == 0L) {
            folder.deleteRecursively()
            null
        } else {
            out
        }
    }.getOrNull()

    /** Removes copies from earlier shares. */
    fun prune(context: Context) {
        val cutoff = System.currentTimeMillis() - KEEP_MILLIS
        dir(context).listFiles()?.forEach { if (it.lastModified() < cutoff) it.deleteRecursively() }
    }

    private fun dir(context: Context) = File(context.cacheDir, DIR).apply { mkdirs() }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun fileName(context: Context, uri: Uri): String {
        val shown = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')
        val clean = shown.orEmpty()
            .replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_")
            .trim()
            .trimStart('.')
            .take(120)
            .ifEmpty { "shared" }
        if (clean.substringAfterLast('.', "").isNotEmpty() && clean.contains('.')) return clean
        // No extension: take one from the type, since the readers go by name too.
        val ext = context.contentResolver.getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return if (ext != null) "$clean.$ext" else clean
    }
}
