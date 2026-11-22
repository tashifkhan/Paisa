package sh.taf.billi.ingest

import android.util.Log
import sh.taf.billi.data.Account
import sh.taf.billi.data.Transaction
import sh.taf.billi.data.repository.AccountRepository
import sh.taf.billi.data.repository.CategoryRepository
import sh.taf.billi.data.repository.TransactionRepository
import sh.taf.billi.ingest.IngestResult.Outcome
import sh.taf.billi.notifications.NotificationImportService
import sh.taf.billi.notifications.UNASSIGNED_ACCOUNT_TYPE
import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.notification.Channel
import com.pennywiseai.parser.notification.Charge
import com.pennywiseai.parser.notification.CrossChannelDedup
import com.pennywiseai.parser.notification.NotificationApp
import com.pennywiseai.parser.notification.NotificationAppKind
import com.pennywiseai.parser.notification.NotificationApps
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Saves a parsed charge as a transaction, once.
 *
 * One payment often arrives three ways: a UPI app notification, a bank SMS and a
 * bank app notification. Before saving, [CrossChannelDedup] compares the charge
 * with nearby rows. A repeat is dropped, and a bank message that follows a UPI
 * app's notification moves its account and balance onto the row already saved.
 */
class TransactionWriter(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    /** The user's app-to-account links. Null in tests that don't need them. */
    private val links: NotificationImportService? = null,
) {
    /** Check-then-insert must not interleave: an SMS and a notification for one charge often land together. */
    private val lock = Mutex()

    suspend fun save(parsed: ParsedTransaction, source: String): IngestResult =
        lock.withLock {
            try {
                saveLocked(parsed, source)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving parsed transaction", e)
                IngestResult.of(Outcome.FAILED, e.message)
            }
        }

    private suspend fun saveLocked(parsed: ParsedTransaction, source: String): IngestResult {
        val hash = parsed.transactionHash?.takeIf { it.isNotBlank() } ?: parsed.generateTransactionId()

        transactionRepository.getTransactionByHash(hash)?.let { existing ->
            return IngestResult.of(if (existing.isDeleted) Outcome.DELETED else Outcome.DUPLICATE)
        }

        // Balance alerts carry no transaction: sync the account and stop.
        if (parsed.type == TransactionType.BALANCE_UPDATE) {
            applyAccountUpdates(resolveAccount(parsed), parsed)
            return IngestResult.of(Outcome.BALANCE_UPDATED)
        }

        crossChannelMatch(parsed, source, hash)?.let { return it }

        val type = mapType(parsed.type)
        // The user's rename ("SWIGGY LTD" → "Swiggy") before the category rules look it up.
        val merchant = categoryRepository.displayNameFor(normalizeMerchant(parsed.merchant))
        // User merchant mapping, then keyword rules, then Other.
        val categoryName = categoryRepository.resolveCategoryName(merchantName = merchant, transactionType = type)
        val categoryId = categoryRepository.findOrCreateCategory(categoryName, type)
        val accountId = resolveAccount(parsed)
        if (parsed.creditLimit != null) applyAccountUpdates(accountId, parsed, applyBalance = false)
        val now = nowIso()

        val transaction = Transaction(
            amount = parsed.amount.toDouble(),
            type = type,
            categoryId = categoryId,
            accountId = accountId,
            note = if (parsed.reference.isNullOrBlank()) merchant else "$merchant · ref ${parsed.reference}",
            transactionDate = isoFromMillis(parsed.timestamp),
            createdAt = now,
            updatedAt = now,
            merchantName = merchant,
            bankName = parsed.bankName,
            smsBody = parsed.smsBody,
            smsSender = parsed.sender,
            accountNumber = parsed.accountLast4,
            balanceAfter = parsed.balance?.toDouble(),
            transactionHash = hash,
            source = source,
            currency = parsed.currency,
            reference = parsed.reference
        )

        val rowId = transactionRepository.insertSmsTransaction(transaction, parsed.balance?.toDouble())
        if (rowId == -1L) return IngestResult.of(Outcome.DUPLICATE)
        Log.d(TAG, "Saved $source transaction id=$rowId ${parsed.amount} ${parsed.bankName}")
        val waiting = accountRepository.getAccountById(accountId)?.type == UNASSIGNED_ACCOUNT_TYPE
        return IngestResult(Outcome.SAVED, rowId, needsAccount = waiting)
    }

    /**
     * Syncs account-level facts: a credit limit marks the account as a credit
     * card, and a balance alert sets the absolute balance.
     */
    private suspend fun applyAccountUpdates(accountId: Int, parsed: ParsedTransaction, applyBalance: Boolean = true) {
        val account = accountRepository.getAccountById(accountId) ?: return
        val limit = parsed.creditLimit
        if (limit != null) {
            val updated = account.copy(creditLimit = limit.toDouble(), type = "Credit Card", icon = "credit_card")
            if (updated != account) accountRepository.updateAccount(updated)
        }
        val balance = parsed.balance
        if (applyBalance && balance != null) {
            accountRepository.applyReportedBalance(accountId, balance.toDouble(), isoFromMillis(parsed.timestamp))
        }
    }

    /** The same charge already saved by another channel, or null when it's new. */
    private suspend fun crossChannelMatch(parsed: ParsedTransaction, source: String, hash: String): IngestResult? {
        val incoming = Charge(
            amount = parsed.amount.toDouble(),
            type = mapType(parsed.type),
            timeMillis = parsed.timestamp,
            channel = channelOf(source, parsed.sender),
            bankName = parsed.bankName,
            merchant = normalizeMerchant(parsed.merchant),
            accountLast4 = parsed.accountLast4,
            reference = parsed.reference
        )
        if (incoming.channel == Channel.OTHER) return null

        val candidates = transactionRepository.findChargeCandidates(
            amount = incoming.amount,
            type = incoming.type,
            fromIso = isoFromMillis(parsed.timestamp - CrossChannelDedup.MAX_WINDOW_MS),
            toIso = isoFromMillis(parsed.timestamp + CrossChannelDedup.MAX_WINDOW_MS)
        ).mapNotNull { row -> chargeOf(row)?.let { row to it } }

        val (row, verdict) = CrossChannelDedup.firstMatch(incoming, candidates) ?: return null
        if (row.isDeleted) return IngestResult.of(Outcome.DELETED, "deleted ${row.source} row ${row.id}")
        return when (verdict) {
            CrossChannelDedup.Verdict.DUPLICATE ->
                IngestResult(Outcome.DUPLICATE, row.id.toLong(), "already saved from ${row.smsSender ?: row.source}")
            CrossChannelDedup.Verdict.ENRICH_EXISTING -> {
                enrichFromBank(row, parsed, source, hash)
                IngestResult(Outcome.MERGED, row.id.toLong(), "merged into row ${row.id} from ${row.smsSender}")
            }
            CrossChannelDedup.Verdict.DISTINCT -> null
        }
    }

    /**
     * The bank's message for a charge an app reported first. Keeps the app's
     * payee name, the category and any edits, and takes the bank's account,
     * balance and message, so the row is what the SMS alone would have saved.
     */
    private suspend fun enrichFromBank(row: Transaction, parsed: ParsedTransaction, source: String, hash: String) {
        val accountId = resolveAccount(parsed)
        val merchant = row.merchantName?.takeIf { it != UNKNOWN_MERCHANT }
            ?: categoryRepository.displayNameFor(normalizeMerchant(parsed.merchant)).takeIf { !accountName.containsMatchIn(it) }
            ?: row.merchantName
            ?: UNKNOWN_MERCHANT
        transactionRepository.updateTransaction(
            row.copy(
                accountId = accountId,
                merchantName = merchant,
                bankName = parsed.bankName,
                accountNumber = parsed.accountLast4 ?: row.accountNumber,
                balanceAfter = parsed.balance?.toDouble() ?: row.balanceAfter,
                reference = row.reference ?: parsed.reference,
                smsBody = parsed.smsBody,
                smsSender = parsed.sender,
                transactionHash = hash,
                source = source,
                updatedAt = nowIso()
            )
        )
        parsed.balance?.let {
            accountRepository.applyReportedBalance(accountId, it.toDouble(), isoFromMillis(parsed.timestamp))
        }
        if (parsed.creditLimit != null) applyAccountUpdates(accountId, parsed, applyBalance = false)
        if (row.accountId != accountId) dropEmptyHolding(row.accountId)
    }

    /**
     * A notification saved first may sit on the app's holding account. Once the
     * bank's message moves the row to the real account, an empty holding goes.
     */
    private suspend fun dropEmptyHolding(accountId: Int) {
        val account = accountRepository.getAccountById(accountId) ?: return
        if (account.type == UNASSIGNED_ACCOUNT_TYPE) accountRepository.deleteAccount(account)
    }

    private fun channelOf(source: String, sender: String?): Channel = when (source) {
        SOURCE_SMS -> Channel.SMS
        SOURCE_NOTIFICATION -> when (NotificationApps.find(sender)?.kind) {
            NotificationAppKind.BANK -> Channel.BANK_APP
            NotificationAppKind.PAYMENT -> Channel.PAYMENT_APP
            null -> Channel.OTHER
        }
        else -> Channel.OTHER
    }

    private fun chargeOf(row: Transaction): Charge? {
        val channel = channelOf(row.source, row.smsSender)
        if (channel == Channel.OTHER) return null
        val at = millisFromIso(row.transactionDate) ?: return null
        return Charge(
            amount = row.amount,
            type = row.type,
            timeMillis = at,
            channel = channel,
            bankName = row.bankName,
            merchant = row.merchantName,
            accountLast4 = row.accountNumber,
            reference = row.reference
        )
    }

    private suspend fun resolveAccount(parsed: ParsedTransaction): Int {
        val app = NotificationApps.find(parsed.sender)
        if (app != null) appAccount(parsed, app)?.let { return it }
        return bankAccount(parsed)
    }

    /**
     * Which of the user's accounts an app's notification belongs to, in order:
     * the link for these digits, the only account ending in them, the app's
     * default link, then the only account that fits the app (the only one at
     * that bank for a bank app, the only bank account for a UPI app). Null hands
     * a bank app with digits to [bankAccount], the same as its SMS. Otherwise the
     * payment waits on the app's holding account until the user picks one.
     */
    private suspend fun appAccount(parsed: ParsedTransaction, app: NotificationApp): Int? {
        val last4 = parsed.accountLast4?.takeIf { it.isNotBlank() }
        val real = accountRepository.getAllAccountsList().filter { it.type != UNASSIGNED_ACCOUNT_TYPE }
        fun usable(id: Int?) = id?.takeIf { wanted -> real.any { it.id == wanted } }
        val isBank = app.kind == NotificationAppKind.BANK
        // A bank app's payments belong to that bank ("HDFC Bank"); a UPI app's name is no bank.
        val bank = parsed.bankName.takeIf { isBank }

        if (last4 != null) {
            usable(links?.linkedAccount(app.packageName, last4))?.let { return it }
            real.filter { it.accountLast4 == last4 }.let { matches ->
                (matches.singleOrNull() ?: matches.singleOrNull { it.bankName.equals(bank, ignoreCase = true) })
                    ?.let { return it.id }
            }
        }
        usable(links?.linkedAccount(app.packageName, null))?.let { return it }

        val only = if (isBank) {
            real.singleOrNull { bank != null && it.bankName.equals(bank, ignoreCase = true) }
        } else {
            real.singleOrNull { it.type == BANK_ACCOUNT_TYPE }
        }
        if (only != null) return only.id
        // New digits from a bank's own parser: an account like its SMS would make.
        if (isBank && bank != null && last4 != null) return null
        return holdingAccount(app.name)
    }

    /** The app's holding account, made on first use. */
    private suspend fun holdingAccount(appName: String): Int {
        accountRepository.getAllAccountsList()
            .firstOrNull { it.type == UNASSIGNED_ACCOUNT_TYPE && it.bankName == appName }
            ?.let { return it.id }
        return accountRepository.insertAccount(
            Account(
                name = appName,
                type = UNASSIGNED_ACCOUNT_TYPE,
                openingBalance = 0.0,
                currentBalance = 0.0,
                icon = "account_balance_wallet",
                color = colorFor(appName),
                bankName = appName,
                accountLast4 = null
            )
        ).toInt()
    }

    /** A bank SMS's account: by bank and digits, else the bank's only account, else a new one. */
    private suspend fun bankAccount(parsed: ParsedTransaction): Int {
        val last4 = parsed.accountLast4
        val bank = parsed.bankName

        if (last4 != null) {
            accountRepository.getAccountByBankAndLast4(bank, last4)?.let { return it.id }
        } else {
            // No digits: reuse the bank-level account instead of making one per message.
            accountRepository.getAccountByBankWithoutLast4(bank)?.let { return it.id }
            accountRepository.getAccountByName(bank)?.let { return it.id }
        }

        val isCreditCard = parsed.creditLimit != null || (parsed.isFromCard && parsed.type == TransactionType.CREDIT)
        val type = when {
            isCreditCard -> "Credit Card"
            parsed.isFromCard -> "Debit Card"
            parsed.isMobileWallet -> "Wallet"
            else -> BANK_ACCOUNT_TYPE
        }
        val opening = parsed.balance?.toDouble() ?: 0.0
        return accountRepository.insertAccount(
            Account(
                name = if (last4 != null) "$bank ····$last4" else bank,
                type = type,
                openingBalance = opening,
                currentBalance = opening,
                icon = if (parsed.isFromCard || isCreditCard) "credit_card" else "account_balance",
                color = colorFor(bank),
                bankName = bank,
                accountLast4 = last4 ?: if (parsed.isMobileWallet) "WALLET" else null,
                creditLimit = parsed.creditLimit?.toDouble()
            )
        ).toInt()
    }

    private fun mapType(type: TransactionType): String =
        if (type == TransactionType.INCOME) "income" else "expense"

    private fun normalizeMerchant(name: String?): String {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isEmpty()) return UNKNOWN_MERCHANT
        if (trimmed != trimmed.uppercase(Locale.getDefault()) || trimmed.length <= 2) return trimmed
        return trimmed.lowercase(Locale.getDefault()).split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    private fun colorFor(bank: String) = bankColors[abs(bank.hashCode()) % bankColors.size]

    companion object {
        private const val TAG = "TransactionWriter"
        const val SOURCE_SMS = "sms"
        const val SOURCE_NOTIFICATION = "notification"
        private const val UNKNOWN_MERCHANT = "Unknown Merchant"

        private const val BANK_ACCOUNT_TYPE = "Bank Account"

        /** "A/c XX1234", masked or long digit runs: a bank parser's account text, not a payee. */
        private val accountName = Regex("""(?:^|\s)(?:a/c|acct|account|xx+\d+|\*+\d+)\b|\d{4,}""", RegexOption.IGNORE_CASE)

        private val bankColors = listOf("#A0C4FF", "#BDB2FF", "#FFCAD4", "#CAFFBF", "#FFD6A5", "#D0F4DE", "#E4C1F9")

        private const val ISO = "yyyy-MM-dd'T'HH:mm:ss"

        internal fun isoFromMillis(millis: Long): String = SimpleDateFormat(ISO, Locale.US).format(Date(millis))

        private fun millisFromIso(iso: String): Long? =
            runCatching { SimpleDateFormat(ISO, Locale.US).parse(iso.take(19))?.time }.getOrNull()

        internal fun nowIso(): String = isoFromMillis(System.currentTimeMillis())
    }
}
