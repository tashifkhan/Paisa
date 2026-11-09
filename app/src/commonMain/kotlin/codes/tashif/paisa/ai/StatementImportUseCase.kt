package codes.tashif.paisa.ai

import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.Transaction
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.CategoryRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import com.pennywiseai.parser.core.md5Hex

/**
 * Commits reviewed [ExtractedTransaction] rows into Room with category resolution + hash dedup.
 */
class StatementImportUseCase(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    data class CommitResult(
        val imported: Int,
        val duplicates: Int,
        val skipped: Int
    )

    suspend fun commit(
        rows: List<ExtractedTransaction>,
        accountId: Int
    ): CommitResult {
        var imported = 0
        var duplicates = 0
        var skipped = 0
        val now = nowIso()

        for (row in rows) {
            if (!row.selected) {
                skipped++
                continue
            }
            if (row.amount <= 0.0) {
                skipped++
                continue
            }

            val type = if (row.type.equals("income", ignoreCase = true)) "income" else "expense"
            val merchant = row.merchant.ifBlank { "Unknown Merchant" }
            val categoryName = row.categoryName?.takeIf { it.isNotBlank() }
                ?: categoryRepository.resolveCategoryName(merchant, type)
            val categoryId = categoryRepository.findOrCreateCategory(categoryName, type)

            val dateIso = toIsoDateTime(row.date)
            val hash = buildHash(row, accountId)

            if (transactionRepository.getTransactionByHash(hash) != null) {
                duplicates++
                continue
            }

            val note = buildString {
                append(merchant)
                if (!row.reference.isNullOrBlank()) {
                    append(" · ref ")
                    append(row.reference)
                }
            }

            val currency = when {
                row.currency.equals("INR", true) || row.currency == "₹" -> "INR"
                else -> row.currency.ifBlank { "INR" }
            }

            val tx = Transaction(
                amount = row.amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                transactionDate = dateIso,
                createdAt = now,
                updatedAt = now,
                merchantName = merchant,
                transactionHash = hash,
                source = "statement",
                currency = currency,
                reference = row.reference
            )

            val id = transactionRepository.insertSmsTransaction(tx, absoluteBalance = null)
            if (id == -1L) {
                duplicates++
            } else {
                imported++
            }
        }

        return CommitResult(imported = imported, duplicates = duplicates, skipped = skipped)
    }

    suspend fun ensureImportAccount(): Int {
        accountRepository.getAccountByName("Statement import")?.let { return it.id }
        return accountRepository.insertAccount(
            Account(
                name = "Statement import",
                type = "Bank Account",
                openingBalance = 0.0,
                currentBalance = 0.0,
                icon = "account_balance",
                color = "#A0C4FF",
                bankName = "Imported"
            )
        ).toInt()
    }

    private fun buildHash(row: ExtractedTransaction, accountId: Int): String {
        val material = listOf(
            "statement",
            accountId.toString(),
            row.date,
            row.amount.toString(),
            row.type,
            row.merchant.lowercase(),
            row.reference.orEmpty(),
            row.rawLine.orEmpty().take(80)
        ).joinToString("|")
        return md5Hex(material).take(32)
    }

    private fun toIsoDateTime(date: String): String {
        val normalized = StatementJsonParser.normalizeDate(date) ?: date.take(10)
        return if (normalized.length == 10) "${normalized}T12:00:00" else normalized
    }

    private fun nowIso(): String =
        formatDate(nowMillis(), "yyyy-MM-dd'T'HH:mm:ss")
}
