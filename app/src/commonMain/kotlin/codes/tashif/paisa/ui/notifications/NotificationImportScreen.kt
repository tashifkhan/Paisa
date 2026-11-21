package codes.tashif.paisa.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import codes.tashif.paisa.ai.local.LocalAiStatus
import codes.tashif.paisa.ai.store.MessageReaderMode
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Block
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import codes.tashif.paisa.notifications.InstalledNotificationApp
import codes.tashif.paisa.notifications.AwaitingAccount
import codes.tashif.paisa.ui.format.formatMoneyAmount
import codes.tashif.paisa.notifications.NotificationSourceApp
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.GroupPosition
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsItem
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.platform.PlatformBackHandler
import codes.tashif.paisa.ui.theme.spacing
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings for reading transactions from bank and UPI app notifications: the
 * master switch, notification access, one switch per app, an app picker for
 * apps that aren't built in, and the on-device parser fallback.
 * [onExternalActivityLaunched] runs before the system settings screen opens,
 * so the app lock doesn't trip on the resulting onStop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationImportScreen(
    onBack: () -> Unit,
    onExternalActivityLaunched: () -> Unit = {},
    viewModel: NotificationImportViewModel = koinViewModel(),
    approvalViewModel: AccountApprovalViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val aiStatus by viewModel.localAiStatus.collectAsState()
    val aiConfig by viewModel.aiConfigState.collectAsState()
    val cloudReady by viewModel.cloudReady.collectAsState()
    val haptics = rememberHaptics()
    val awaiting by approvalViewModel.awaiting.collectAsState()
    val accounts by approvalViewModel.choices.collectAsState()
    val currency by approvalViewModel.currency.collectAsState()
    var choosing by remember { mutableStateOf<AwaitingAccount?>(null) }
    var linkingApp by remember { mutableStateOf<NotificationSourceApp?>(null) }

    var pickerOpen by remember { mutableStateOf(false) }
    var loadingApps by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledNotificationApp>>(emptyList()) }
    var appQuery by remember { mutableStateOf("") }
    var pickedApp by remember { mutableStateOf<InstalledNotificationApp?>(null) }
    var removingApp by remember { mutableStateOf<NotificationSourceApp?>(null) }

    PlatformBackHandler(onBack = onBack)
    // Access is granted in system settings, so check again on the way back.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshAccess()
        onPauseOrDispose { }
    }
    LaunchedEffect(pickerOpen) {
        if (pickerOpen) {
            loadingApps = true
            installedApps = viewModel.installedApps()
            loadingApps = false
        }
    }

    val openAccess = {
        onExternalActivityLaunched()
        viewModel.openAccessSettings()
    }
    val upiApps = state.apps.filter { !it.isBank }
    val bankApps = state.apps.filter { it.isBank }
    val onCount = state.apps.count { it.enabled }
    val knownPackages = state.apps.map { it.packageName }.toSet()
    val available = installedApps
        .filter { it.packageName !in knownPackages }
        .filter {
            appQuery.isBlank() ||
                it.name.contains(appQuery, ignoreCase = true) ||
                it.packageName.contains(appQuery, ignoreCase = true)
        }

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "Payment notifications", onBack = onBack)

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
            if (!state.supported) {
                item {
                    SettingsItem(
                        title = "Needs the Android app",
                        subtitle = "Browsers can't read other apps' notifications.",
                        icon = Icons.Rounded.PhoneAndroid,
                        position = GroupPosition.Single
                    )
                }
                return@LazyColumn
            }

            item {
                SettingsGroup {
                    SettingsItem(
                        title = "Read payment notifications",
                        subtitle = if (state.enabled) {
                            "On for $onCount of ${state.apps.size} apps"
                        } else {
                            "Off. Paisa reads nothing from other apps."
                        },
                        icon = Icons.Rounded.NotificationsActive,
                        position = groupPositionOf(0, 2),
                        onClick = { viewModel.setEnabled(!state.enabled) },
                        trailing = {
                            Switch(
                                checked = state.enabled,
                                onCheckedChange = {
                                    haptics.toggle(on = it)
                                    viewModel.setEnabled(it)
                                }
                            )
                        }
                    )
                    SettingsItem(
                        title = "Notification access",
                        subtitle = if (state.hasAccess) {
                            "Allowed in system settings"
                        } else {
                            "Tap to allow Paisa in system settings"
                        },
                        icon = Icons.Rounded.Shield,
                        position = groupPositionOf(1, 2),
                        iconContainerColor = if (state.hasAccess) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        iconContentColor = if (state.hasAccess) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = openAccess,
                        trailing = {
                            if (state.hasAccess) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Allowed",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    "Needed",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }

            if (awaiting.isNotEmpty()) {
                item {
                    SettingsSectionLabel("Pick an account (${awaiting.size})")
                    SettingsGroup {
                        awaiting.forEachIndexed { index, item ->
                            val tx = item.transaction
                            SettingsItem(
                                title = "${formatMoneyAmount(currency, tx.amount)} · ${tx.merchantName ?: "Unknown"}",
                                subtitle = listOfNotNull(
                                    item.appName,
                                    item.last4?.let { "••$it" },
                                    tx.transactionDate.take(10)
                                ).joinToString(" · "),
                                icon = Icons.Rounded.AccountBalanceWallet,
                                position = groupPositionOf(index, awaiting.size),
                                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                onClick = { choosing = item },
                                trailing = { Text("Choose", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                            )
                        }
                    }
                    Note("Keep “Always use” on to send that app's future payments there too.")
                }
            }

            item {
                Note("Only the apps switched on below are read. A payment that also arrives by SMS is saved once.")
                if (!state.hasAccess) {
                    Note("Blocked as a restricted setting? App info → ⋮ → Allow restricted settings.")
                }
            }

            item {
                SettingsSectionLabel("Unclear messages")
                ReaderChoices(
                    mode = aiConfig.messageReader,
                    onDevice = aiStatus,
                    cloudReady = cloudReady,
                    onPick = { haptics.click(); viewModel.setMessageReader(it) }
                )
                if (aiConfig.messageReader == MessageReaderMode.CLOUD) {
                    CloudWarning(cloudReady)
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        SettingsSectionLabel("Apps ($onCount on)")
                    }
                    TextButton(
                        onClick = {
                            haptics.click()
                            viewModel.setAllAppsEnabled(true)
                        },
                        enabled = onCount < state.apps.size
                    ) { Text("All on") }
                    TextButton(
                        onClick = {
                            haptics.click()
                            viewModel.setAllAppsEnabled(false)
                        },
                        enabled = onCount > 0
                    ) { Text("All off") }
                }
            }

            item {
                AppGroup(
                    label = "UPI apps",
                    apps = upiApps,
                    linkedName = { linkedName(it, state.links, accounts) },
                    onOpen = { linkingApp = it },
                    onToggle = viewModel::setAppEnabled,
                    onRemove = { removingApp = it }
                )
            }
            item {
                AppGroup(
                    label = "Bank apps",
                    apps = bankApps,
                    linkedName = { linkedName(it, state.links, accounts) },
                    onOpen = { linkingApp = it },
                    onToggle = viewModel::setAppEnabled,
                    onRemove = { removingApp = it }
                )
            }

            item {
                SettingsSectionLabel("Missing an app?")
                SettingsGroup {
                    SettingsItem(
                        title = "Add a bank or UPI app",
                        subtitle = "Pick an installed app that isn't in the list above",
                        icon = Icons.Rounded.Add,
                        position = GroupPosition.Single,
                        onClick = {
                            appQuery = ""
                            pickerOpen = true
                        }
                    )
                }
            }
        }
    }

    if (pickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { pickerOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = MaterialTheme.spacing.medium)
            ) {
                Text(
                    "Add an app",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Text(
                    "Any installed app that posts payment alerts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = appQuery,
                    onValueChange = { appQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.small),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Search installed apps") }
                )
                if (loadingApps) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (available.isEmpty()) {
                    Text(
                        if (appQuery.isBlank()) {
                            "Every app installed here is already listed, or there's nothing to add."
                        } else {
                            "No installed app matches \"$appQuery\"."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.medium)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).padding(top = MaterialTheme.spacing.small)) {
                        items(available, key = { it.packageName }) { app ->
                            ListItem(
                                headlineContent = { Text(app.name) },
                                supportingContent = { Text(app.packageName) },
                                leadingContent = {
                                    Icon(Icons.Rounded.PhoneAndroid, contentDescription = null)
                                },
                                modifier = Modifier.clickable {
                                    pickedApp = app
                                    pickerOpen = false
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    pickedApp?.let { app ->
        AlertDialog(
            onDismissRequest = { pickedApp = null },
            title = { Text("Add ${app.name}") },
            text = {
                Text("Bank apps try that bank's SMS rules first.")
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.addApp(app.packageName, app.name, isBank = false)
                            pickedApp = null
                        }
                    ) { Text("UPI app") }
                    TextButton(
                        onClick = {
                            viewModel.addApp(app.packageName, app.name, isBank = true)
                            pickedApp = null
                        }
                    ) { Text("Bank app") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pickedApp = null }) { Text("Cancel") }
            }
        )
    }

    choosing?.let { item ->
        ChooseAccountSheet(
            item = item,
            accounts = accounts,
            currency = currency,
            onChoose = { account, always ->
                approvalViewModel.assign(item, account, always)
                choosing = null
            },
            onDismiss = { choosing = null }
        )
    }

    linkingApp?.let { app ->
        AppAccountSheet(
            app = app,
            links = state.links.filter { it.packageName == app.packageName },
            accounts = accounts,
            onSetDefault = { viewModel.setLink(app.packageName, null, it) },
            onRemoveLink = { viewModel.setLink(app.packageName, it, null) },
            onDismiss = { linkingApp = null }
        )
    }

    removingApp?.let { app ->
        AlertDialog(
            onDismissRequest = { removingApp = null },
            title = { Text("Stop reading ${app.name}?") },
            text = {
                Text("Paisa stops reading its notifications. You can add it again later.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeApp(app.packageName)
                        removingApp = null
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { removingApp = null }) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun AppGroup(
    label: String,
    apps: List<NotificationSourceApp>,
    linkedName: (NotificationSourceApp) -> String?,
    onOpen: (NotificationSourceApp) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (NotificationSourceApp) -> Unit
) {
    if (apps.isEmpty()) return
    val haptics = rememberHaptics()
    SettingsSectionLabel(label)
    SettingsGroup {
        apps.forEachIndexed { index, app ->
            SettingsItem(
                title = app.name,
                subtitle = listOfNotNull(
                    linkedName(app)?.let { "→ $it" },
                    "Added by you".takeIf { app.custom },
                    "Not installed".takeIf { !app.installed }
                ).joinToString(" · ").ifEmpty { null },
                icon = if (app.isBank) Icons.Rounded.AccountBalance else Icons.Rounded.PhoneAndroid,
                position = groupPositionOf(index, apps.size),
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onOpen(app) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = app.enabled,
                            onCheckedChange = {
                                haptics.toggle(on = it)
                                onToggle(app.packageName, it)
                            }
                        )
                        if (app.custom) {
                            IconButton(onClick = { onRemove(app) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove ${app.name}"
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

/** Off, Gemini Nano on this phone, or a cloud model, for messages the rules can't read. */
@Composable
private fun ReaderChoices(
    mode: MessageReaderMode,
    onDevice: LocalAiStatus,
    cloudReady: Boolean,
    onPick: (MessageReaderMode) -> Unit
) {
    val rows = listOf(
        Triple(MessageReaderMode.OFF, "Off", "Unreadable messages wait in review"),
        Triple(
            MessageReaderMode.ON_DEVICE,
            "On-device",
            when {
                !onDevice.supported -> "Not available on this phone"
                onDevice.ready -> "Gemini Nano · nothing leaves the phone"
                onDevice.downloadable -> "Download Gemini Nano in AI → Local AI"
                else -> "Gemini Nano · ${onDevice.skipReason ?: "not ready"}"
            }
        ),
        Triple(
            MessageReaderMode.CLOUD,
            "Cloud",
            if (cloudReady) "Your SMS and alerts model in AI" else "Needs a cloud key in AI"
        )
    )
    SettingsGroup {
        rows.forEachIndexed { index, (value, title, subtitle) ->
            val available = value != MessageReaderMode.ON_DEVICE || onDevice.supported
            SettingsItem(
                title = title,
                subtitle = subtitle,
                icon = when (value) {
                    MessageReaderMode.OFF -> Icons.Rounded.Block
                    MessageReaderMode.ON_DEVICE -> Icons.Rounded.PhoneAndroid
                    MessageReaderMode.CLOUD -> Icons.Rounded.Cloud
                },
                position = groupPositionOf(index, rows.size),
                modifier = if (available) Modifier else Modifier.alpha(0.5f),
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { onPick(value) },
                trailing = { RadioButton(selected = mode == value, onClick = null, enabled = available) }
            )
        }
    }
}

@Composable
private fun CloudWarning(cloudReady: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.small)
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                buildString {
                    append("The message text is sent to your AI provider. Slower than the rules, and may cost per message.")
                    if (!cloudReady) append(" Add a cloud key in AI to start.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(start = MaterialTheme.spacing.small)
            )
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.small,
            end = MaterialTheme.spacing.small,
            top = MaterialTheme.spacing.smaller
        )
    )
}
