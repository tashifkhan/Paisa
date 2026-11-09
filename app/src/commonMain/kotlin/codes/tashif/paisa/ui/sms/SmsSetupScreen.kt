package codes.tashif.paisa.ui.sms

import codes.tashif.paisa.ui.platform.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.theme.spacing

/**
 * [onExternalActivityLaunched] runs right before the system permission dialog
 * opens, so the app lock doesn't trip on the resulting onStop.
 */
@Composable
fun SmsSetupScreen(
    onBack: () -> Unit,
    onExternalActivityLaunched: () -> Unit = {},
    viewModel: SmsViewModel = koinViewModel()
) {
    val scan by viewModel.smsScanProgress.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val unrecognized by viewModel.unrecognizedSms.collectAsState()
    val unrecognizedCount by viewModel.unrecognizedSmsCount.collectAsState()

    PlatformBackHandler(onBack = onBack)

    val permissions = rememberSmsPermissions(
        onExternalActivityLaunched = onExternalActivityLaunched,
        onReadSmsGranted = { viewModel.startSmsScan(forceFull = true) }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "SMS Import", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.extraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            if (smsImportSupported) {
                                "Paisa reads bank SMS on-device only. Nothing leaves your phone."
                            } else {
                                SmsViewModel.SMS_UNSUPPORTED_MESSAGE
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (smsImportSupported) {
                item {
                    SmsPermissionsSection(
                        hasReadSms = permissions.hasReadSms,
                        hasReceiveSms = permissions.hasReceiveSms,
                        hasNotifications = permissions.hasNotifications,
                        isScanning = scan.isRunning,
                        onGrantAndScan = permissions::request,
                        onScanNew = { viewModel.startSmsScan(forceFull = false) },
                        onFullRescan = { viewModel.startSmsScan(forceFull = true) },
                        onCancelScan = viewModel::cancelSmsScan
                    )
                }
            }

            if (scan.isRunning || scan.saved > 0 || scan.processed > 0) {
                item {
                    SmsScanProgressSection(
                        scan = scan,
                        lastSmsScanAt = settings?.lastSmsScanAt
                    )
                }
            }

            item {
                Spacer(Modifier.height(MaterialTheme.spacing.smaller))
                Text(
                    "Unrecognized SMS ($unrecognizedCount)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Messages that look financial but no bank parser matched yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (unrecognized.isEmpty()) {
                item {
                    Text(
                        "None pending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(unrecognized, key = { it.id }) { sms ->
                    UnrecognizedSmsCard(
                        sms = sms,
                        onDelete = { viewModel.deleteUnrecognized(sms.id) },
                        onDismiss = { viewModel.markUnrecognizedReviewed(sms.id) }
                    )
                }
            }
        }
    }
}
