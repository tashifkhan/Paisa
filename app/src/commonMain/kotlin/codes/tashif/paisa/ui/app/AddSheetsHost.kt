package codes.tashif.paisa.ui.app

import androidx.compose.runtime.Composable
import codes.tashif.paisa.ui.accounts.AddAccountSheetHost
import codes.tashif.paisa.ui.budgets.AddBudgetSheet
import codes.tashif.paisa.ui.debts.AddDebtPersonSheet
import codes.tashif.paisa.ui.transaction.AddTransactionSheet

/** The add sheets the FAB and widgets open over the tabs. */
@Composable
fun AddSheetsHost(state: ShellState) {
    if (state.isOpen(AddSheet.TRANSACTION)) {
        AddTransactionSheet(
            onDismiss = { state.close(AddSheet.TRANSACTION) },
            onOpenAiSettings = {
                state.close(AddSheet.TRANSACTION)
                state.open(FullScreen.AI)
            }
        )
    }
    if (state.isOpen(AddSheet.BUDGET)) {
        AddBudgetSheet(onDismiss = { state.close(AddSheet.BUDGET) })
    }
    if (state.isOpen(AddSheet.ACCOUNT)) {
        AddAccountSheetHost(onDismiss = { state.close(AddSheet.ACCOUNT) })
    }
    if (state.isOpen(AddSheet.DEBT_PERSON)) {
        AddDebtPersonSheet(onDismiss = { state.close(AddSheet.DEBT_PERSON) })
    }
}
