package codes.tashif.paisa.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.platform.handCursor
import kotlinx.coroutines.launch

/**
 * A small info button that opens a rich tooltip: a title, a few lines on
 * what something means, and an optional action. Tap it on a phone; hover or
 * click it with a mouse. It stays open until tapped away, so there's time
 * to read it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTip(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            RichTooltip(
                title = { Text(title) },
                action = if (actionLabel != null && onAction != null) {
                    {
                        TextButton(onClick = {
                            state.dismiss()
                            onAction()
                        }) { Text(actionLabel) }
                    }
                } else {
                    null
                }
            ) { Text(text) }
        },
        state = state,
        modifier = modifier
    ) {
        IconButton(
            onClick = { scope.launch { state.show() } },
            modifier = Modifier.size(32.dp).handCursor()
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "About $title",
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
