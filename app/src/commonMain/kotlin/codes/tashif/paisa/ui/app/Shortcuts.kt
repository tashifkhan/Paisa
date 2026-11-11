package codes.tashif.paisa.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.AppTab

private val digitKeys = listOf(
    Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine
)

/**
 * Keyboard shortcuts. Single keys only count while nothing that takes text
 * has focus ([typing] false), so they never eat what someone types. Returns
 * true when the key was used.
 */
internal fun handleShortcut(
    event: KeyEvent,
    typing: Boolean,
    tabs: List<AppTab>,
    onTab: (AppTab) -> Unit,
    onAdd: () -> Unit,
    onSearch: () -> Unit,
    onShowShortcuts: () -> Unit
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val command = event.isCtrlPressed || event.isMetaPressed
    if (command && event.key == Key.K) {
        onSearch()
        return true
    }
    if (typing || command) return false
    val digit = digitKeys.indexOf(event.key)
    // The typed character tells "?" from "/" even when Shift isn't reported.
    val typed = event.utf16CodePoint.toChar()
    return when {
        typed == '?' -> { onShowShortcuts(); true }
        digit >= 0 -> tabs.getOrNull(digit)?.let { onTab(it); true } ?: false
        event.key == Key.N -> { onAdd(); true }
        event.key == Key.Slash && event.isShiftPressed -> { onShowShortcuts(); true }
        event.key == Key.Slash -> { onSearch(); true }
        else -> false
    }
}

/** The same shortcuts for keys the browser hands over by name. See [handleShortcut]. */
internal fun handlePageShortcut(
    key: String,
    command: Boolean,
    tabs: List<AppTab>,
    onTab: (AppTab) -> Unit,
    onAdd: () -> Unit,
    onSearch: () -> Unit,
    onShowShortcuts: () -> Unit
): Boolean {
    if (command) {
        if (key.equals("k", ignoreCase = true)) { onSearch(); return true }
        return false
    }
    val digit = key.singleOrNull()?.digitToIntOrNull()
    return when {
        digit != null && digit >= 1 -> tabs.getOrNull(digit - 1)?.let { onTab(it); true } ?: false
        key.equals("n", ignoreCase = true) -> { onAdd(); true }
        key == "?" -> { onShowShortcuts(); true }
        key == "/" -> { onSearch(); true }
        else -> false
    }
}

/** The list behind the ? key and the drawer's shortcuts button. */
@Composable
internal fun ShortcutsDialog(tabs: List<AppTab>, onDismiss: () -> Unit) {
    val rows = buildList {
        add("N" to "Add, on whatever tab is open")
        add("/  or  Ctrl K" to "Search transactions")
        tabs.forEachIndexed { i, tab -> add("${i + 1}" to tab.label) }
        add("Esc" to "Close or go back")
        add("Right click" to "More actions on a transaction")
        add("?" to "This list")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keyboard shortcuts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { (keys, what) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(width = 120.dp, height = 28.dp), contentAlignment = Alignment.CenterStart) {
                            Text(
                                keys,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(what, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

/** What an empty detail pane shows before anything is picked. */
@Composable
internal fun PanePlaceholder(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }
    }
}
