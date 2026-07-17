package com.paisa.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.paisa.app.data.PaisaViewModel
import com.paisa.app.ui.components.PaisaTopBar
import com.paisa.app.ui.theme.spacing

@Composable
fun MoreScreen(
    viewModel: PaisaViewModel,
    onOpenSmsSetup: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val scan by viewModel.smsScanProgress.collectAsState()
    val unrecognizedCount by viewModel.unrecognizedSmsCount.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "More")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = MaterialTheme.spacing.smaller)
        ) {
            ListItem(
                headlineContent = { Text("SMS import") },
                supportingContent = {
                    Text(
                        when {
                            scan.isRunning -> "Scanning… ${scan.processed}/${scan.total}"
                            unrecognizedCount > 0 -> "$unrecognizedCount unrecognized · tap to manage"
                            else -> "Import bank transactions from SMS"
                        }
                    )
                },
                leadingContent = {
                    Icon(Icons.Rounded.Sms, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSmsSetup)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Categories") },
                supportingContent = { Text("${categories.size} categories") },
                leadingContent = {
                    Icon(Icons.Rounded.Category, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: categories screen */ }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Currency") },
                supportingContent = { Text(settings?.currency ?: "₹") },
                leadingContent = {
                    Icon(Icons.Rounded.Payments, contentDescription = null)
                }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = {
                    Text(
                        settings?.themeMode?.replaceFirstChar { it.uppercase() } ?: "System"
                    )
                },
                leadingContent = {
                    Icon(Icons.Rounded.DarkMode, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val current = settings ?: return@clickable
                        val next = when (current.themeMode) {
                            "system" -> "light"
                            "light" -> "dark"
                            else -> "system"
                        }
                        viewModel.updateSettings(current.copy(themeMode = next))
                    }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("About Paisa") },
                supportingContent = { Text("Offline-first personal finance · SMS parsers from PennyWise") },
                leadingContent = {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                }
            )
        }
    }
}
