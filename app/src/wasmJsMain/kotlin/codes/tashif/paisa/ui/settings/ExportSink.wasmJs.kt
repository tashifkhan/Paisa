package codes.tashif.paisa.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.khronos.webgl.Int8Array
import org.khronos.webgl.set

private fun jsDownload(name: String, mime: String, bytes: Int8Array): Unit = js(
    """{
    const url = URL.createObjectURL(new Blob([bytes], { type: mime }));
    const a = document.createElement('a');
    a.href = url; a.download = name;
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
    }"""
)

/** Collects the export and hands it to the browser as a download on close. */
private class DownloadSink(private val name: String, private val mime: String) : ExportSink {
    private val parts = mutableListOf<ByteArray>()
    private var done = false

    override suspend fun write(bytes: ByteArray) {
        parts += bytes
    }

    override fun close() {
        if (done) return
        done = true
        val all = parts.fold(ByteArray(0)) { acc, b -> acc + b }
        val array = Int8Array(all.size)
        for (i in all.indices) array[i] = all[i]
        jsDownload(name, mime, array)
    }
}

@Composable
actual fun rememberCreateDocument(
    mimeType: String,
    onSink: (sink: ExportSink?, error: String?) -> Unit
): (fileName: String) -> Unit = remember(mimeType) {
    { name -> onSink(DownloadSink(name, mimeType), null) }
}
