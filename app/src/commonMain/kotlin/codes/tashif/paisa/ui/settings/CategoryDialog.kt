package codes.tashif.paisa.ui.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
    onSave: (name: String, type: String, color: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "expense") }
    var color by remember { mutableStateOf(initial?.color ?: categoryColors.first()) }
    val haptics = rememberHaptics()
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.imePadding(),
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
                ChoiceCards(
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
                    onSave(name.trim(), type, color)
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
