package codes.tashif.paisa.ui.sms

import codes.tashif.paisa.platform.formatDate
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.ui.theme.spacing

/** Live or last-run scan numbers. Emits its children without a wrapper, like [SmsPermissionsSection]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmsScanProgressSection(
    scan: SmsScanProgress,
    lastSmsScanAt: Long?
) {
    Text(
        "Scan progress",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(MaterialTheme.spacing.smaller))
    if (scan.isRunning && scan.total > 0) {
        LinearWavyProgressIndicator(
            progress = { scan.processed.toFloat() / scan.total.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
    } else if (scan.isRunning) {
        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(MaterialTheme.spacing.smaller))
    Text(
        if (scan.isRunning) {
            "Processing ${scan.processed} / ${scan.total}"
        } else {
            "Last run: ${scan.saved} saved · ${scan.duplicates} duplicates · " +
                "${scan.unrecognized} unrecognized"
        },
        style = MaterialTheme.typography.bodyMedium
    )
    lastSmsScanAt?.takeIf { it > 0 }?.let { ts ->
        val formatted = formatDate(ts, "dd MMM yyyy, HH:mm")
        Text(
            "Last scan watermark: $formatted",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
