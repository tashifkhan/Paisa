package codes.tashif.paisa.ui.backup

import androidx.compose.runtime.Composable

/** A file the user picked to save a backup into or restore one from. */
expect class BackupFile

/** Host part of [url], or null when it has none. */
expect fun urlHost(url: String): String?

/** Screen actions that leave the app: Google sign-in, file pickers and the browser. */
class BackupLaunchers(
    val connectGoogle: () -> Unit,
    val saveToFile: () -> Unit,
    val restoreFromFile: () -> Unit,
    val openUrl: (String) -> Unit
)

/** Results go back to [viewModel]. */
@Composable
expect fun rememberBackupLaunchers(viewModel: BackupViewModel): BackupLaunchers
