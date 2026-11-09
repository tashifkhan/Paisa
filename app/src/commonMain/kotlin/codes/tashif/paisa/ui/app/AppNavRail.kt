package codes.tashif.paisa.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.sms.SmsScanService
import org.koin.compose.koinInject

/** What the add and rescan buttons at the top of the rail or drawer need. */
class NavActions(
    val scanRunning: Boolean,
    val onRescanSms: () -> Unit,
    val onOpenSmsSetup: () -> Unit,
    val onAdd: (AddSheet) -> Unit,
    val onShowShortcuts: () -> Unit
)

/**
 * The side rail that replaces the bottom bar on tablets, unfolded foldables
 * and phones in landscape. Same tabs and label setting as [AppNavBar]. The
 * add button sits at the top of the rail, where Material puts it, instead of
 * floating over the content.
 */
@Composable
fun AppNavRail(
    navTabs: List<AppTab>,
    currentTab: AppTab,
    showLabels: Boolean,
    actions: NavActions,
    onSelect: (AppTab) -> Unit
) {
    val haptics = rememberHaptics()
    NavigationRail(
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                NavTooltip(addLabelFor(currentTab)) {
                    FloatingActionButton(
                        onClick = {
                            haptics.click()
                            actions.onAdd(addSheetFor(currentTab))
                        },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = addLabelFor(currentTab))
                    }
                }
                if (currentTab == AppTab.HOME) RescanButton(actions)
            }
        }
    ) {
        // Items sit in the middle of the rail, within thumb reach on a tablet
        // held by its sides.
        Spacer(Modifier.weight(1f))
        navTabs.forEachIndexed { index, tab ->
            NavTooltip("${tab.label}  ${index + 1}") {
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
        }
        Spacer(Modifier.weight(1f))
    }
}

/**
 * The always-open drawer on laptops, desktops and big tablets in landscape.
 * Every tab is listed with its shortcut, so nothing hides behind More.
 */
@Composable
fun AppNavDrawer(
    currentTab: AppTab,
    actions: NavActions,
    onSelect: (AppTab) -> Unit
) {
    val haptics = rememberHaptics()
    PermanentDrawerSheet(
        modifier = Modifier.width(248.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp)
        ) {
            Text(
                "Paisa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 16.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 4.dp, bottom = 20.dp)
            ) {
                NavTooltip("${addLabelFor(currentTab)}  N") {
                    ExtendedFloatingActionButton(
                        onClick = {
                            haptics.click()
                            actions.onAdd(addSheetFor(currentTab))
                        },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(addLabelFor(currentTab)) },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        modifier = Modifier.semantics { contentDescription = addLabelFor(currentTab) }
                    )
                }
                if (currentTab == AppTab.HOME) RescanButton(actions)
            }
            AppTab.entries.forEachIndexed { index, tab ->
                NavigationDrawerItem(
                    selected = currentTab == tab,
                    onClick = {
                        if (currentTab != tab) haptics.tick()
                        onSelect(tab)
                    },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label) },
                    badge = {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                )
            }
            Spacer(Modifier.weight(1f))
            NavigationDrawerItem(
                selected = false,
                onClick = actions.onShowShortcuts,
                icon = { Icon(Icons.Rounded.Keyboard, contentDescription = null) },
                label = { Text("Shortcuts") },
                badge = { Text("?", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

/** Rescans SMS when the app can read it, otherwise opens SMS setup. Same as the phone's small FAB. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RescanButton(actions: NavActions) {
    val smsScan = koinInject<SmsScanService>()
    val haptics = rememberHaptics()
    NavTooltip("Rescan SMS") {
        SmallFloatingActionButton(
            onClick = {
                if (!actions.scanRunning) {
                    haptics.click()
                    if (smsScan.hasReadSmsPermission()) actions.onRescanSms() else actions.onOpenSmsSetup()
                }
            },
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
        ) {
            if (actions.scanRunning) {
                LoadingIndicator(modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Rounded.Sync, contentDescription = "Rescan SMS")
            }
        }
    }
}

/** A hover tooltip, for mouse users who can't guess what an icon does. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NavTooltip(text: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Right),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        content = content
    )
}
