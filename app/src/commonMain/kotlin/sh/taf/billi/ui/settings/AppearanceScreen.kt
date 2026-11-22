package sh.taf.billi.ui.settings

import sh.taf.billi.ui.platform.PlatformBackHandler
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import sh.taf.billi.core.NavTabs
import sh.taf.billi.ui.components.DetailHeader
import sh.taf.billi.ui.components.SettingsSectionLabel
import sh.taf.billi.ui.haptics.rememberHaptics
import sh.taf.billi.ui.theme.spacing

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val haptics = rememberHaptics()

    PlatformBackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(
            title = "Appearance",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            val current = settings ?: return@Column

            SettingsSectionLabel("Home sections")
            AppearanceToggleCard(
                icon = Icons.Rounded.CreditCard,
                title = "Credit cards on Home",
                subtitle = "Credit card summary and balance breakdown",
                checked = current.showCreditCards,
                onCheckedChange = { checked ->
                    haptics.toggle(on = checked)
                    viewModel.editSettings { it.copy(showCreditCards = checked) }
                }
            )
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            AppearanceToggleCard(
                icon = Icons.Rounded.Handshake,
                title = "Debts on Home",
                subtitle = "Summary of what you owe and what's owed to you",
                checked = current.showDebts,
                onCheckedChange = { checked ->
                    haptics.toggle(on = checked)
                    viewModel.editSettings { it.copy(showDebts = checked) }
                }
            )
            Spacer(Modifier.height(MaterialTheme.spacing.small))

            ThemeModePicker(
                selectedId = current.themeMode,
                onSelect = { mode -> viewModel.editSettings { it.copy(themeMode = mode) } }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.small))

            PalettePicker(
                selectedPaletteId = current.colorPalette,
                onSelect = { palette -> viewModel.editSettings { it.copy(colorPalette = palette.id) } }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))

            AppearanceToggleCard(
                icon = Icons.Rounded.DarkMode,
                title = "AMOLED Black",
                subtitle = "Pure black surfaces in dark mode (OLED)",
                checked = current.amoledDark,
                onCheckedChange = { checked ->
                    haptics.toggle(on = checked)
                    viewModel.editSettings { it.copy(amoledDark = checked) }
                }
            )
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            AppearanceToggleCard(
                icon = Icons.Rounded.Animation,
                title = "Expressive motion",
                subtitle = "Springy M3 motion. Off = standard, calmer transitions",
                checked = current.expressiveUi,
                onCheckedChange = { checked ->
                    haptics.toggle(on = checked)
                    viewModel.editSettings { it.copy(expressiveUi = checked) }
                }
            )
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            AppearanceToggleCard(
                icon = Icons.Rounded.Contrast,
                title = "High contrast",
                subtitle = "Stronger text and outlines — less soft Material look",
                checked = current.highContrast,
                onCheckedChange = { checked ->
                    haptics.toggle(on = checked)
                    viewModel.editSettings { it.copy(highContrast = checked) }
                }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.small))
            NavBarSettings(
                floatingNavBar = current.floatingNavBar,
                navBarLabels = current.navBarLabels,
                navTabs = current.navTabs,
                floatingNavLabels = current.floatingNavLabels,
                floatingNavFrosted = current.floatingNavFrosted,
                onFloatingNavLabelsChange = { checked ->
                    viewModel.editSettings { it.copy(floatingNavLabels = checked) }
                },
                onFloatingNavFrostedChange = { checked ->
                    viewModel.editSettings { it.copy(floatingNavFrosted = checked) }
                },
                onFloatingNavBarChange = { checked ->
                    viewModel.editSettings { it.copy(floatingNavBar = checked) }
                },
                onNavBarLabelsChange = { checked ->
                    viewModel.editSettings { it.copy(navBarLabels = checked) }
                },
                onNavTabsChange = { tabs ->
                    viewModel.editSettings { it.copy(navTabs = NavTabs.serialize(tabs)) }
                }
            )
        }
    }
}
