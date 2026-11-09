package codes.tashif.paisa.ui.settings

import codes.tashif.paisa.ui.platform.dynamicAccentColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.PaisaPalette
import codes.tashif.paisa.ui.theme.spacing

/**
 * "Color palette" heading and a two-column grid of palettes, folded down to
 * a few featured ones until the user asks for more. Emits its children into
 * the caller's Column.
 */
@Composable
fun PalettePicker(
    selectedPaletteId: String?,
    onSelect: (PaisaPalette) -> Unit
) {
    val haptics = rememberHaptics()
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
    val selectedPalette = PaisaPalette.fromId(selectedPaletteId)
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
                        onSelect(palette)
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
}

@Composable
private fun PaletteCard(
    palette: PaisaPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDynamic = palette == PaisaPalette.Dynamic
    val accent = (if (isDynamic) dynamicAccentColor() else null) ?: palette.seed

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
