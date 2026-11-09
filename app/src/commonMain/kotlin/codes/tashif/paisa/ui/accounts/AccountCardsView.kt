package codes.tashif.paisa.ui.accounts

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
import androidx.compose.runtime.Composable
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
                items(accountTx, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        currency = currency,
                        onClick = { onOpenTransaction(tx.id) }
                    )
                }
            }
        }
    }
}
