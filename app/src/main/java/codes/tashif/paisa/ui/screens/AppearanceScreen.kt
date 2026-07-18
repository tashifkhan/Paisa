package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.PaisaPalette
import codes.tashif.paisa.ui.theme.spacing

private data class ThemeMode(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val themeModes = listOf(
    ThemeMode("system", "System", Icons.Rounded.AutoAwesome),
    ThemeMode("light", "Light", Icons.Rounded.LightMode),
    ThemeMode("dark", "Dark", Icons.Rounded.DarkMode)
)

@Composable
fun AppearanceScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val haptics = rememberHaptics()

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        codes.tashif.paisa.ui.components.DetailHeader(
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

        // --- THEME MODE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
        ) {
            themeModes.forEach { mode ->
                val selected = current.themeMode == mode.id
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!selected) haptics.tick()
                            viewModel.updateSettings(current.copy(themeMode = mode.id))
                        },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.spacing.medium),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(MaterialTheme.spacing.small))

        // --- COLOR PALETTE ---
        val allPalettes = PaisaPalette.entries.filter {
            it != PaisaPalette.Dynamic || PaisaPalette.isDynamicSupported
        }
        // Prefer Dynamic first, then the next few as featured previews
        val featuredPalettes = remember(allPalettes) {
            buildList {
                val dynamic = allPalettes.find { it == PaisaPalette.Dynamic }
                if (dynamic != null) add(dynamic)
                allPalettes.filter { it != PaisaPalette.Dynamic }
                    .take(4 - size)
                    .forEach { add(it) }
            }
        }
        val selectedPalette = PaisaPalette.fromId(current.colorPalette)
        // If user already picked a non-featured theme, start expanded so it stays visible
        var showAllPalettes by remember(selectedPalette) {
            mutableStateOf(selectedPalette !in featuredPalettes)
        }
        val visiblePalettes = if (showAllPalettes) allPalettes else featuredPalettes
        val hiddenCount = (allPalettes.size - featuredPalettes.size).coerceAtLeast(0)

        Text(
            text = "Color palette",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.extraSmall,
                bottom = MaterialTheme.spacing.smaller
            )
        )

        visiblePalettes.chunked(2).forEach { rowPalettes ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.spacing.smaller),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)
            ) {
                rowPalettes.forEach { palette ->
                    val selected = selectedPalette == palette
                    PaletteCard(
                        palette = palette,
                        selected = selected,
                        onClick = {
                            if (!selected) haptics.tick()
                            viewModel.updateSettings(
                                current.copy(colorPalette = palette.id)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowPalettes.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        if (hiddenCount > 0) {
            FilledTonalButton(
                onClick = {
                    haptics.click()
                    showAllPalettes = !showAllPalettes
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.spacing.small),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(
                    imageVector = if (showAllPalettes) {
                        Icons.Rounded.ExpandLess
                    } else {
                        Icons.Rounded.ExpandMore
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (showAllPalettes) {
                        "Show less"
                    } else {
                        "Show more · $hiddenCount themes"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))

        // --- TOGGLES ---
        ToggleCard(
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
        ToggleCard(
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
        ToggleCard(
            icon = Icons.Rounded.Contrast,
            title = "High contrast",
            subtitle = "Stronger text and outlines — less soft Material look",
            checked = current.highContrast,
            onCheckedChange = {
                haptics.toggle(on = it)
                viewModel.updateSettings(current.copy(highContrast = it))
            }
        )
        }
    }
}

@Composable
private fun PaletteCard(
    palette: PaisaPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDynamic = palette == PaisaPalette.Dynamic
    val accent = if (
        isDynamic &&
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    ) {
        colorResource(android.R.color.system_accent1_400)
    } else {
        palette.seed
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                accent.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isDynamic) "Dynamic" else palette.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isDynamic) "Wallpaper Colors" else "Fixed Palette",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(MaterialTheme.spacing.smaller))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // Row handles the toggle so Switch does not double-fire with a parent clickable.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}
