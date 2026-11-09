package codes.tashif.paisa.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import codes.tashif.paisa.backup.BackupNames
import codes.tashif.paisa.platform.nowMillis
import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.files.File
import kotlin.js.Promise

/**
 * A backup file in the browser. Saving has no picker: [name] becomes the
 * download's file name. Restoring holds the [file] the user picked.
 */
actual class BackupFile(val name: String, val file: File? = null)

actual fun urlHost(url: String): String? = jsUrlHost(url)

@Composable
actual fun rememberBackupLaunchers(viewModel: BackupViewModel): BackupLaunchers = remember(viewModel) {
    BackupLaunchers(
        // BackupScreen shows cloudUnavailableMessage before it gets here.
        connectGoogle = { viewModel.showMessage(WebBackupService.CLOUD_UNAVAILABLE) },
        saveToFile = { viewModel.exportToFile(BackupFile(BackupNames.forTime(nowMillis()))) },
        restoreFromFile = {
            jsPickFile(BackupNames.EXTENSION) { file -> viewModel.restoreFromFile(BackupFile(file.name, file)) }
        },
        openUrl = { url -> jsOpenUrl(url) }
    )
}

/** Starts a browser download of [bytes] named [name]. */
internal fun downloadBytes(name: String, bytes: ByteArray, mime: String) =
    jsDownload(name, bytes.toInt8Array(), mime)

internal suspend fun readBytes(file: File): ByteArray =
    jsReadFile(file).await<Int8Array>().toByteArray()

private fun ByteArray.toInt8Array(): Int8Array {
    val out = Int8Array(size)
    for (i in indices) out[i] = this[i]
    return out
}

private fun Int8Array.toByteArray(): ByteArray = ByteArray(length) { this[it] }

private fun jsUrlHost(url: String): String? = js(
    "(() => { try { const h = new URL(url).hostname; return h ? h : null; } catch (e) { return null; } })()"
)

private fun jsOpenUrl(url: String): Unit = js("{ window.open(url, '_blank', 'noopener'); }")

private fun jsPickFile(accept: String, onPicked: (File) -> Unit): Unit = js(
    """{
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = accept;
      input.addEventListener('change', () => {
        if (input.files && input.files.length > 0) onPicked(input.files[0]);
      });
      input.click();
    }"""
)

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
