package sh.taf.billi.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.runtime.Composable
import sh.taf.billi.ui.components.ConnectedToggleGroup
import sh.taf.billi.ui.components.ToggleOption

private val themeModes = listOf(
    ToggleOption("system", "System", Icons.Rounded.AutoAwesome),
    ToggleOption("light", "Light", Icons.Rounded.LightMode),
    ToggleOption("dark", "Dark", Icons.Rounded.DarkMode)
)

/**
 * System, Light and Dark as one connected button group, the same control as
 * the period picker on Analysis. [onSelect] gets the mode id.
 */
@Composable
fun ThemeModePicker(
    selectedId: String,
    onSelect: (String) -> Unit
) {
    ConnectedToggleGroup(
        options = themeModes,
        isChecked = { it == selectedId },
        onClick = { if (it != selectedId) onSelect(it) }
    )
}
