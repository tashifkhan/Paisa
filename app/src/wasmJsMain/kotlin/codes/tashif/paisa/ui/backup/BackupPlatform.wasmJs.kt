package codes.tashif.paisa.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import codes.tashif.paisa.backup.BackupFile
import codes.tashif.paisa.backup.BackupNames
import codes.tashif.paisa.platform.nowMillis
import org.w3c.files.File

actual fun urlHost(url: String): String? = jsUrlHost(url)

@Composable
actual fun rememberBackupLaunchers(viewModel: BackupViewModel): BackupLaunchers = remember(viewModel) {
    // Load Google sign-in ahead of the click, so its popup isn't blocked.
    jsGoogleClientId()?.takeIf { it.isNotBlank() }?.let { jsLoadGoogle() }
    BackupLaunchers(
        connectGoogle = {
            val clientId = jsGoogleClientId()
            if (clientId.isNullOrBlank()) {
                viewModel.showMessage(
                    "Google Drive on the web needs a Google OAuth client ID. " +
                        "Put it in the paisa-google-client-id meta tag in index.html."
                )
            } else {
                jsGoogleToken(
                    clientId,
                    onToken = { token -> viewModel.onGoogleAuthorized(token) },
                    onError = { message -> viewModel.showMessage(message) }
                )
            }
        },
        saveToFile = { viewModel.exportToFile(BackupFile(BackupNames.forTime(nowMillis()))) },
        restoreFromFile = {
            jsPickFile(BackupNames.EXTENSION) { file -> viewModel.restoreFromFile(BackupFile(file.name, file)) }
        },
        openUrl = { url -> jsOpenUrl(url) }
    )
}

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

/** The web OAuth client ID, from `<meta name="paisa-google-client-id">`. */
private fun jsGoogleClientId(): String? =
    js("document.querySelector('meta[name=\"paisa-google-client-id\"]')?.content || null")

/**
 * Google Identity Services token flow: loads the library once, then shows
 * Google's popup for `drive.appdata` and `email`. Must run from a click.
 */
private fun jsGoogleToken(clientId: String, onToken: (String) -> Unit, onError: (String) -> Unit): Unit = js(
    """{
    const request = () => {
      const client = google.accounts.oauth2.initTokenClient({
        client_id: clientId,
        scope: 'https://www.googleapis.com/auth/drive.appdata email',
        callback: (r) => {
          if (r && r.access_token) onToken(r.access_token);
          else onError((r && (r.error_description || r.error)) || 'Google sign-in was cancelled');
        },
        error_callback: (e) => onError((e && e.message) || 'Google sign-in was cancelled'),
      });
      client.requestAccessToken({ prompt: '' });
    };
    if (window.google && google.accounts && google.accounts.oauth2) { request(); return; }
    const s = document.createElement('script');
    s.src = 'https://accounts.google.com/gsi/client';
    s.onload = request;
    s.onerror = () => onError("Couldn't load Google sign-in");
    document.head.appendChild(s);
    }"""
)

private fun jsLoadGoogle(): Unit = js(
    """{
    if (!document.getElementById('paisa-gsi')) {
      const s = document.createElement('script');
      s.id = 'paisa-gsi';
      s.src = 'https://accounts.google.com/gsi/client';
      s.async = true;
      document.head.appendChild(s);
    }
    }"""
)
