package codes.tashif.paisa.ui.statement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.ui.theme.spacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StatementExtractingContent(
    completed: Int,
    total: Int,
    currentFile: Int,
    totalFiles: Int,
    onContinueInBackground: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // M3 loading indicator — preferred replacement for indeterminate circular progress.
        LoadingIndicator()
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        Text(
            "Extracting transactions…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        if (total > 0) {
            LinearWavyProgressIndicator(
                progress = { completed.toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth(0.75f)
            )
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            Text(
                text = if (totalFiles > 1) {
                    "File $currentFile of $totalFiles · chunk $completed of $total"
                } else {
                    "Chunk $completed of $total"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(0.75f))
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            Text(
                text = if (totalFiles > 1) {
                    "File $currentFile of $totalFiles · reading…"
                } else {
                    "Reading file…"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = "You can leave this screen — we'll notify you when it's ready.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        OutlinedButton(onClick = onContinueInBackground) {
            Text("Continue in background")
        }
    }
}
