package codes.tashif.paisa.ui.statement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.ui.theme.spacing

/** Selected count, where the rows came from, unreadable files, and Select or Clear shown. */
@Composable
internal fun StatementPreviewSummary(
    preview: StatementUiState.Preview,
    selectedCount: Int,
    matchCount: Int,
    allRegularRowsSelected: Boolean,
    onToggleShown: () -> Unit
) {
    val s = preview
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                text = "$selectedCount of ${s.rows.size} selected",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    s.filesProcessed > 1 ->
                        "Parsed from ${s.filesProcessed} files " +
                            "(${s.chunksProcessed} chunks)"
                    s.chunksProcessed > 1 ->
                        "Parsed from ${s.chunksProcessed} statement chunks"
                    else -> "Review rows, then import into an account"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (s.failedFiles.isNotEmpty()) {
                Text(
                    text = "Could not read: " +
                        s.failedFiles.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (matchCount > 0) {
                Text(
                    text = "$matchCount possible " +
                        "${if (matchCount == 1) "match was" else "matches were"} " +
                        "set aside",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onToggleShown) {
                    Text(if (allRegularRowsSelected) "Clear shown" else "Select shown")
                }
            }
        }
    }
}
