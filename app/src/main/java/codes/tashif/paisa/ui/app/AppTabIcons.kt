package codes.tashif.paisa.ui.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import codes.tashif.paisa.data.AppTab

internal val AppTab.icon: ImageVector
    get() = when (this) {
        AppTab.HOME -> Icons.Rounded.Home
        AppTab.ANALYTICS -> Icons.Rounded.BarChart
        AppTab.BUDGETS -> Icons.Rounded.DonutLarge
        AppTab.ACCOUNTS -> Icons.Rounded.AccountBalance
        AppTab.DEBTS -> Icons.Rounded.Handshake
        AppTab.MORE -> Icons.Rounded.MoreHoriz
    }
