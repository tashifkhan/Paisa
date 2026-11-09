package codes.tashif.paisa.ui.app

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.haptics.rememberHaptics

/**
 * The side rail that replaces the bottom bar on tablets, unfolded foldables
 * and phones in landscape. Same tabs and label setting as [AppNavBar].
 */
@Composable
fun AppNavRail(
    navTabs: List<AppTab>,
    currentTab: AppTab,
    showLabels: Boolean,
    onSelect: (AppTab) -> Unit
) {
    val haptics = rememberHaptics()
    NavigationRail {
        // Items sit in the middle of the rail, within thumb reach on a tablet
        // held by its sides.
        Spacer(Modifier.weight(1f))
        navTabs.forEach { tab ->
            NavigationRailItem(
                selected = currentTab == tab,
                onClick = {
                    if (currentTab != tab) haptics.tick()
                    onSelect(tab)
                },
                icon = {
                    Icon(tab.icon, contentDescription = if (showLabels) null else tab.label)
                },
                label = { Text(tab.label) },
                alwaysShowLabel = showLabels
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
