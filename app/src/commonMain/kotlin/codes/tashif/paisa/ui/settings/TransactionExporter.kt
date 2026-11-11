package codes.tashif.paisa.ui.settings

import codes.tashif.paisa.data.TransactionWithDetails
import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first

/** Draws the PDF statement. Platforms without a PDF renderer bind none. */
interface PdfStatementRenderer {
    suspend fun render(currency: String, txList: List<TransactionWithDetails>): ByteArray
}

/** Writes every non-deleted transaction to a CSV file or a PDF statement. */
class TransactionExporter(
    private val transactionsRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val pdfRenderer: PdfStatementRenderer?
) {
    val supportsPdf: Boolean get() = pdfRenderer != null

    /** Writes CSV to [sink]. Returns false on any error. */
    suspend fun writeCsv(sink: ExportSink): Boolean = try {
        val txList = transactionsRepository.allTransactions.first()
        val csv = buildString {
            append(
                "Transaction ID,Date,Type,Amount,Currency,Note,Category,Account," +
                    "Merchant,Bank,Source,Reference,Tags\n"
            )
            txList.forEach { tx ->
                append(
                    listOf(
                        tx.id.toString(),
                        tx.transactionDate,
                        tx.type,
                        tx.amount.toString(),
                        tx.currency,
                        csvEscape(tx.note),
                        csvEscape(tx.categoryName),
                        csvEscape(tx.accountName),
                        csvEscape(tx.merchantName.orEmpty()),
                        csvEscape(tx.bankName.orEmpty()),
                        tx.source,
                        csvEscape(tx.reference.orEmpty()),
                        csvEscape(tx.tags)
                    ).joinToString(",") + "\n"
                )
            }
        }
        sink.write(csv.encodeToByteArray())
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        sink.close()
    }

    /** Writes a multi-page PDF statement to [sink]. Returns false on any error. */
    suspend fun writePdf(sink: ExportSink): Boolean = try {
        val renderer = pdfRenderer ?: error("PDF export isn't available here")
        val currency = settingsRepository.settings.first()?.currency ?: "₹"
        val txList = transactionsRepository.allTransactions.first().sortedBy { it.transactionDate }
        sink.write(renderer.render(currency, txList))
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        sink.close()
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
