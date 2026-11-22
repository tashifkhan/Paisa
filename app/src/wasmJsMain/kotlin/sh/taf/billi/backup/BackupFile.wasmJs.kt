package sh.taf.billi.backup

import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import org.w3c.files.File
import kotlin.js.Promise

/**
 * A backup file in the browser. Saving has no picker: [name] becomes the
 * download's file name. Restoring holds the [file] the user picked.
 */
actual class BackupFile(val name: String, val file: File? = null)

/** Starts a browser download of [bytes] named [name]. */
internal fun downloadBytes(name: String, bytes: ByteArray, mime: String) =
    jsDownload(name, bytes.toInt8Array(), mime)

internal suspend fun readBytes(file: File): ByteArray =
    jsReadFile(file).await<Int8Array>().toByteArray()

private fun jsDownload(name: String, data: Int8Array, mime: String): Unit = js(
    """{
      const url = URL.createObjectURL(new Blob([data], { type: mime }));
      const a = document.createElement('a');
      a.href = url;
      a.download = name;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    }"""
)

private fun jsReadFile(file: File): Promise<Int8Array> =
    js("file.arrayBuffer().then(b => new Int8Array(b))")
