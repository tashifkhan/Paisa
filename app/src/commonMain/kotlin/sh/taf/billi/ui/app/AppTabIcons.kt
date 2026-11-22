package sh.taf.billi.ui.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import sh.taf.billi.core.AppTab
import sh.taf.billi.ui.components.TabMotion

internal val AppTab.icon: ImageVector
    get() = when (this) {
        AppTab.HOME -> Icons.Rounded.Home
        AppTab.ANALYTICS -> Icons.Rounded.BarChart
        AppTab.BUDGETS -> Icons.Rounded.DonutLarge
        AppTab.ACCOUNTS -> Icons.Rounded.AccountBalance
        AppTab.DEBTS -> Icons.Rounded.Handshake
        AppTab.MORE -> Icons.Rounded.MoreHoriz
    }

/** What each icon does when tapped in the floating bar, picked to suit its shape. */
internal val AppTab.motion: TabMotion
    get() = when (this) {
        AppTab.HOME -> TabMotion.HOP
        AppTab.ANALYTICS -> TabMotion.HOP
        // A ring and three dots both look right turning.
        AppTab.BUDGETS -> TabMotion.FULL_TURN
        AppTab.ACCOUNTS -> TabMotion.WIGGLE
        AppTab.DEBTS -> TabMotion.WIGGLE
        AppTab.MORE -> TabMotion.HALF_TURN
    }
