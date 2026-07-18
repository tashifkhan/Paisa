package codes.tashif.paisa.ai

import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.Repository
import codes.tashif.paisa.data.Transaction
import com.pennywiseai.parser.core.md5Hex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Commits reviewed [ExtractedTransaction] rows into Room with category resolution + hash dedup.
 */
class StatementImportUseCase(
    private val repository: Repository
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
                ?: repository.resolveCategoryName(merchant, type)
            val categoryId = repository.findOrCreateCategory(categoryName, type)

            val dateIso = toIsoDateTime(row.date)
            val hash = buildHash(row, accountId)

            if (repository.getTransactionByHash(hash) != null) {
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

            val id = repository.insertSmsTransaction(tx, absoluteBalance = null)
            if (id == -1L) {
                duplicates++
            } else {
                imported++
            }
        }

        return CommitResult(imported = imported, duplicates = duplicates, skipped = skipped)
    }

    suspend fun ensureImportAccount(): Int {
        repository.getAccountByName("Statement import")?.let { return it.id }
        return repository.insertAccount(
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
            row.merchant.lowercase(Locale.US),
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
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
}
