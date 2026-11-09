package codes.tashif.paisa.ui.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: SmsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scan by viewModel.smsScanProgress.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val unrecognized by viewModel.unrecognizedSms.collectAsState()
    val unrecognizedCount by viewModel.unrecognizedSmsCount.collectAsState()

    BackHandler(onBack = onBack)

    var hasReadSms by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasReceiveSms by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotifications by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
            else ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasReadSms = result[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        hasReceiveSms = result[Manifest.permission.RECEIVE_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifications = result[Manifest.permission.POST_NOTIFICATIONS] == true ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        }
        if (hasReadSms) {
            viewModel.startSmsScan(forceFull = true)
        }
    }

    fun requestPermissions() {
        val perms = buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        onExternalActivityLaunched()
        permissionLauncher.launch(perms.toTypedArray())
    }

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
                            "Paisa reads bank SMS on-device only. Nothing leaves your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                SmsPermissionsSection(
                    hasReadSms = hasReadSms,
                    hasReceiveSms = hasReceiveSms,
                    hasNotifications = hasNotifications,
                    isScanning = scan.isRunning,
                    onGrantAndScan = ::requestPermissions,
                    onScanNew = { viewModel.startSmsScan(forceFull = false) },
                    onFullRescan = { viewModel.startSmsScan(forceFull = true) },
                    onCancelScan = viewModel::cancelSmsScan
                )
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
