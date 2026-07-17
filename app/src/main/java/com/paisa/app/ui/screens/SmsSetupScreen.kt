package com.paisa.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.paisa.app.data.PaisaViewModel
import com.paisa.app.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSetupScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scan by viewModel.smsScanProgress.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val unrecognized by viewModel.unrecognizedSms.collectAsState()
    val unrecognizedCount by viewModel.unrecognizedSmsCount.collectAsState()

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
        permissionLauncher.launch(perms.toTypedArray())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("SMS Import", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Sms, contentDescription = null)
                            Text(
                                "Paisa reads bank SMS on-device only. Nothing leaves your phone.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Read SMS: ${if (hasReadSms) "Granted" else "Needed"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Receive SMS: ${if (hasReceiveSms) "Granted" else "Needed"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Notifications: ${if (hasNotifications) "Granted" else "Optional"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                if (!hasReadSms) {
                    Button(onClick = { requestPermissions() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant SMS permission & scan")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startSmsScan(forceFull = false) },
                            enabled = !scan.isRunning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Scan new")
                        }
                        OutlinedButton(
                            onClick = { viewModel.startSmsScan(forceFull = true) },
                            enabled = !scan.isRunning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Full rescan")
                        }
                    }
                    if (scan.isRunning) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.cancelSmsScan() },
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
                    Spacer(Modifier.height(8.dp))
                    if (scan.isRunning && scan.total > 0) {
                        LinearProgressIndicator(
                            progress = { scan.processed.toFloat() / scan.total.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (scan.isRunning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (scan.isRunning) {
                            "Processing ${scan.processed} / ${scan.total}"
                        } else {
                            "Last run: ${scan.saved} saved · ${scan.duplicates} duplicates · ${scan.unrecognized} unrecognized"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    settings?.lastSmsScanAt?.takeIf { it > 0 }?.let { ts ->
                        val formatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            .format(Date(ts))
                        Text(
                            "Last scan watermark: $formatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Unrecognized SMS ($unrecognizedCount)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Messages that look financial but no bank parser matched yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }

            if (unrecognized.isEmpty()) {
                item {
                    Text(
                        "None pending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(32.dp))
                }
            } else {
                items(unrecognized, key = { it.id }) { sms ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(Modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                            Text(sms.sender, fontWeight = FontWeight.SemiBold)
                            Text(
                                sms.body.take(180) + if (sms.body.length > 180) "…" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.markUnrecognizedReviewed(sms.id) }) {
                                    Text("Dismiss")
                                }
                                OutlinedButton(onClick = { viewModel.deleteUnrecognized(sms.id) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}
