package sh.taf.billi.notifications

import sh.taf.billi.data.Account
import sh.taf.billi.data.Transaction
import sh.taf.billi.data.repository.AccountRepository
import sh.taf.billi.data.repository.TransactionRepository
import sh.taf.billi.data.repository.nowIso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** A payment waiting for its account, with the app it came from. */
data class AwaitingAccount(
    val transaction: Transaction,
    /** The holding account's name, which is the app's name ("Google Pay"). */
    val appName: String
) {
    /** "Which account paid?" for money out, "Which account got this?" for money in. */
    val question: String
        get() = if (transaction.type == "income") "Which account got this?" else "Which account paid?"

    /** The account digits the app named, if any ("1234" in "from ••1234"). */
    val last4: String? get() = transaction.accountNumber?.takeIf { it.isNotBlank() }
}

/**
 * Payments read from a UPI or bank app land on that app's holding account
 * when Billi can't tell which of your accounts paid. This is where the user
 * picks the account, and optionally makes it the rule for that app.
 */
class AccountApproval(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val links: NotificationImportService
) {
    val awaiting: Flow<List<AwaitingAccount>> =
        combine(transactions.awaitingAccount, accounts.allAccounts) { rows, all ->
            val names = all.associate { it.id to it.name }
            rows.map { AwaitingAccount(it, names[it.accountId] ?: it.bankName.orEmpty()) }
        }

    /** Every real account, for the picker. Holding accounts are left out. */
    val choices: Flow<List<Account>> =
        accounts.allAccounts.map { all -> all.filter { it.type != UNASSIGNED_ACCOUNT_TYPE } }

    /**
     * Moves [transactionId] to [accountId]. With [remember], the app's payments
     * from the same account digits go there from now on, and the ones already
     * waiting move too. Returns how many transactions moved.
     */
    suspend fun assign(transactionId: Int, accountId: Int, remember: Boolean): Int {
        val row = transactions.getRawTransactionById(transactionId) ?: return 0
        val target = accounts.getAccountById(accountId) ?: return 0
        if (target.type == UNASSIGNED_ACCOUNT_TYPE) return 0

        val sender = row.smsSender?.takeIf { it.isNotBlank() }
        val moving = if (remember && sender != null) {
            links.setLink(sender, row.accountNumber?.takeIf { it.isNotBlank() }, accountId)
            transactions.awaitingAccountList().filter {
                it.id == row.id ||
                    (it.smsSender == sender && it.accountNumber.orEmpty() == row.accountNumber.orEmpty())
            }
        } else {
            listOf(row)
        }

        val from = moving.map { it.accountId }.toSet()
        val now = nowIso()
        transactions.updateTransactions(moving.map { it.copy(accountId = accountId, updatedAt = now) })
        from.forEach { dropIfEmpty(it) }
        return moving.size
    }

    /** A holding account with nothing left on it goes away. */
    private suspend fun dropIfEmpty(accountId: Int) {
        val account = accounts.getAccountById(accountId) ?: return
        if (account.type != UNASSIGNED_ACCOUNT_TYPE) return
        accounts.deleteAccount(account)
    }
}
