package codes.tashif.paisa.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.ui.components.CategoryBadge
import codes.tashif.paisa.ui.components.CircleIconButton
import codes.tashif.paisa.ui.components.DetailHeader
import codes.tashif.paisa.ui.components.SettingsGroup
import codes.tashif.paisa.ui.components.SettingsSectionLabel
import codes.tashif.paisa.ui.components.groupPositionOf
import codes.tashif.paisa.ui.components.groupShape
import codes.tashif.paisa.ui.haptics.rememberHaptics
import codes.tashif.paisa.ui.theme.spacing

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
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
