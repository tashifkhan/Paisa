package codes.tashif.paisa.ui.accounts

import codes.tashif.paisa.ui.home.DayHeader
import codes.tashif.paisa.ui.components.TransactionGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import codes.tashif.paisa.ui.platform.VerticalScrollbarFor
import codes.tashif.paisa.ui.platform.handCursor
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.ui.components.EmptyState
import codes.tashif.paisa.ui.components.TransactionRow
import codes.tashif.paisa.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountCardsView(
    accounts: List<Account>,
    transactionsByAccount: Map<Int, List<TransactionWithDetails>>,
    currency: String,
    onOpenActions: (Account) -> Unit,
    onOpenTransaction: (Int) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 760.dp) {
            AccountCardsWide(accounts, transactionsByAccount, currency, onOpenActions, onOpenTransaction)
        } else {
            AccountCarousel(accounts, transactionsByAccount, currency, onOpenActions, onOpenTransaction)
        }
    }
}

/**
 * Wide panes: every account in a grid you can click, and the picked one's
 * transactions beside it. A carousel only moves by swiping, which a mouse
 * can't do.
 */
@Composable
private fun AccountCardsWide(
    accounts: List<Account>,
    transactionsByAccount: Map<Int, List<TransactionWithDetails>>,
    currency: String,
    onOpenActions: (Account) -> Unit,
    onOpenTransaction: (Int) -> Unit
) {
    var selectedId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    val current = accounts.firstOrNull { it.id == selectedId } ?: accounts.firstOrNull()
    val gridState = rememberLazyGridState()
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1.2f).fillMaxHeight()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 250.dp),
                state = gridState,
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.small,
                    top = MaterialTheme.spacing.small,
                    bottom = MaterialTheme.spacing.extraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(accounts, key = { _, a -> a.id }) { index, account ->
                    val selected = account.id == current?.id
                    AccountCarouselItem(
                        account = account,
                        currency = currency,
                        index = index,
                        onEdit = { onOpenActions(account) },
                        modifier = Modifier
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.shapes.extraLarge
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { selectedId = account.id }
                            .handCursor()
                    )
                }
            }
            VerticalScrollbarFor(gridState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            AccountTransactions(current, transactionsByAccount, currency, onOpenTransaction)
        }
    }
}

@Composable
private fun AccountTransactions(
    account: Account?,
    transactionsByAccount: Map<Int, List<TransactionWithDetails>>,
    currency: String,
    onOpenTransaction: (Int) -> Unit
) {
    val accountTx = account?.let { transactionsByAccount[it.id] }.orEmpty()
    Text(
        text = account?.let { "${it.name} · Transactions" } ?: "Transactions",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(
            horizontal = MaterialTheme.spacing.medium,
            vertical = MaterialTheme.spacing.extraSmall
        )
    )
    if (accountTx.isEmpty()) {
        EmptyState(
            title = "Nothing here yet",
            subtitle = "Transactions for this account will appear here"
        )
    } else {
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.extraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                accountTx.groupBy { it.transactionDate.take(10) }.forEach { (day, dayTxs) ->
                    item(key = "h$day") {
                        DayHeader(
                            day = day,
                            currency = currency,
                            spent = dayTxs.filter { it.type == "expense" }.sumOf { it.amount }
                        )
                    }
                    item(key = "g$day") {
                        TransactionGroup(dayTxs) { tx, position ->
                            TransactionRow(
                                tx = tx,
                                currency = currency,
                                position = position,
                                onClick = { onOpenTransaction(tx.id) }
                            )
                        }
                    }
                }
            }
            VerticalScrollbarFor(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCarousel(
    accounts: List<Account>,
    transactionsByAccount: Map<Int, List<TransactionWithDetails>>,
    currency: String,
    onOpenActions: (Account) -> Unit,
    onOpenTransaction: (Int) -> Unit
) {
    // M3 multi-browse carousel — same layout as m3.material.io / Compose samples:
    // large focal item + smaller peeks that morph size on scroll, with maskClip.
    val carouselState = rememberCarouselState { accounts.size }
    val currentItem = carouselState.currentItem
    val currentAccount = accounts.getOrNull(currentItem)
    val accountTx = currentAccount?.let { transactionsByAccount[it.id] }.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 280.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = MaterialTheme.spacing.small, bottom = MaterialTheme.spacing.small),
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { page ->
            val account = accounts[page]
            AccountCarouselItem(
                account = account,
                currency = currency,
                index = page,
                onEdit = { onOpenActions(account) },
                modifier = Modifier
                    .height(205.dp)
                    .maskClip(MaterialTheme.shapes.extraLarge)
            )
        }

        Text(
            text = currentAccount?.let { "${it.name} · Transactions" } ?: "Transactions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.extraSmall
            )
        )

        if (accountTx.isEmpty()) {
            EmptyState(
                title = "Nothing here yet",
                subtitle = "Transactions for this account will appear here"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.extraLarge
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                accountTx.groupBy { it.transactionDate.take(10) }.forEach { (day, dayTxs) ->
                    item(key = "h$day") {
                        DayHeader(
                            day = day,
                            currency = currency,
                            spent = dayTxs.filter { it.type == "expense" }.sumOf { it.amount }
                        )
                    }
                    item(key = "g$day") {
                        TransactionGroup(dayTxs) { tx, position ->
                            TransactionRow(
                                tx = tx,
                                currency = currency,
                                position = position,
                                onClick = { onOpenTransaction(tx.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
