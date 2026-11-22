package sh.taf.billi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sh.taf.billi.data.TransactionWithDetails

/**
 * One day's transactions joined into a single block, rounded at the ends
 * and tight in between, the way More groups its rows.
 */
@Composable
fun TransactionGroup(
    transactions: List<TransactionWithDetails>,
    row: @Composable (tx: TransactionWithDetails, position: GroupPosition) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        transactions.forEachIndexed { index, tx ->
            key(tx.id) { row(tx, groupPositionOf(index, transactions.size)) }
        }
    }
}
