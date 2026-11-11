package codes.tashif.paisa.ui.sms

import codes.tashif.paisa.ui.platform.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.GroupPosition
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
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
        DetailHeader(title = "SMS import", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.extraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                SmsStatusCard(
                    supported = smsImportSupported,
                    allowed = permissions.hasReadSms && permissions.hasReceiveSms,
                    scan = scan,
                    lastSmsScanAt = settings?.lastSmsScanAt
                )
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
                        onCancelScan = viewModel::cancelSmsScan,
                        onMarkScanned = viewModel::markInboxScanned
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        SettingsSectionLabel(
                            if (unrecognizedCount > 0) "Not recognized ($unrecognizedCount)" else "Not recognized"
                        )
                    }
                    if (unrecognizedCount > 0) {
                        TextButton(
                            onClick = viewModel::dismissAllUnrecognized,
                            modifier = Modifier.padding(top = MaterialTheme.spacing.smaller)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Dismiss all")
                        }
                    }
                }
                Text(
                    "These look like payments, but no bank format matched. Dismiss keeps them out of " +
                        "this list; delete removes them from Paisa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = MaterialTheme.spacing.small,
                        end = MaterialTheme.spacing.small,
                        bottom = MaterialTheme.spacing.smaller
                    )
                )
            }

            if (unrecognized.isEmpty()) {
                item {
                    SettingsItem(
                        title = "All caught up",
                        subtitle = "Nothing waiting for review",
                        icon = Icons.Rounded.DoneAll,
                        position = GroupPosition.Single,
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            } else {
                itemsIndexed(unrecognized, key = { _, sms -> sms.id }) { index, sms ->
                    UnrecognizedSmsCard(
                        sms = sms,
                        position = groupPositionOf(index, unrecognized.size),
                        onDelete = { viewModel.deleteUnrecognized(sms.id) },
                        onDismiss = { viewModel.markUnrecognizedReviewed(sms.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}
