package codes.tashif.paisa.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmsSetupScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scan by viewModel.smsScanProgress.collectAsState()
    val haptics = rememberHaptics()
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
        viewModel.onExternalActivityLaunched()
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
                Text(
                    "Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(MaterialTheme.spacing.smaller))
                PermissionRow(label = "Read SMS", granted = hasReadSms, requiredHint = "Needed")
                PermissionRow(label = "Receive SMS", granted = hasReceiveSms, requiredHint = "Needed")
                PermissionRow(
                    label = "Notifications",
                    granted = hasNotifications,
                    requiredHint = "Optional"
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                if (!hasReadSms) {
                    Button(onClick = {
                        haptics.click()
                        requestPermissions()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant SMS permission & scan")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
                    ) {
                        Button(
                            onClick = {
                                haptics.click()
                                viewModel.startSmsScan(forceFull = false)
                            },
                            enabled = !scan.isRunning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Scan new")
                        }
                        OutlinedButton(
                            onClick = {
                                haptics.click()
                                viewModel.startSmsScan(forceFull = true)
                            },
                            enabled = !scan.isRunning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Full rescan")
                        }
                    }
                    if (scan.isRunning) {
                        TextButton(
                            onClick = {
                                haptics.reject()
                                viewModel.cancelSmsScan()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel scan")
                        }
                    }
                }
            }

            if (scan.isRunning || scan.saved > 0 || scan.processed > 0) {
                item {
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
                    settings?.lastSmsScanAt?.takeIf { it > 0 }?.let { ts ->
                        val formatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            .format(Date(ts))
                        Text(
                            "Last scan watermark: $formatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                            Text(sms.sender, fontWeight = FontWeight.SemiBold)
                            Text(
                                sms.body.take(180) + if (sms.body.length > 180) "…" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(
                                    MaterialTheme.spacing.smaller,
                                    Alignment.End
                                )
                            ) {
                                TextButton(
                                    onClick = {
                                        haptics.reject()
                                        viewModel.deleteUnrecognized(sms.id)
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Delete")
                                }
                                OutlinedButton(
                                    onClick = {
                                        haptics.click()
                                        viewModel.markUnrecognizedReviewed(sms.id)
                                    }
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    requiredHint: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) {
                Icons.Rounded.CheckCircle
            } else {
                Icons.Rounded.RadioButtonUnchecked
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (granted) "Granted" else requiredHint,
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
