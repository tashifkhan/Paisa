package codes.tashif.paisa.ui.settings

import codes.tashif.paisa.ui.platform.PlatformBackHandler
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
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

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

            ThemeModePicker(
                selectedId = current.themeMode,
                onSelect = { viewModel.updateSettings(current.copy(themeMode = it)) }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.small))

            PalettePicker(
                selectedPaletteId = current.colorPalette,
                onSelect = { viewModel.updateSettings(current.copy(colorPalette = it.id)) }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))

            AppearanceToggleCard(
                icon = Icons.Rounded.DarkMode,
                title = "AMOLED Black",
                subtitle = "Pure black surfaces in dark mode (OLED)",
                checked = current.amoledDark,
                onCheckedChange = {
                    haptics.toggle(on = it)
                    viewModel.updateSettings(current.copy(amoledDark = it))
                }
            )
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            AppearanceToggleCard(
                icon = Icons.Rounded.Animation,
                title = "Expressive motion",
                subtitle = "Springy M3 motion. Off = standard, calmer transitions",
                checked = current.expressiveUi,
                onCheckedChange = {
                    haptics.toggle(on = it)
                    viewModel.updateSettings(current.copy(expressiveUi = it))
                }
            )
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            AppearanceToggleCard(
                icon = Icons.Rounded.Contrast,
                title = "High contrast",
                subtitle = "Stronger text and outlines — less soft Material look",
                checked = current.highContrast,
                onCheckedChange = {
                    haptics.toggle(on = it)
                    viewModel.updateSettings(current.copy(highContrast = it))
                }
            )

            Spacer(Modifier.height(MaterialTheme.spacing.small))
            NavBarSettings(
                floatingNavBar = current.floatingNavBar,
                navBarLabels = current.navBarLabels,
                navTabs = current.navTabs,
                onFloatingNavBarChange = {
                    viewModel.updateSettings(current.copy(floatingNavBar = it))
                },
                onNavBarLabelsChange = {
                    viewModel.updateSettings(current.copy(navBarLabels = it))
                },
                onNavTabsChange = viewModel::setNavTabs
            )
        }
    }
}
