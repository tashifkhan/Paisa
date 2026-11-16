package codes.tashif.paisa.ui.categories

import codes.tashif.paisa.ui.platform.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.ui.components.CategoryBadge
import codes.tashif.paisa.ui.components.CategoryVisuals
import codes.tashif.paisa.ui.components.ChoiceCards
import codes.tashif.paisa.ui.components.ChoiceOption
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

private val categoryColors = listOf(
    "#FFDFBA", "#FFB3BA", "#D0F4DE", "#A9DEF9", "#E4C1F9",
    "#FCF6BD", "#B5EAD7", "#FFDAC1", "#C7CEEA", "#F1F0CF"
)

@Composable
internal fun CategoryDialog(
    title: String,
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, color: String, icon: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    initialName: String = "",
    /** Set when the caller needs one type, e.g. a picker listing only expenses. Hides the choice. */
    fixedType: String? = null
) {
    var name by remember { mutableStateOf(initial?.name ?: initialName) }
    var type by remember { mutableStateOf(fixedType ?: initial?.type ?: "expense") }
    var color by remember { mutableStateOf(initial?.color ?: categoryColors.first()) }
    // Null until the user picks: a new category's icon follows its name.
    var pickedIcon by remember { mutableStateOf(initial?.icon) }
    val icon = pickedIcon ?: CategoryVisuals.suggestIconName(name)
    var emoji by remember { mutableStateOf(initial?.icon?.takeIf { CategoryVisuals.isEmoji(it) }.orEmpty()) }
    val haptics = rememberHaptics()
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    )
                )
                if (fixedType == null) ChoiceCards(
                    options = listOf(
                        ChoiceOption(
                            id = "expense",
                            label = "Expense",
                            icon = Icons.AutoMirrored.Rounded.TrendingDown
                        ),
                        ChoiceOption(
                            id = "income",
                            label = "Income",
                            icon = Icons.AutoMirrored.Rounded.TrendingUp
                        )
                    ),
                    selectedId = type,
                    onSelect = { type = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        MaterialTheme.spacing.smaller
                    )
                ) {
                    categoryColors.take(5).forEach { hex ->
                        ColorSwatch(hex, color == hex) {
                            if (color != hex) haptics.tick()
                            color = hex
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        MaterialTheme.spacing.smaller
                    )
                ) {
                    categoryColors.drop(5).forEach { hex ->
                        ColorSwatch(hex, color == hex) {
                            if (color != hex) haptics.tick()
                            color = hex
                        }
                    }
                }
                Text(
                    "Icon",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    CategoryBadge(iconName = icon, categoryName = name, colorHex = color, size = 48.dp)
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { typed ->
                            // Emoji only. 11 chars fits the longest joined emoji, like a family.
                            emoji = typed.filter { it.code > 127 }.take(11)
                            pickedIcon = emoji.ifEmpty { null }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Emoji") },
                        placeholder = { Text("Type one, or pick below") },
                        singleLine = true
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller)) {
                    items(CategoryVisuals.iconNames) { iconName ->
                        IconChoice(iconName, selected = icon == iconName) {
                            if (icon != iconName) haptics.tick()
                            emoji = ""
                            pickedIcon = iconName
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete category", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.confirm()
                    onSave(name.trim(), type, color, icon)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun IconChoice(iconName: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (selected) colors.primaryContainer else colors.surfaceContainerHighest, CircleShape)
            .then(if (selected) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryVisuals.icon(iconName),
            contentDescription = iconName.replace('_', ' '),
            tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(parseHex(hex), CircleShape)
            .then(
                if (selected) {
                    Modifier.border(
                        2.5.dp,
                        MaterialTheme.colorScheme.onSurface,
                        CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun parseHex(hex: String): Color =
    runCatching { (parseColor(hex)) }
        .getOrDefault(Color(0xFFD0D0D0))
