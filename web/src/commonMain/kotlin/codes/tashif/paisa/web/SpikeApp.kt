package codes.tashif.paisa.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.shared.NotAPaisaFileException
import codes.tashif.paisa.shared.PaisaContainer
import codes.tashif.paisa.shared.PaisaFile
import codes.tashif.paisa.shared.WrongPasswordException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** A file the user picked in the browser. */
class PickedFile(val name: String, val bytes: ByteArray)

/** What the page needs from the browser, so this file stays common code. */
interface WebPlatform {
    /** Null until the worker answers; throws if the database can't open. */
    suspend fun openDatabase(): WebDatabase

    /** True when the browser has an origin private file system. */
    val hasOpfs: Boolean

    /** Opens the file chooser. Null when the user cancels. */
    suspend fun pickFile(): PickedFile?
}

private val Green = Color(0xFF2E7D32)

@Composable
fun SpikeApp(platform: WebPlatform, dark: Boolean) {
    val colors = if (dark) {
        darkColorScheme(primary = Color(0xFF81C784), secondaryContainer = Color(0xFF1E3A22))
    } else {
        lightColorScheme(primary = Green, secondaryContainer = Color(0xFFDCEFD9))
    }
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Paisa web", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Phase 0 spike. Kotlin/Wasm with Room 3 in your browser's private storage, " +
                            "and the phone's backup and sync files opened with WebCrypto.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SpikeBody(platform)
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun now(): Long = Clock.System.now().toEpochMilliseconds()

@Composable
private fun SpikeBody(platform: WebPlatform) {
    val scope = rememberCoroutineScope()
    var db by remember { mutableStateOf<WebDatabase?>(null) }
    var dbError by remember { mutableStateOf<String?>(null) }
    var launches by remember { mutableStateOf(0) }
    var recent by remember { mutableStateOf<List<OpenedFile>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val opened = platform.openDatabase()
            opened.dao().insertLaunch(Launch(openedAt = now()))
            launches = opened.dao().launchCount()
            recent = opened.dao().recentFiles()
            db = opened
        } catch (e: Throwable) {
            dbError = e.message ?: e.toString()
        }
    }

    Section("Local database") {
        when {
            dbError != null -> Text("Couldn't open the database: $dbError", color = MaterialTheme.colorScheme.error)
            db == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.padding(4.dp))
                Text("Starting SQLite in a web worker…")
            }
            else -> {
                Text(
                    if (launches == 1) "Opened once in this browser. Reload the page and this goes up."
                    else "Opened $launches times in this browser.",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (platform.hasOpfs) "Stored in the origin private file system, so it survives reloads."
                    else "This browser has no private file system. Data lasts until the tab closes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    var picked by remember { mutableStateOf<PickedFile?>(null) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PaisaFile?>(null) }
    var headerLine by remember { mutableStateOf<String?>(null) }
    var openError by remember { mutableStateOf<String?>(null) }

    Section("Open a backup or sync file") {
        Text(
            "Pick a .paisa backup, or a paisa-sync-*.chg file from Google Drive or Nextcloud. " +
                "Nothing leaves this tab.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    platform.pickFile()?.let {
                        picked = it
                        result = null
                        openError = null
                        headerLine = try {
                            val h = PaisaContainer.readHeader(it.bytes)
                            if (h.encrypted) "Encrypted, PBKDF2 ${h.iterations} iterations" else "Not encrypted"
                        } catch (e: NotAPaisaFileException) {
                            openError = e.message
                            null
                        }
                    }
                }
            }) { Text("Choose file") }
            Text(picked?.let { "${it.name} · ${it.bytes.size} bytes" } ?: "No file yet")
        }
        headerLine?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Backup or sync password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = picked != null && !busy,
            onClick = {
                val file = picked ?: return@Button
                busy = true
                openError = null
                scope.launch {
                    try {
                        val parsed = PaisaFile.parse(PaisaContainer.openJson(file.bytes, password.ifEmpty { null }))
                        result = parsed
                        db?.dao()?.let { dao ->
                            dao.insertFile(
                                OpenedFile(
                                    name = file.name,
                                    kind = if (parsed is PaisaFile.SyncChanges) "sync" else "backup",
                                    deviceName = (parsed as? PaisaFile.SyncChanges)?.deviceName,
                                    rows = parsed.tableCounts.values.sum(),
                                    openedAt = now()
                                )
                            )
                            recent = dao.recentFiles()
                        }
                    } catch (e: WrongPasswordException) {
                        openError = "Wrong password, or the file was changed."
                    } catch (e: Throwable) {
                        openError = e.message ?: e.toString()
                    } finally {
                        busy = false
                    }
                }
            }
        ) { Text(if (busy) "Decrypting…" else "Decrypt") }
        openError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        result?.let { FileSummary(it) }
    }

    if (recent.isNotEmpty()) {
        Section("Opened in this browser") {
            recent.forEach { f ->
                Text(
                    "${f.name} · ${f.kind}${f.deviceName?.let { " from $it" } ?: ""} · ${f.rows} rows",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun FileSummary(file: PaisaFile) {
    HorizontalDivider()
    when (file) {
        is PaisaFile.SyncChanges -> {
            Text(
                "Sync changes from ${file.deviceName.ifEmpty { "an unnamed device" }}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${if (file.full) "Full snapshot" else "Delta"} #${file.seq} · device ${file.deviceId} · " +
                    "schema ${file.dbVersion} · ${file.deletedCount} deletions",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        is PaisaFile.Backup -> {
            Text("Backup${file.appVersion?.let { " from Paisa $it" } ?: ""}", style = MaterialTheme.typography.titleMedium)
            Text("Schema ${file.dbVersion}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    file.tableCounts.filterValues { it > 0 }.forEach { (table, count) ->
        Text("$table: $count", fontWeight = FontWeight.Medium)
        file.samples[table].orEmpty().forEach { row ->
            val preview = row.entries.take(5).joinToString(" · ") { (k, v) ->
                "$k=${(v as? JsonPrimitive)?.content ?: v.toString()}"
            }
            Text(
                preview,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
