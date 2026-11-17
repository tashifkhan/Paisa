package codes.tashif.paisa.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import codes.tashif.paisa.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The target for "Share to Paisa". Copies what was shared into the app's
 * cache, hands the copies to [MainActivity] in Paisa's own task, and closes.
 *
 * Copying first matters. A share only grants read access to this activity
 * for as long as it lives, and the main app may sit behind the lock screen
 * for a while before it reads the files.
 */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity already forwarded its share.
        if (savedInstanceState != null) {
            finish()
            return
        }
        val uris = sharedUris(intent)
        val text = sharedText(intent, hasFiles = uris.isNotEmpty())
        lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) {
                SharedFiles.prune(applicationContext)
                uris.mapNotNull { SharedFiles.copy(applicationContext, it) }
            }
            if (files.isNotEmpty() || !text.isNullOrBlank()) {
                startActivity(SharedFiles.mainIntent(this@ShareActivity, files, text))
            }
            finish()
        }
    }

    private fun sharedUris(intent: Intent): List<Uri> {
        val out = mutableListOf<Uri>()
        when (intent.action) {
            Intent.ACTION_SEND -> intent.stream()?.let(out::add)
            Intent.ACTION_SEND_MULTIPLE -> out += intent.streamList()
        }
        // Some apps only put the files in ClipData.
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let { if (it !in out) out += it }
        }
        return out.take(MAX_FILES)
    }

    /**
     * Shared text becomes the AI note. With files, a subject or caption
     * that's just the file name adds nothing, so it's dropped.
     */
    private fun sharedText(intent: Intent, hasFiles: Boolean): String? {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()
        if (text.isNullOrEmpty()) return null
        if (hasFiles && !text.contains(' ') && text.contains('.')) return null
        return text.take(MAX_TEXT_CHARS)
    }

    @Suppress("DEPRECATION")
    private fun Intent.stream(): Uri? =
        if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else getParcelableExtra(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun Intent.streamList(): List<Uri> =
        if (Build.VERSION.SDK_INT >= 33) getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        else getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()

    private companion object {
        const val MAX_FILES = 10
        const val MAX_TEXT_CHARS = 4_000
    }
}
