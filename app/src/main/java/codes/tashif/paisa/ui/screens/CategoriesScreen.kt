package codes.tashif.paisa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.data.PaisaViewModel
import codes.tashif.paisa.ui.components.CategoryBadge
import codes.tashif.paisa.ui.components.ChoiceCards
import codes.tashif.paisa.ui.components.ChoiceOption
import codes.tashif.paisa.ui.components.CircleIconButton
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp

private val categoryColors = listOf(
    "#FFDFBA", "#FFB3BA", "#D0F4DE", "#A9DEF9", "#E4C1F9",
    "#FCF6BD", "#B5EAD7", "#FFDAC1", "#C7CEEA", "#F1F0CF"
)

@Composable
fun CategoriesScreen(
    viewModel: PaisaViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    var editing by remember { mutableStateOf<Category?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(
            title = "Categories",
            onBack = onBack,
            actions = {
                CircleIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "Add category",
                    onClick = {
                        haptics.click()
                        showAdd = true
                    }
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.medium)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        ) {
            val expense = categories.filter { it.type == "expense" }
            val income = categories.filter { it.type == "income" }

            if (expense.isNotEmpty()) {
                SettingsSectionLabel("Expense")
                CategoryGroup(expense) { editing = it }
            }
            if (income.isNotEmpty()) {
                SettingsSectionLabel("Income")
                CategoryGroup(income) { editing = it }
            }
        }
    }

    if (showAdd) {
        CategoryDialog(
            title = "New category",
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { name, type, color ->
                viewModel.addCategory(name, type, color)
                showAdd = false
            }
        )
    }

    editing?.let { category ->
        CategoryDialog(
            title = "Edit category",
            initial = category,
            onDismiss = { editing = null },
            onSave = { name, type, color ->
                viewModel.updateCategory(
                    category.copy(name = name, type = type, color = color)
                )
                editing = null
            },
            onDelete = if (category.isDefault) {
                null
            } else {
                {
                    haptics.reject()
                    viewModel.deleteCategory(category)
                    editing = null
                }
            }
        )
    }
}

@Composable
private fun CategoryGroup(
    categories: List<Category>,
    onClick: (Category) -> Unit
) {
    val haptics = rememberHaptics()
    SettingsGroup {
        categories.forEachIndexed { index, category ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = groupShape(groupPositionOf(index, categories.size)),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.click()
                            onClick(category)
                        }
                        .padding(
                            horizontal = MaterialTheme.spacing.medium,
                            vertical = MaterialTheme.spacing.small
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadge(
                        iconName = category.icon,
                        categoryName = category.name,
                        colorHex = category.color,
                        size = 44.dp
                    )
                    Spacer(Modifier.size(MaterialTheme.spacing.small))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (category.isDefault) "Default · ${category.type}" else category.type.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDialog(
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
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFFD0D0D0))
