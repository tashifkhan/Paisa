package codes.tashif.paisa.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.data.MerchantMapping
import codes.tashif.paisa.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Categories and merchant rules, for the Categories and Merchant rules screens. */
class CategoriesViewModel(
    private val categoriesRepository: CategoryRepository,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoriesRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val merchantMappings: StateFlow<List<MerchantMapping>> = categoriesRepository.allMerchantMappings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String, type: String, color: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            categoriesRepository.insertCategory(
                Category(
                    name = trimmed,
                    type = type,
                    icon = "category",
                    color = color,
                    orderIndex = categories.value.size
                )
            )
            snackbarManager.show("Category added")
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoriesRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val deleted = categoriesRepository.deleteCategory(category)
            if (deleted) {
                snackbarManager.show("Category deleted")
            } else {
                snackbarManager.show("${category.name} has transactions — reassign them first")
            }
        }
    }

    fun saveMerchantMapping(merchantName: String, categoryName: String, categoryType: String) {
        viewModelScope.launch {
            categoriesRepository.saveMerchantMapping(merchantName, categoryName, categoryType, applyToPast = true)
            snackbarManager.show("Saved rule for $merchantName")
        }
    }

    fun deleteMerchantMapping(merchantName: String) {
        viewModelScope.launch {
            categoriesRepository.deleteMerchantMapping(merchantName)
            snackbarManager.show("Removed mapping")
        }
    }
}
