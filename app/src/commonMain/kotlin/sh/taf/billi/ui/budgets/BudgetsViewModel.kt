package sh.taf.billi.ui.budgets

import sh.taf.billi.platform.BilliCalendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sh.taf.billi.core.SnackbarManager
import sh.taf.billi.data.Budget
import sh.taf.billi.data.Category
import sh.taf.billi.data.Settings
import sh.taf.billi.data.TransactionWithDetails
import sh.taf.billi.data.repository.BudgetRepository
import sh.taf.billi.data.repository.CategoryRepository
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
            val cal = BilliCalendar.getInstance()
            budgetRepository.insertBudget(
                Budget(
                    categoryId = categoryId,
                    budgetAmount = amount,
                    month = cal.get(BilliCalendar.MONTH) + 1,
                    year = cal.get(BilliCalendar.YEAR),
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
