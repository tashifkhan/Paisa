package codes.tashif.paisa.ui.statement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import codes.tashif.paisa.ai.ExtractedTransaction
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * Review step: summary, target account, the extracted rows (likely matches tucked
 * behind "Show more"), and the import and cancel buttons. The screen owns the
 * picked account and menu state so they survive leaving and re-entering preview.
 */
@Composable
internal fun StatementPreviewContent(
    preview: StatementUiState.Preview,
    accounts: List<Account>,
    currency: String,
    selectedAccountId: Int?,
    onSelectAccount: (Int) -> Unit,
    accountMenuExpanded: Boolean,
    onAccountMenuExpandedChange: (Boolean) -> Unit,
    showMatches: Boolean,
    onToggleShowMatches: () -> Unit,
    onUpdateRows: (List<ExtractedTransaction>) -> Unit,
    onImport: (accountId: Int?) -> Unit,
    onCancel: () -> Unit
) {
    val haptics = rememberHaptics()
    val s = preview
    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
        ?: accounts.firstOrNull()
    val selectedCount = s.rows.count { it.selected }
    val regularRows = s.rows.withIndex().filterNot { it.value.likelyDuplicate }
    val matchingRows = s.rows.withIndex().filter { it.value.likelyDuplicate }
    val allRegularRowsSelected = regularRows.isNotEmpty() &&
        regularRows.all { it.value.selected }

    fun toggleRow(indexedRow: IndexedValue<ExtractedTransaction>) {
        val row = indexedRow.value
        haptics.toggle(on = !row.selected)
        val updated = s.rows.toMutableList()
        updated[indexedRow.index] = row.copy(selected = !row.selected)
        onUpdateRows(updated)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StatementPreviewSummary(
            preview = s,
            selectedCount = selectedCount,
            matchCount = matchingRows.size,
            allRegularRowsSelected = allRegularRowsSelected,
            onToggleShown = {
                haptics.toggle(on = !allRegularRowsSelected)
                val updated = s.rows.map { row ->
                    if (row.likelyDuplicate) row
                    else row.copy(selected = !allRegularRowsSelected)
                }
                onUpdateRows(updated)
            }
        )

        Spacer(Modifier.height(MaterialTheme.spacing.small))

        ImportAccountPicker(
            accounts = accounts,
            selectedAccount = selectedAccount,
            expanded = accountMenuExpanded,
            onExpandedChange = onAccountMenuExpandedChange,
            onSelect = onSelectAccount
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            itemsIndexed(regularRows, key = { _, indexedRow ->
                val row = indexedRow.value
                "${row.date}-${row.amount}-${row.merchant}-${indexedRow.index}"
            }) { _, indexedRow ->
                StatementPreviewRow(
                    row = indexedRow.value,
                    currency = currency,
                    onToggle = { toggleRow(indexedRow) }
                )
            }

            if (matchingRows.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = {
                            haptics.tick()
                            onToggleShowMatches()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showMatches) {
                                "Show less"
                            } else {
                                "Show more (${matchingRows.size} " +
                                    if (matchingRows.size == 1) "match)" else "matches)"
                            }
                        )
                    }
                }
            }

            if (showMatches) {
                itemsIndexed(matchingRows, key = { _, indexedRow ->
                    val row = indexedRow.value
                    "match-${row.date}-${row.amount}-${row.merchant}-${indexedRow.index}"
                }) { _, indexedRow ->
                    StatementPreviewRow(
                        row = indexedRow.value,
                        currency = currency,
                        onToggle = { toggleRow(indexedRow) }
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            Button(
                onClick = {
                    haptics.confirm()
                    onImport(selectedAccount?.id)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCount > 0
            ) {
                Text("Import selected ($selectedCount)")
            }
            OutlinedButton(
                onClick = {
                    haptics.reject()
                    onCancel()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
