package codes.tashif.paisa.ui.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.ui.theme.spacing

/**
 * The status card at the top of SMS import: what Paisa is doing right now,
 * live progress while a scan runs, and the last run's numbers as tiles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmsStatusCard(
    supported: Boolean,
    allowed: Boolean,
    scan: SmsScanProgress,
    lastSmsScanAt: Long?
) {
    val hasRun = scan.saved > 0 || scan.processed > 0 || scan.duplicates > 0 || scan.unrecognized > 0
    Surface(
        modifier = Modifier.fillMaxWidth(),
        // Same lopsided corners as the Home balance hero.
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        val onCard = MaterialTheme.colorScheme.onPrimaryContainer
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (scan.isRunning) {
                        LoadingIndicator(modifier = Modifier.size(48.dp), color = onCard)
                    } else {
                        Surface(
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 18.dp),
                            color = onCard.copy(alpha = 0.12f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Sms, contentDescription = null, tint = onCard)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(MaterialTheme.spacing.medium))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            !supported -> "SMS import isn't available here"
                            scan.isRunning -> "Scanning your inbox"
                            allowed -> "Watching for bank SMS"
                            else -> "SMS import is off"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onCard
                    )
                    Text(
                        text = when {
                            !supported -> SmsViewModel.SMS_UNSUPPORTED_MESSAGE
                            scan.isRunning && scan.total > 0 -> "${scan.processed} of ${scan.total} messages"
                            scan.isRunning -> "Counting messages…"
                            else -> lastSmsScanAt?.takeIf { it > 0 }
                                ?.let { "Last scan ${formatDate(it, "d MMM, h:mm a")}" }
                                ?: "Paisa reads messages on this phone. No server sees them."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = onCard.copy(alpha = 0.8f)
                    )
                }
            }

            if (scan.isRunning) {
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                if (scan.total > 0) {
                    LinearWavyProgressIndicator(
                        progress = { scan.processed.toFloat() / scan.total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = onCard,
                        trackColor = onCard.copy(alpha = 0.16f)
                    )
                } else {
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = onCard,
                        trackColor = onCard.copy(alpha = 0.16f)
                    )
                }
            }

            if (supported && (hasRun || scan.isRunning)) {
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatPill("Saved", scan.saved, onCard, Modifier.weight(1f))
                    StatPill("Duplicates", scan.duplicates, onCard, Modifier.weight(1f))
                    StatPill("Not matched", scan.unrecognized, onCard, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: Int, tint: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = tint.copy(alpha = 0.10f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint.copy(alpha = 0.75f), maxLines = 1)
        }
    }
}
