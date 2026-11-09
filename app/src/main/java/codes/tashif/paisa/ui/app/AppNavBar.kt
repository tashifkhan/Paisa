package codes.tashif.paisa.ui.app

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.components.FloatingNavBar
import codes.tashif.paisa.ui.components.NavTab
import codes.tashif.paisa.ui.haptics.rememberHaptics

/** The bottom bar, floating or standard, over [navTabs]. */
@Composable
fun AppNavBar(
    navTabs: List<AppTab>,
    currentTab: AppTab,
    floating: Boolean,
    showLabels: Boolean,
    onSelect: (AppTab) -> Unit
) {
    val haptics = rememberHaptics()
    val tabs = navTabs.map { NavTab(it.label, it.icon) }
    // A tab opened from More isn't in the bar; nothing shows as selected then.
    val selectedIndex = navTabs.indexOf(currentTab)
    val select: (Int) -> Unit = { index ->
        if (selectedIndex != index) haptics.tick()
        onSelect(navTabs[index])
    }
    if (floating) {
        FloatingNavBar(tabs = tabs, selected = selectedIndex, onSelect = select)
    } else {
        NavigationBar {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { select(index) },
                    // A hidden label isn't composed, so name the icon or
                    // TalkBack reads an unlabeled button.
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = if (showLabels) null else tab.label
                        )
                    },
                    label = { Text(tab.label) },
                    // Off: only the selected tab is labeled.
                    alwaysShowLabel = showLabels
                )
            }
        }
    }
}
