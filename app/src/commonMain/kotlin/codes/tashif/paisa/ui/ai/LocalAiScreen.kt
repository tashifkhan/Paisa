package codes.tashif.paisa.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ai.local.LocalAiCompatibility
import codes.tashif.paisa.ai.local.LocalAiFeature
import codes.tashif.paisa.ai.local.LocalAiState
import codes.tashif.paisa.ai.local.LocalAiStatus
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.theme.spacing
import org.koin.compose.viewmodel.koinViewModel

/** Everything about the on-device model: status, download, limits and a test. */
@Composable
fun LocalAiScreen(
    onBack: () -> Unit,
    viewModel: LocalAiViewModel = koinViewModel()
) {
    val status by viewModel.status.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val test by viewModel.test.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "Local AI", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            LocalStatusCard(status)
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            LocalActions(status, busy, viewModel)
            test?.let {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                AiResultCard(it)
            }
            if (status.supported) {
                SettingsSectionLabel("On this phone")
                SettingsGroup {
                    LocalFact(
                        title = "Text and photos",
                        subtitle = "Notes, receipts and statement text · about 4,000 tokens a call",
                        icon = Icons.Rounded.TextFields,
                        position = groupPositionOf(0, 3)
                    )
                    LocalFact(
                        title = "No PDFs or voice notes",
                        subtitle = "Cloud models in the ladder handle these",
                        icon = Icons.Rounded.PictureAsPdf,
                        position = groupPositionOf(1, 3)
                    )
                    LocalFact(
                        title = "Only while Paisa is open",
                        subtitle = "Android blocks it in the background",
                        icon = Icons.Rounded.Lock,
                        position = groupPositionOf(2, 3)
                    )
                }
            } else {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                NoteCard(text = "Works on: ${LocalAiCompatibility.SUMMARY}", icon = Icons.Rounded.Smartphone)
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                NoteCard(text = "A cloud model still works here. Add a key under AI → Providers.", icon = Icons.Rounded.Info)
            }
            if (status.modelName?.contains("demo") == true) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                NoteCard(
                    text = "Debug demo engine. No real model runs.",
                    icon = Icons.Rounded.ErrorOutline,
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun LocalStatusCard(status: LocalAiStatus) {
    val accent = when (status.state) {
        LocalAiState.AVAILABLE -> MaterialTheme.colorScheme.primary
        LocalAiState.DOWNLOADABLE, LocalAiState.DOWNLOADING -> MaterialTheme.colorScheme.tertiary
        LocalAiState.ERROR, LocalAiState.UNSUPPORTED -> MaterialTheme.colorScheme.onSurfaceVariant
        LocalAiState.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp).size(28.dp)
                    )
                }
                Column(Modifier.padding(start = MaterialTheme.spacing.small)) {
                    Text(
                        text = status.modelName?.let { "Gemini Nano · $it" } ?: "Gemini Nano",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stateLabel(status),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                }
            }
            status.detail?.let {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (status.state == LocalAiState.DOWNLOADING) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                val progress = status.progress
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        drawStopIndicator = {},
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    downloadLabel(status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (status.features.isNotEmpty()) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = status.features
                        .map(LocalAiFeature::label)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LocalActions(
    status: LocalAiStatus,
    busy: Boolean,
    viewModel: LocalAiViewModel
) {
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        when {
            status.downloadable -> Button(
                onClick = viewModel::download,
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(MaterialTheme.spacing.extraSmall))
                Text(if (busy) "Starting…" else "Download model")
            }
            status.state == LocalAiState.DOWNLOADING -> Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.weight(1f)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(MaterialTheme.spacing.extraSmall))
                Text("Downloading…")
            }
            status.ready -> FilledTonalButton(
                onClick = viewModel::test,
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(MaterialTheme.spacing.extraSmall))
                Text(if (busy) "Testing…" else "Test on-device model")
            }
            else -> OutlinedButton(
                onClick = viewModel::refresh,
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(MaterialTheme.spacing.extraSmall))
                Text("Check again")
            }
        }
        if (status.ready) {
            TextButton(onClick = viewModel::refresh, enabled = !busy) { Text("Refresh") }
        }
    }
}

@Composable
private fun LocalFact(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    position: codes.tashif.paisa.ui.components.GroupPosition
) {
    SettingsItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        position = position,
        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

private fun stateLabel(status: LocalAiStatus): String = when (status.state) {
    LocalAiState.CHECKING -> "Checking this phone…"
    LocalAiState.AVAILABLE -> "Ready on this phone"
    LocalAiState.DOWNLOADABLE -> "Available to download"
    LocalAiState.DOWNLOADING -> "Downloading"
    LocalAiState.UNSUPPORTED -> "Not available on this phone"
    LocalAiState.ERROR -> "Couldn't start"
}

private fun downloadLabel(status: LocalAiStatus): String {
    val done = megabytes(status.bytesDownloaded)
    val total = status.bytesToDownload?.let(::megabytes)
    return if (total != null) "$done of $total" else done
}

private fun megabytes(bytes: Long): String =
    if (bytes < 1024 * 1024) "${bytes / 1024} KB" else "${bytes / (1024 * 1024)} MB"
