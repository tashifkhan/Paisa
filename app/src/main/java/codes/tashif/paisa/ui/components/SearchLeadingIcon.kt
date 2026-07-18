package codes.tashif.paisa.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Leading control for search fields:
 * - empty query → search icon
 * - non-empty → X that clears the query (not a trailing clear button)
 */
@Composable
fun SearchLeadingIcon(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (query.isEmpty()) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = modifier,
            tint = tint
        )
    } else {
        IconButton(onClick = onClear, modifier = modifier) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Clear search",
                tint = tint
            )
        }
    }
}
