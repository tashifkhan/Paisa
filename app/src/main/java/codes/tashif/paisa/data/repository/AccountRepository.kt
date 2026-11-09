package codes.tashif.paisa.data.repository

import androidx.room3.withWriteTransaction
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AccountRepository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    suspend fun getAccountById(id: Int): Account? = accountDao.getAccountById(id)

    suspend fun getAccountByName(name: String): Account? = accountDao.getAccountByName(name)

    suspend fun getAccountByBankAndLast4(bankName: String, accountLast4: String): Account? =
        accountDao.getAccountByBankAndLast4(bankName, accountLast4)

    suspend fun getAccountByBankName(bankName: String): Account? =
        accountDao.getAccountByBankName(bankName)

    suspend fun getAccountByBankWithoutLast4(bankName: String): Account? =
        accountDao.getAccountByBankWithoutLast4(bankName)

    /** Deletes the account together with every transaction that belongs to it. */
    suspend fun deleteAccountCascade(account: Account) {
        accountDao.deleteTransactionsForAccount(account.id)
        accountDao.deleteAccount(account)
    }

    /** Makes [accountId] the single default account (preselected for new transactions). */
    suspend fun setDefaultAccount(accountId: Int) {
        accountDao.clearDefaultAccount()
        accountDao.markDefaultAccount(accountId)
    }

    /** Persists the given id order as each account's orderIndex. */
    suspend fun reorderAccounts(orderedIds: List<Int>) {
        orderedIds.forEachIndexed { position, id ->
            accountDao.setAccountOrder(id, position)
        }
    }

    /**
     * Merges every group of accounts sharing (bankName, accountLast4) — the
     * shadow duplicates the SMS parser used to create. Returns merge count.
     * Within a group the oldest account (lowest id) is kept.
     */
    suspend fun autoMergeDuplicateAccounts(): Int {
        val accounts = accountDao.getAllAccounts().first()
        var merges = 0
        accounts
            .groupBy { Triple(it.bankName ?: it.name, it.accountLast4, it.type) }
            .values
            .filter { it.size > 1 }
            .forEach { group ->
                val target = group.minByOrNull { it.id } ?: return@forEach
                mergeAccounts(target.id, group.map { it.id } - target.id)
                merges += group.size - 1
            }
        return merges
    }

    /** New accounts start anchored at their opening balance, before every transaction. */
    suspend fun insertAccount(account: Account): Long = db.withWriteTransaction {
        val anchored = if (account.balanceAnchorAt.isEmpty()) {
            account.copy(balanceAnchor = account.openingBalance)
        } else {
            account
        }
        val id = accountDao.insertAccount(anchored)
        accountDao.recomputeBalance(id.toInt())
        id
    }

    /** Saves account fields. currentBalance is derived, so it's recomputed, not taken from [account]. */
    suspend fun updateAccount(account: Account) = db.withWriteTransaction {
        accountDao.updateAccount(account)
        accountDao.recomputeBalance(account.id)
    }

    suspend fun renameAccount(accountId: Int, name: String) =
        accountDao.renameAccount(accountId, name)

    /**
     * The user typed in what the account holds right now. Pinned after the newest
     * transaction so future-dated entries don't move the number they just entered.
     */
    suspend fun updateCurrentBalance(accountId: Int, balance: Double) = db.withWriteTransaction {
        val newest = accountDao.getNewestTransactionDate(accountId).orEmpty()
        val now = nowIso()
        accountDao.setBalanceAnchor(accountId, balance, if (newest > now) newest else now)
        accountDao.recomputeBalance(accountId)
    }

    /**
     * A bank SMS reported [balance] as of [at]. Ignored when an earlier-dated SMS
     * arrives late, so a rescan of old messages can't roll the balance back.
     */
    suspend fun applyReportedBalance(accountId: Int, balance: Double, at: String) {
        db.withWriteTransaction {
            val account = accountDao.getAccountById(accountId) ?: return@withWriteTransaction
            if (at < account.balanceAnchorAt) return@withWriteTransaction
            accountDao.setBalanceAnchor(accountId, balance, at)
            accountDao.recomputeBalance(accountId)
        }
    }

    suspend fun deleteAccount(account: Account): Boolean {
        val count = transactionDao.getTransactionCountForAccount(account.id)
        if (count > 0) return false
        accountDao.deleteAccount(account)
        return true
    }

    /**
     * Merges [sourceIds] into the account [targetId]: transactions and recurring
     * transactions move to the target, missing bank metadata is filled from the
     * sources, and the source accounts are deleted.
     *
     * The merged balance is the latest SMS-reported balance across all moved
     * transactions (merging is mostly used to unify duplicates of the same real
     * account, where summing balances would double count). Without any reported
     * balance the target's balance is kept.
     */
    suspend fun mergeAccounts(targetId: Int, sourceIds: List<Int>) {
        val ids = sourceIds.filter { it != targetId }.distinct()
        if (ids.isEmpty()) return
        val target = accountDao.getAccountById(targetId) ?: return
        val sources = ids.mapNotNull { accountDao.getAccountById(it) }
        if (sources.isEmpty()) return

        db.withWriteTransaction {
            accountDao.reassignTransactions(ids, targetId)
            accountDao.reassignRecurringTransactions(ids, targetId)

            // Pin the merged balance: the newest SMS-reported balance if there is one,
            // else what the target already showed. Moved transactions are duplicates
            // of the same real account, so adding them again would double count.
            val reported = latestReportedBalance(targetId)
            val (anchor, at) = reported
                ?: (target.currentBalance to accountDao.getNewestTransactionDate(targetId).orEmpty())
            accountDao.updateAccount(
                target.copy(
                    balanceAnchor = anchor,
                    balanceAnchorAt = at,
                    bankName = target.bankName ?: sources.firstNotNullOfOrNull { it.bankName },
                    accountLast4 = target.accountLast4
                        ?: sources.firstNotNullOfOrNull { it.accountLast4 },
                    creditLimit = target.creditLimit
                        ?: sources.firstNotNullOfOrNull { it.creditLimit }
                )
            )
            sources.forEach { accountDao.deleteAccount(it) }
            accountDao.recomputeBalance(targetId)
        }
    }

    private suspend fun latestReportedBalance(accountId: Int): Pair<Double, String>? {
        val tx = transactionDao.getLatestWithBalance(accountId) ?: return null
        val balance = tx.balanceAfter ?: return null
        return balance to tx.transactionDate
    }
}
