package codes.tashif.paisa.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.data.NavTabs
import codes.tashif.paisa.ui.app.icon
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * Navbar style toggles and the list of tabs shown in it. A ColumnScope
 * extension so AnimatedVisibility keeps its vertical expand animation.
 */
@Composable
fun ColumnScope.NavBarSettings(
    floatingNavBar: Boolean,
    navBarLabels: Boolean,
    navTabs: String,
    onFloatingNavBarChange: (Boolean) -> Unit,
    onNavBarLabelsChange: (Boolean) -> Unit,
    onNavTabsChange: (List<AppTab>) -> Unit
) {
    val haptics = rememberHaptics()

    Text(
        text = "Navigation bar",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.extraSmall,
            bottom = MaterialTheme.spacing.smaller
        )
    )
    AppearanceToggleCard(
        icon = Icons.Rounded.CallToAction,
        title = "Floating navbar",
        subtitle = "Pill-shaped bar that floats above the bottom edge",
        checked = floatingNavBar,
        onCheckedChange = {
            haptics.toggle(on = it)
            onFloatingNavBarChange(it)
        }
    )
    // The floating bar always labels just the selected tab.
    AnimatedVisibility(visible = !floatingNavBar) {
        Column {
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            AppearanceToggleCard(
                icon = Icons.AutoMirrored.Rounded.Label,
                title = "Navbar labels",
                subtitle = "Show a label under every tab, not just the selected one",
                checked = navBarLabels,
                onCheckedChange = {
                    haptics.toggle(on = it)
                    onNavBarLabelsChange(it)
                }
            )
        }
    }

    Spacer(Modifier.height(MaterialTheme.spacing.small))
    Text(
        text = "Tabs in the navbar",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.extraSmall,
            bottom = MaterialTheme.spacing.smaller
        )
    )
    Text(
        text = "Home and More always stay. Up to ${NavTabs.MAX} tabs fit; the rest open from More.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.extraSmall,
            bottom = MaterialTheme.spacing.smaller
        )
    )
    val shownTabs = NavTabs.parse(navTabs)
    AppTab.entries.filter { it !in NavTabs.REQUIRED }.forEach { tab ->
        val on = tab in shownTabs
        val full = !on && shownTabs.size >= NavTabs.MAX
        AppearanceToggleCard(
            icon = tab.icon,
            title = tab.label,
            subtitle = when {
                on -> "In the navbar"
                full -> "Navbar is full. Turn another tab off first"
                else -> "Opens from More"
            },
            checked = on,
            onCheckedChange = { want ->
                if (want && full) {
                    haptics.reject()
                } else {
                    haptics.toggle(on = want)
                    onNavTabsChange(NavTabs.toggle(shownTabs, tab, want))
                }
            }
        )
        Spacer(Modifier.height(MaterialTheme.spacing.smaller))
    }
}
