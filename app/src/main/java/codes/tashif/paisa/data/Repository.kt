package codes.tashif.paisa.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Repository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val savingsGoalDao = db.savingsGoalDao()
    private val recurringTransactionDao = db.recurringTransactionDao()
    private val settingsDao = db.settingsDao()
    private val unrecognizedSmsDao = db.unrecognizedSmsDao()
    private val merchantMappingDao = db.merchantMappingDao()
    private val debtDao = db.debtDao()

    // --- ACCOUNTS ---
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
    suspend fun insertAccount(account: Account): Long = db.withTransaction {
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
    suspend fun updateAccount(account: Account) = db.withTransaction {
        accountDao.updateAccount(account)
        accountDao.recomputeBalance(account.id)
    }

    suspend fun renameAccount(accountId: Int, name: String) =
        accountDao.renameAccount(accountId, name)

    /**
     * The user typed in what the account holds right now. Pinned after the newest
     * transaction so future-dated entries don't move the number they just entered.
     */
    suspend fun updateCurrentBalance(accountId: Int, balance: Double) = db.withTransaction {
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
        db.withTransaction {
            val account = accountDao.getAccountById(accountId) ?: return@withTransaction
            if (at < account.balanceAnchorAt) return@withTransaction
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

        db.withTransaction {
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

    // --- CATEGORIES ---
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category): Boolean {
        val count = transactionDao.getTransactionCountForCategory(category.id)
        if (count > 0) return false
        categoryDao.deleteCategory(category)
        return true
    }

    /**
     * Resolve a category by name+type, creating a lightweight default if missing.
     * Used by SMS import when merchant mapping produces a category not yet seeded.
     */
    suspend fun findOrCreateCategory(name: String, type: String): Int {
        val canonical = canonicalizeCategoryName(name, type)
        categoryDao.getCategoryByName(canonical, type)?.let { return it.id }
        // Fallbacks for legacy / alias names
        val aliases = buildList {
            add(canonical)
            add(name)
            CATEGORY_ALIASES[name.lowercase()]?.let { add(it) }
            if (type == "expense") {
                add("Others")
                add("Other")
            } else {
                add("Income")
                add("Other")
            }
        }.distinct()
        for (alias in aliases) {
            categoryDao.getCategoryByName(alias, type)?.let { return it.id }
        }
        val id = categoryDao.insertCategory(
            Category(
                name = canonical,
                type = type,
                icon = "more_horiz",
                color = if (type == "income") "#BFFCC6" else "#E8AEB2",
                isDefault = false,
                orderIndex = 99
            )
        )
        return id.toInt()
    }

    /** Map historical / PennyWise names onto seeded Paisa categories. */
    fun canonicalizeCategoryName(name: String, type: String): String {
        if (type == "income") return name
        return CATEGORY_ALIASES[name.lowercase(Locale.getDefault())] ?: name
    }

    companion object {
        private val CATEGORY_ALIASES = mapOf(
            "transportation" to "Transport",
            "transport" to "Transport",
            "healthcare" to "Health & Fitness",
            "health" to "Health & Fitness",
            "fitness" to "Health & Fitness",
            "health & fitness" to "Health & Fitness",
            "mobile" to "Bills & Utilities",
            "food" to "Food & Dining",
            "dining" to "Food & Dining",
            "food & dining" to "Food & Dining",
            "grocery" to "Groceries",
            "utilities" to "Bills & Utilities",
            "bills" to "Bills & Utilities",
            "other" to "Others",
            "uncategorized" to "Others"
        )
    }

    // --- MERCHANT MAPPINGS ---
    val allMerchantMappings: Flow<List<MerchantMapping>> = merchantMappingDao.getAllMappings()

    suspend fun getCategoryForMerchant(merchantName: String): MerchantMapping? =
        merchantMappingDao.getMappingForMerchant(merchantName.trim())

    suspend fun saveMerchantMapping(
        merchantName: String,
        categoryName: String,
        categoryType: String = "expense",
        applyToPast: Boolean = true
    ) {
        val merchant = merchantName.trim()
        if (merchant.isEmpty()) return
        val now = nowIso()
        val existing = merchantMappingDao.getMappingForMerchant(merchant)
        merchantMappingDao.upsert(
            MerchantMapping(
                merchantName = existing?.merchantName ?: merchant,
                categoryName = categoryName,
                categoryType = categoryType,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        if (applyToPast) {
            val categoryId = findOrCreateCategory(categoryName, categoryType)
            merchantMappingDao.updateTransactionsForMerchant(merchant, categoryId, now)
        }
    }

    suspend fun deleteMerchantMapping(merchantName: String) {
        merchantMappingDao.deleteByMerchant(merchantName)
    }

    /**
     * Resolve category name for a merchant: user mapping first, then keywords.
     */
    suspend fun resolveCategoryName(merchantName: String, transactionType: String): String {
        val type = if (transactionType.equals("income", ignoreCase = true)) "income" else "expense"
        val mapping = getCategoryForMerchant(merchantName)
        if (mapping != null && mapping.categoryType == type) {
            return canonicalizeCategoryName(mapping.categoryName, type)
        }
        return canonicalizeCategoryName(
            codes.tashif.paisa.sms.CategoryMapping.determineCategory(merchantName, type.uppercase(Locale.getDefault())),
            type
        )
    }

    // --- TRANSACTIONS ---
    val allTransactions: Flow<List<TransactionWithDetails>> = transactionDao.getAllTransactions()

    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionsForAccount(accountId)

    suspend fun getTransactionById(id: Int): TransactionWithDetails? =
        transactionDao.getTransactionById(id)

    suspend fun getRawTransactionById(id: Int): Transaction? =
        transactionDao.getRawTransactionById(id)

    suspend fun getTransactionByHash(hash: String): Transaction? =
        transactionDao.getTransactionByHash(hash)

    /**
     * Fuzzy match against already-logged transactions (SMS/manual/statement):
     * same type, same amount (±0.01), date within ±[windowDays] days — bank posting
     * dates often lag the SMS by a day or two.
     */
    suspend fun findSimilarTransaction(
        amount: Double,
        type: String,
        dateIso: String,
        windowDays: Int = 2
    ): Transaction? {
        val day = dateIso.take(10)
        if (day.length != 10) return null
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val parsed = runCatching { fmt.parse(day) }.getOrNull() ?: return null
        val cal = java.util.Calendar.getInstance()
        cal.time = parsed
        cal.add(java.util.Calendar.DAY_OF_MONTH, -windowDays)
        val from = fmt.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_MONTH, 2 * windowDays)
        val to = fmt.format(cal.time)
        return transactionDao.findSimilarTransactions(
            type = type,
            amountLow = amount - 0.01,
            amountHigh = amount + 0.01,
            fromDate = from,
            toDate = to
        ).minByOrNull {
            // Prefer the entry closest to the statement row's date
            runCatching { fmt.parse(it.transactionDate.take(10))?.time }
                .getOrNull()
                ?.let { t -> kotlin.math.abs(t - parsed.time) }
                ?: Long.MAX_VALUE
        }
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return db.withTransaction {
            val id = transactionDao.insertTransaction(transaction)
            accountDao.recomputeBalance(transaction.accountId)
            id
        }
    }

    /**
     * Insert SMS-sourced transaction. If the SMS carried an absolute balance,
     * prefer that over delta adjustment for the account's currentBalance.
     */
    suspend fun insertSmsTransaction(
        transaction: Transaction,
        absoluteBalance: Double?
    ): Long {
        return db.withTransaction {
            val rowId = transactionDao.insertTransaction(transaction)
            if (rowId == -1L) return@withTransaction -1L

            if (absoluteBalance != null) {
                // The reported balance already includes this transaction.
                applyReportedBalance(transaction.accountId, absoluteBalance, transaction.transactionDate)
            }
            accountDao.recomputeBalance(transaction.accountId)
            rowId
        }
    }

    suspend fun updateTransaction(newTransaction: Transaction) {
        db.withTransaction {
            val oldTransaction = transactionDao.getRawTransactionById(newTransaction.id)
                ?: return@withTransaction
            transactionDao.updateTransaction(newTransaction)
            accountDao.recomputeBalance(oldTransaction.accountId)
            if (newTransaction.accountId != oldTransaction.accountId) {
                accountDao.recomputeBalance(newTransaction.accountId)
            }
        }
    }

    suspend fun deleteTransaction(transactionId: Int) {
        db.withTransaction {
            val transaction = transactionDao.getRawTransactionById(transactionId)
                ?: return@withTransaction
            // Soft-delete so SMS re-scan won't re-import
            if (transaction.transactionHash != null) {
                transactionDao.softDeleteTransaction(transactionId, nowIso())
            } else {
                transactionDao.deleteTransaction(transaction)
            }
            accountDao.recomputeBalance(transaction.accountId)
        }
    }

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

    // --- RECURRING ---
    val allRecurringTransactions: Flow<List<RecurringTransaction>> =
        recurringTransactionDao.getAllRecurringTransactions()

    suspend fun insertRecurringTransaction(rule: RecurringTransaction): Long =
        recurringTransactionDao.insertRecurringTransaction(rule)

    suspend fun updateRecurringTransaction(rule: RecurringTransaction) =
        recurringTransactionDao.updateRecurringTransaction(rule)

    suspend fun deleteRecurringTransaction(rule: RecurringTransaction) =
        recurringTransactionDao.deleteRecurringTransaction(rule)

    suspend fun processRecurringTransactions() {
        db.withTransaction {
            val nowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dueRules = recurringTransactionDao.getEnabledRecurringTransactions()
            val touched = mutableSetOf<Int>()
            for (rule in dueRules) {
                if (rule.nextExecutionDate > nowStr) continue
                var execDateStr = rule.nextExecutionDate
                while (execDateStr <= nowStr) {
                    val transaction = Transaction(
                        amount = rule.amount,
                        type = rule.type,
                        categoryId = rule.categoryId,
                        accountId = rule.accountId,
                        note = "[Recurring] ${rule.note}",
                        transactionDate = "${execDateStr}T09:00:00",
                        createdAt = nowIso(),
                        updatedAt = nowIso(),
                        source = "manual",
                        // Same id on every device, so two synced phones that both run
                        // this rule for the same day produce one transaction, not two.
                        syncId = rule.syncId?.let { "rec:$it:$execDateStr" }
                    )
                    transactionDao.insertTransaction(transaction)
                    touched += rule.accountId
                    execDateStr = advanceDate(execDateStr, rule.frequency)
                }
                recurringTransactionDao.updateRecurringTransaction(
                    rule.copy(nextExecutionDate = execDateStr)
                )
            }
            touched.forEach { accountDao.recomputeBalance(it) }
        }
    }

    private fun advanceDate(dateStr: String, frequency: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        val calendar = Calendar.getInstance().apply { time = date }
        when (frequency.lowercase(Locale.getDefault())) {
            "daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "yearly" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1)
        }
        return sdf.format(calendar.time)
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

    // --- DEBTS ---
    /** Everyone you lend to or borrow from, with what they owe you now. */
    val debtPeople: Flow<List<DebtPersonBalance>> = debtDao.getPeopleWithBalances()

    fun debtEntries(personId: Int): Flow<List<DebtEntry>> = debtDao.getEntries(personId)

    /** Finds [name] (any case) or adds them. A non-zero [startingAmount] is recorded as their first entry. */
    suspend fun addDebtPerson(name: String, startingAmount: Double = 0.0, note: String = ""): Int =
        db.withTransaction {
            val clean = name.trim()
            require(clean.isNotEmpty()) { "Name required" }
            val now = nowIso()
            val id = debtDao.getPersonByName(clean)?.id
                ?: debtDao.insertPerson(DebtPerson(name = clean, createdAt = now)).toInt()
            if (startingAmount != 0.0) {
                debtDao.insertEntry(
                    DebtEntry(
                        personId = id,
                        amount = startingAmount,
                        note = note.ifBlank { "Starting balance" },
                        entryDate = now,
                        createdAt = now
                    )
                )
            }
            id
        }

    suspend fun renameDebtPerson(person: DebtPerson, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) debtDao.updatePerson(person.copy(name = clean))
    }

    /** Removes the person and their whole history. */
    suspend fun deleteDebtPerson(person: DebtPerson) = debtDao.deletePerson(person)

    /** [amount] > 0: they owe you more. < 0: they owe you less (or you owe them). */
    suspend fun addDebtEntry(
        personId: Int,
        amount: Double,
        note: String,
        entryDate: String = nowIso(),
        transactionSyncId: String? = null
    ): Long = debtDao.insertEntry(
        DebtEntry(
            personId = personId,
            amount = amount,
            note = note.trim(),
            entryDate = entryDate,
            transactionSyncId = transactionSyncId,
            createdAt = nowIso()
        )
    )

    suspend fun deleteDebtEntry(id: Int) = debtDao.deleteEntry(id)

    /** Brings the person's balance to zero with one closing entry. */
    suspend fun settleDebt(personId: Int) = db.withTransaction {
        val balance = debtDao.getBalance(personId)
        if (kotlin.math.abs(balance) >= 0.005) {
            addDebtEntry(personId, -balance, "Settled up")
        }
    }

    /**
     * Records [amount] of a transaction against a person. Money you paid out
     * (an expense) means they owe you; money you received means they paid you
     * back, or you now owe them. The transaction itself is left as it is.
     */
    suspend fun linkTransactionToDebt(
        transactionId: Int,
        personId: Int?,
        newPersonName: String?,
        amount: Double
    ): Int = db.withTransaction {
        val tx = requireNotNull(transactionDao.getRawTransactionById(transactionId)) { "No transaction $transactionId" }
        val id = personId ?: addDebtPerson(requireNotNull(newPersonName) { "Person name required" })
        val what = tx.merchantName?.takeIf { it.isNotBlank() } ?: tx.note.ifBlank { "Transaction" }
        addDebtEntry(
            personId = id,
            amount = if (tx.type == "expense") amount else -amount,
            note = what,
            entryDate = tx.transactionDate,
            transactionSyncId = tx.syncId
        )
        id
    }

    // --- SETTINGS ---
    val appSettings: Flow<Settings?> = settingsDao.getSettings()

    suspend fun getSettingsDirect(): Settings? = settingsDao.getSettingsDirect()

    suspend fun updateSettings(settings: Settings) = settingsDao.updateSettings(settings)

    // --- UNRECOGNIZED SMS ---
    val pendingUnrecognizedSms: Flow<List<UnrecognizedSms>> = unrecognizedSmsDao.getPending()
    val pendingUnrecognizedCount: Flow<Int> = unrecognizedSmsDao.getPendingCount()

    suspend fun insertUnrecognizedSms(sms: UnrecognizedSms): Long =
        unrecognizedSmsDao.insert(sms)

    suspend fun markUnrecognizedReviewed(id: Int) = unrecognizedSmsDao.markReviewed(id)

    suspend fun deleteUnrecognized(id: Int) = unrecognizedSmsDao.delete(id)
}
