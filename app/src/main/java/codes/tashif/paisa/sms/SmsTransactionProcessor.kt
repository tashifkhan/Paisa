package codes.tashif.paisa.sms

import android.util.Log
import codes.tashif.paisa.data.Account
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.Repository
import codes.tashif.paisa.data.Transaction
import codes.tashif.paisa.data.UnrecognizedSms
import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.BankParserFactory
import com.pennywiseai.parser.core.md5Hex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Parses bank SMS via [BankParserFactory] and persists transactions into Paisa Room DB.
 * Shared by the real-time [SmsBroadcastReceiver] and the bulk [SmsReaderWorker].
 */
class SmsTransactionProcessor(
    private val db: AppDatabase,
    private val repository: Repository = Repository(db)
) {
    companion object {
        private const val TAG = "SmsTransactionProcessor"
        private val bankColors = listOf(
            "#A0C4FF", "#BDB2FF", "#FFCAD4", "#CAFFBF", "#FFD6A5", "#D0F4DE", "#E4C1F9"
        )
    }

    data class ProcessingResult(
        val success: Boolean,
        val transactionId: Long? = null,
        val reason: String? = null
    )

    data class ScanStats(
        var total: Int = 0,
        var parsed: Int = 0,
        var saved: Int = 0,
        var duplicates: Int = 0,
        var unrecognized: Int = 0,
        var skipped: Int = 0
    )

    suspend fun processAndSaveTransaction(
        sender: String,
        body: String,
        timestamp: Long
    ): ProcessingResult {
        return try {
            val parsers = BankParserFactory.getParsers(sender)
            if (parsers.isEmpty()) {
                // Only keep transactional-looking messages from unknown senders for review
                if (looksLikeTransaction(body)) {
                    storeUnrecognized(sender, body, timestamp)
                    return ProcessingResult(false, reason = "Unrecognized sender stored for review")
                }
                return ProcessingResult(false, reason = "No parser for sender: $sender")
            }

            val parsed = parsers.firstNotNullOfOrNull { it.parse(body, sender, timestamp) }
            if (parsed == null) {
                // Known bank sender but not a parseable transaction (OTP/promo/etc.)
                return ProcessingResult(false, reason = "Could not parse transaction from SMS")
            }

            saveParsedTransaction(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
            ProcessingResult(false, reason = e.message)
        }
    }

    suspend fun saveParsedTransaction(parsed: ParsedTransaction): ProcessingResult {
        return try {
            val hash = parsed.transactionHash?.takeIf { it.isNotBlank() }
                ?: parsed.generateTransactionId()

            val existing = repository.getTransactionByHash(hash)
            if (existing != null) {
                if (existing.isDeleted) {
                    return ProcessingResult(false, reason = "Previously deleted")
                }
                return ProcessingResult(false, reason = "Duplicate transaction")
            }

            // Balance-update SMS carry no transaction — sync the account and stop.
            if (parsed.type == TransactionType.BALANCE_UPDATE) {
                val accountId = resolveAccount(parsed)
                applyAccountUpdates(accountId, parsed)
                return ProcessingResult(false, reason = "Balance update applied")
            }

            val type = mapType(parsed.type)
            val merchant = normalizeMerchant(parsed.merchant)
            // User merchant mapping → keyword rules → Others (canonical seed names)
            val categoryName = repository.resolveCategoryName(
                merchantName = merchant,
                transactionType = type
            )
            val categoryId = repository.findOrCreateCategory(categoryName, type)
            val accountId = resolveAccount(parsed)
            if (parsed.creditLimit != null) {
                applyAccountUpdates(accountId, parsed, applyBalance = false)
            }
            val dateIso = isoFromMillis(parsed.timestamp)
            val now = nowIso()

            val note = buildString {
                append(merchant)
                if (!parsed.reference.isNullOrBlank()) {
                    append(" · ref ")
                    append(parsed.reference)
                }
            }

            val transaction = Transaction(
                amount = parsed.amount.toDouble(),
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                transactionDate = dateIso,
                createdAt = now,
                updatedAt = now,
                merchantName = merchant,
                bankName = parsed.bankName,
                smsBody = parsed.smsBody,
                smsSender = parsed.sender,
                accountNumber = parsed.accountLast4,
                balanceAfter = parsed.balance?.toDouble(),
                transactionHash = hash,
                source = "sms",
                currency = parsed.currency,
                reference = parsed.reference
            )

            val rowId = repository.insertSmsTransaction(transaction, parsed.balance?.toDouble())
            if (rowId == -1L) {
                return ProcessingResult(false, reason = "Duplicate transaction")
            }

            Log.d(TAG, "Saved SMS transaction id=$rowId ${parsed.amount} ${parsed.bankName}")
            ProcessingResult(true, transactionId = rowId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving parsed transaction", e)
            ProcessingResult(false, reason = e.message)
        }
    }

    /**
     * Sync account-level facts carried by the SMS: credit limit (marks the
     * account as a Credit Card) and, for balance updates, the absolute balance.
     */
    private suspend fun applyAccountUpdates(
        accountId: Int,
        parsed: ParsedTransaction,
        applyBalance: Boolean = true
    ) {
        val account = repository.getAccountById(accountId) ?: return
        var updated = account
        parsed.creditLimit?.let { limit ->
            updated = updated.copy(
                creditLimit = limit.toDouble(),
                type = "Credit Card",
                icon = "credit_card"
            )
        }
        if (applyBalance && parsed.balance != null) {
            updated = updated.copy(currentBalance = parsed.balance!!.toDouble())
        }
        if (updated != account) {
            repository.updateAccount(updated)
        }
    }

    private suspend fun resolveAccount(parsed: ParsedTransaction): Int {
        val last4 = parsed.accountLast4
        val bank = parsed.bankName

        if (last4 != null) {
            repository.getAccountByBankAndLast4(bank, last4)?.let { return it.id }
        } else {
            // No digits in the SMS: reuse the bank-level account instead of
            // creating one per message (survives user renames via bankName).
            repository.getAccountByBankWithoutLast4(bank)?.let { return it.id }
            repository.getAccountByName(bank)?.let { return it.id }
        }

        val name = when {
            last4 != null -> "$bank ····$last4"
            parsed.isMobileWallet -> bank
            else -> bank
        }
        val isCreditCard = parsed.creditLimit != null ||
            (parsed.isFromCard && parsed.type == TransactionType.CREDIT)
        val type = when {
            isCreditCard -> "Credit Card"
            parsed.isFromCard -> "Debit Card"
            parsed.isMobileWallet -> "Wallet"
            else -> "Bank Account"
        }
        val color = bankColors[kotlin.math.abs(bank.hashCode()) % bankColors.size]
        val opening = parsed.balance?.toDouble() ?: 0.0

        return repository.insertAccount(
            Account(
                name = name,
                type = type,
                openingBalance = opening,
                currentBalance = opening,
                icon = if (parsed.isFromCard || isCreditCard) "credit_card" else "account_balance",
                color = color,
                bankName = bank,
                accountLast4 = last4 ?: if (parsed.isMobileWallet) "WALLET" else null,
                creditLimit = parsed.creditLimit?.toDouble()
            )
        ).toInt()
    }

    private suspend fun storeUnrecognized(sender: String, body: String, timestamp: Long) {
        val hash = md5Hex(body).take(16)
        repository.insertUnrecognizedSms(
            UnrecognizedSms(
                sender = sender,
                body = body,
                bodyHash = hash,
                timestamp = timestamp,
                createdAt = nowIso()
            )
        )
    }

    private fun mapType(type: TransactionType): String = when (type) {
        TransactionType.INCOME -> "income"
        TransactionType.INVESTMENT -> "expense"
        TransactionType.TRANSFER -> "expense"
        TransactionType.CREDIT -> "expense"
        TransactionType.BALANCE_UPDATE -> "expense"
        TransactionType.EXPENSE -> "expense"
    }

    private fun normalizeMerchant(name: String?): String {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isEmpty()) return "Unknown Merchant"
        return if (trimmed == trimmed.uppercase(Locale.getDefault()) && trimmed.length > 2) {
            trimmed.lowercase(Locale.getDefault()).split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        } else {
            trimmed
        }
    }

    private fun looksLikeTransaction(body: String): Boolean {
        val lower = body.lowercase(Locale.getDefault())
        val keywords = listOf(
            "debited", "credited", "withdrawn", "deposited", "spent",
            "received", "transferred", "paid", "rs.", "inr", "₹", "a/c"
        )
        return keywords.any { lower.contains(it) }
    }

    private fun isoFromMillis(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
