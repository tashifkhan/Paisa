package sh.taf.billi.ui.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sh.taf.billi.data.Account
import sh.taf.billi.notifications.AccountLink
import sh.taf.billi.notifications.NotificationSourceApp
import sh.taf.billi.ui.components.SettingsSectionLabel
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing

/** The account an app's payments go to by default, shown under the app's name. */
internal fun linkedName(app: NotificationSourceApp, links: List<AccountLink>, accounts: List<Account>): String? {
    val default = links.firstOrNull { it.packageName == app.packageName && it.last4 == null } ?: return null
    return accounts.firstOrNull { it.id == default.accountId }?.name
}

/**
 * Where one app's payments go: a default account, or "ask each time", which
 * leaves a payment in the review list unless its account digits are linked.
 * Digit links ("••1234 → HDFC") come from the review list and can be removed here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppAccountSheet(
    app: NotificationSourceApp,
    links: List<AccountLink>,
    accounts: List<Account>,
    onSetDefault: (accountId: Int?) -> Unit,
    onRemoveLink: (last4: String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberHaptics()
    val defaultId = links.firstOrNull { it.last4 == null }?.accountId
    val digitLinks = links.filter { it.last4 != null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.medium)
        ) {
            Text("${app.name} pays from", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Used when an alert doesn't name the account digits.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp).padding(top = MaterialTheme.spacing.small)) {
                item(key = "ask") {
                    Choice(
                        title = "Ask each time",
                        subtitle = "Payments wait for you to pick",
                        selected = defaultId == null,
                        onClick = {
                            haptics.click()
                            onSetDefault(null)
                        }
                    )
                }
                items(accounts, key = { it.id }) { account ->
                    Choice(
                        title = account.name,
                        subtitle = listOfNotNull(account.type, account.accountLast4?.let { "••$it" }).joinToString(" · "),
                        selected = defaultId == account.id,
                        onClick = {
                            haptics.click()
                            onSetDefault(account.id)
                        }
                    )
                }
                if (digitLinks.isNotEmpty()) {
                    item(key = "digits") { SettingsSectionLabel("By account digits") }
                    items(digitLinks, key = { "d:${it.last4}" }) { link ->
                        ListItem(
                            headlineContent = { Text("••${link.last4}") },
                            supportingContent = {
                                Text(accounts.firstOrNull { it.id == link.accountId }?.name ?: "A deleted account")
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    haptics.reject()
                                    onRemoveLink(link.last4!!)
                                }) {
                                    Icon(Icons.Rounded.LinkOff, contentDescription = "Unlink ••${link.last4}")
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Choice(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    )
}
