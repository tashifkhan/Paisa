package sh.taf.billi.data.repository

import sh.taf.billi.data.AppDatabase
import sh.taf.billi.data.Budget
import sh.taf.billi.data.SavingsGoal
import kotlinx.coroutines.flow.Flow

/** Budgets and savings goals. */
class BudgetRepository(db: AppDatabase) {

    private val budgetDao = db.budgetDao()
    private val savingsGoalDao = db.savingsGoalDao()

    // --- BUDGETS ---
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    fun getBudgetsForPeriod(month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getBudgetsForPeriod(month, year)

    suspend fun insertBudget(budget: Budget): Long = budgetDao.insertBudget(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    // --- SAVINGS GOALS ---
    val allSavingsGoals: Flow<List<SavingsGoal>> = savingsGoalDao.getAllSavingsGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoal): Long =
        savingsGoalDao.insertSavingsGoal(goal)

    suspend fun updateSavingsGoal(goal: SavingsGoal) = savingsGoalDao.updateSavingsGoal(goal)

    suspend fun deleteSavingsGoal(goal: SavingsGoal) = savingsGoalDao.deleteSavingsGoal(goal)
}
