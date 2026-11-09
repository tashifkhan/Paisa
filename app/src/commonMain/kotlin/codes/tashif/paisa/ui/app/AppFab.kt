package codes.tashif.paisa.ui.app

import codes.tashif.paisa.ui.sms.SmsScanService
import org.koin.compose.koinInject
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.AppTab
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

/**
 * The add FAB for the current tab, plus the SMS rescan button on Home.
 * [onRescanSms] runs only when READ_SMS is granted; otherwise SMS setup opens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppFab(
    currentTab: AppTab,
    scanRunning: Boolean,
    onRescanSms: () -> Unit,
    onOpenSmsSetup: () -> Unit,
    onAdd: (AddSheet) -> Unit,
    debtPersonOpen: Boolean
) {
    val smsScan = koinInject<SmsScanService>()
    val haptics = rememberHaptics()

    AnimatedVisibility(
        // On a person's page in Debts, their own buttons add entries instead.
        visible = currentTab in setOf(AppTab.HOME, AppTab.BUDGETS, AppTab.ACCOUNTS) ||
            (currentTab == AppTab.DEBTS && !debtPersonOpen),
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            if (currentTab == AppTab.HOME) {
                SmallFloatingActionButton(
                    onClick = {
                        if (!scanRunning) {
                            haptics.click()
                            val canReadSms = smsScan.hasReadSmsPermission()
                            if (canReadSms) onRescanSms() else onOpenSmsSetup()
                        }
                    }
                ) {
                    if (scanRunning) {
                        LoadingIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Rounded.Sync, contentDescription = "Rescan SMS")
                    }
                }
            }
            FloatingActionButton(
                onClick = {
                    haptics.click()
                    onAdd(addSheetFor(currentTab))
                }
            ) {
                Icon(Icons.Rounded.Add, contentDescription = addLabelFor(currentTab))
            }
        }
    }
}

/** What + adds on [tab]. Tabs with nothing of their own add a transaction. */
internal fun addSheetFor(tab: AppTab): AddSheet = when (tab) {
    AppTab.BUDGETS -> AddSheet.BUDGET
    AppTab.ACCOUNTS -> AddSheet.ACCOUNT
    AppTab.DEBTS -> AddSheet.DEBT_PERSON
    else -> AddSheet.TRANSACTION
}

internal fun addLabelFor(tab: AppTab): String = when (tab) {
    AppTab.BUDGETS -> "Add budget"
    AppTab.ACCOUNTS -> "Add account"
    AppTab.DEBTS -> "Add person"
    else -> "Add transaction"
}
