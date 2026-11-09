package codes.tashif.paisa.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.Budget
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.data.repository.BudgetRepository
import codes.tashif.paisa.data.repository.CategoryRepository
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/** Budgets, plus the categories and transactions needed to show what each has spent. */
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    val budgets: StateFlow<List<Budget>> = budgetRepository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<TransactionWithDetails>> = transactionRepository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Adds a budget for the current month. A null [categoryId] means overall spending. */
    fun addBudget(categoryId: Int?, amount: Double, name: String?) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            budgetRepository.insertBudget(
                Budget(
                    categoryId = categoryId,
                    budgetAmount = amount,
                    month = cal.get(Calendar.MONTH) + 1,
                    year = cal.get(Calendar.YEAR),
                    budgetName = name?.trim()?.takeIf { it.isNotBlank() }
                )
            )
            snackbarManager.show("Budget added")
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
            snackbarManager.show("Budget deleted")
        }
    }
}
