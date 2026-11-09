package codes.tashif.paisa.ui.accounts

import androidx.compose.foundation.layout.Column
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.app.CenteredContent
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.PaisaTopBar
import codes.tashif.paisa.ui.haptics.rememberHaptics

@Composable
fun AccountsScreen(viewModel: AccountsViewModel = koinViewModel()) {
    val accounts by viewModel.accounts.collectAsState()
    val transactionsByAccount by viewModel.transactionsByAccount.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currency ?: "₹"
    var accountToRename by remember { mutableStateOf<Account?>(null) }
    var accountToUpdateBalance by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var accountForActions by remember { mutableStateOf<Account?>(null) }
    var listView by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var showMergeDialog by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(
            title = "Accounts",
            actions = {
                IconButton(onClick = {
                    haptics.toggle(on = !listView)
                    listView = !listView
                    selectedIds = emptySet()
                }) {
                    Icon(
                        imageVector = if (listView) {
                            Icons.Rounded.ViewCarousel
                        } else {
                            Icons.AutoMirrored.Rounded.ViewList
                        },
                        contentDescription = if (listView) "Card view" else "List view"
                    )
                }
            }
        )

        if (accounts.isEmpty()) {
            EmptyState(
                title = "No accounts",
                subtitle = "Accounts appear here after your first SMS scan or import"
            )
            return@Column
        }

        if (listView) {
            CenteredContent {
            AccountListView(
                accounts = accounts,
                currency = currency,
                selectedIds = selectedIds,
                onToggleSelect = { id ->
                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                },
                onOpenActions = { accountForActions = it },
                onReorder = { viewModel.reorderAccounts(it) },
                onClearSelection = { selectedIds = emptySet() },
                onMergeClick = { showMergeDialog = true },
                onAutoMerge = { viewModel.autoMergeDuplicateAccounts() }
            )
            }
        } else {
            AccountCardsView(
                accounts = accounts,
                transactionsByAccount = transactionsByAccount,
                currency = currency,
                onOpenActions = { accountForActions = it },
                onOpenTransaction = { viewModel.openTransaction(it) }
            )
        }
    }

    accountToRename?.let { account ->
        RenameAccountDialog(
            account = account,
            onDismiss = { accountToRename = null },
            onSave = { newName ->
                viewModel.renameAccount(account, newName)
                accountToRename = null
            }
        )
    }

    accountForActions?.let { account ->
        AccountActionsSheet(
            account = account,
            onDismiss = { accountForActions = null },
            onRename = {
                accountForActions = null
                accountToRename = account
            },
            onUpdateBalance = {
                accountForActions = null
                accountToUpdateBalance = account
            },
            onSetDefault = {
                accountForActions = null
                viewModel.setDefaultAccount(account)
            },
            onDelete = {
                accountForActions = null
                accountToDelete = account
            }
        )
    }

    accountToUpdateBalance?.let { account ->
        UpdateBalanceDialog(
            account = account,
            onDismiss = { accountToUpdateBalance = null },
            onSave = { balance ->
                viewModel.updateAccountBalance(account, balance)
                accountToUpdateBalance = null
            }
        )
    }

    accountToDelete?.let { account ->
        DeleteAccountDialog(
            account = account,
            onDismiss = { accountToDelete = null },
            onConfirm = {
                viewModel.deleteAccountCascade(account)
                accountToDelete = null
            }
        )
    }

    if (showMergeDialog && selectedIds.size >= 2) {
        MergeAccountsDialog(
            accounts = accounts.filter { it.id in selectedIds },
            currency = currency,
            onDismiss = { showMergeDialog = false },
            onMerge = { targetId ->
                viewModel.mergeAccounts(
                    targetId = targetId,
                    sourceIds = selectedIds.filter { it != targetId }
                )
                selectedIds = emptySet()
                showMergeDialog = false
            }
        )
    }
}
