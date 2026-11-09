package codes.tashif.paisa.ui.settings

import codes.tashif.paisa.data.repository.SettingsRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** Writes every non-deleted transaction to a CSV file or a PDF statement. */
class TransactionExporter @Inject constructor(
    private val transactionsRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) {

    /** Writes CSV to [outStream] on the IO dispatcher and closes it. Returns false on any error. */
    suspend fun writeCsv(outStream: java.io.OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val txList = transactionsRepository.allTransactions.first()
            java.io.BufferedWriter(java.io.OutputStreamWriter(outStream)).use { writer ->
                writer.write(
                    "Transaction ID,Date,Type,Amount,Currency,Note,Category,Account," +
                        "Merchant,Bank,Source,Reference,Tags\n"
                )
                txList.forEach { tx ->
                    writer.write(
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
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                outStream.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Writes a multi-page PDF statement to [outStream] on the IO dispatcher and
     * closes it. Returns false on any error.
     */
    suspend fun writePdf(outStream: java.io.OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val currency = settingsRepository.settings.first()?.currency ?: "₹"
            val txList = transactionsRepository.allTransactions.first().sortedBy { it.transactionDate }
            val pdfDocument = android.graphics.pdf.PdfDocument()

            val pageWidth = 595 // A4
            val pageHeight = 842
            val margin = 40f
            val rowHeight = 24f
            val headerHeight = 35f
            val yStart = 140f
            val rowsPerPage =
                ((pageHeight - yStart - margin) / rowHeight).toInt().coerceAtLeast(1)

            val brandColor = android.graphics.Color.parseColor("#0B6E4F")
            val chunks =
                if (txList.isEmpty()) listOf(emptyList()) else txList.chunked(rowsPerPage)

            chunks.forEachIndexed { pageIndex, pageTxList ->
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo
                    .Builder(pageWidth, pageHeight, pageIndex + 1)
                    .create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                }
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 10f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(
                        "sans-serif",
                        android.graphics.Typeface.NORMAL
                    )
                }
                val boldTextPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 10f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(
                        "sans-serif",
                        android.graphics.Typeface.BOLD
                    )
                }
                val titlePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#121212")
                    textSize = 24f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(
                        "sans-serif",
                        android.graphics.Typeface.BOLD
                    )
                }
                val subtitlePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10f
                    isAntiAlias = true
                }

                if (pageIndex == 0) {
                    canvas.drawText("PAISA", margin, 54f, titlePaint)
                    val generatedAt = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                    canvas.drawText(
                        "Generated on: $generatedAt",
                        margin,
                        78f,
                        subtitlePaint
                    )
                    canvas.drawText(
                        "Total Transactions: ${txList.size}",
                        margin,
                        92f,
                        subtitlePaint
                    )
                    paint.color = brandColor
                    canvas.drawRect(margin, 116f, pageWidth - margin, 120f, paint)
                } else {
                    canvas.drawText(
                        "PAISA - Transactions Statement (Page ${pageIndex + 1})",
                        margin,
                        44f,
                        boldTextPaint
                    )
                    paint.color = brandColor
                    canvas.drawRect(margin, 52f, pageWidth - margin, 54f, paint)
                }

                val headerY = if (pageIndex == 0) yStart else 70f
                paint.color = android.graphics.Color.parseColor("#EEEEEE")
                canvas.drawRect(
                    margin,
                    headerY,
                    pageWidth - margin,
                    headerY + headerHeight,
                    paint
                )

                val colDateX = margin + 5f
                val colTimeX = margin + 75f
                val colAccountX = margin + 135f
                val colCategoryX = margin + 235f
                val colTypeX = margin + 345f
                val colAmountX = margin + 425f

                val headerTextY = headerY + 22f
                canvas.drawText("Date", colDateX, headerTextY, boldTextPaint)
                canvas.drawText("Time", colTimeX, headerTextY, boldTextPaint)
                canvas.drawText("Account", colAccountX, headerTextY, boldTextPaint)
                canvas.drawText("Category", colCategoryX, headerTextY, boldTextPaint)
                canvas.drawText("Type", colTypeX, headerTextY, boldTextPaint)
                canvas.drawText(
                    "Amount ($currency)",
                    colAmountX,
                    headerTextY,
                    boldTextPaint
                )

                var currentY = headerY + headerHeight
                pageTxList.forEachIndexed { index, tx ->
                    if (index % 2 == 0) {
                        paint.color = android.graphics.Color.parseColor("#F9F9F9")
                        canvas.drawRect(
                            margin,
                            currentY,
                            pageWidth - margin,
                            currentY + rowHeight,
                            paint
                        )
                    }

                    val rowTextY = currentY + 16f
                    val parts = tx.transactionDate.split("T")
                    val datePart = parts.getOrNull(0).orEmpty()
                    val timePart = parts.getOrNull(1)?.take(5).orEmpty()

                    canvas.drawText(datePart, colDateX, rowTextY, textPaint)
                    canvas.drawText(timePart, colTimeX, rowTextY, textPaint)
                    canvas.drawText(
                        truncateForPdf(tx.accountName, 12),
                        colAccountX,
                        rowTextY,
                        textPaint
                    )
                    canvas.drawText(
                        truncateForPdf(tx.categoryName, 15),
                        colCategoryX,
                        rowTextY,
                        textPaint
                    )

                    val typePaint = android.graphics.Paint(textPaint).apply {
                        color = if (tx.type.equals("income", ignoreCase = true)) {
                            android.graphics.Color.parseColor("#00796B")
                        } else {
                            android.graphics.Color.parseColor("#D32F2F")
                        }
                        typeface = android.graphics.Typeface.create(
                            "sans-serif",
                            android.graphics.Typeface.BOLD
                        )
                    }
                    canvas.drawText(
                        tx.type.uppercase(Locale.getDefault()),
                        colTypeX,
                        rowTextY,
                        typePaint
                    )
                    canvas.drawText(
                        String.format(
                            Locale.getDefault(),
                            "%s%.2f",
                            currency,
                            tx.amount
                        ),
                        colAmountX,
                        rowTextY,
                        textPaint
                    )

                    currentY += rowHeight
                }

                val footerY = pageHeight - 20f
                canvas.drawText(
                    "Page ${pageIndex + 1} of ${chunks.size}",
                    pageWidth - margin - 60f,
                    footerY,
                    subtitlePaint
                )
                pdfDocument.finishPage(page)
            }

            pdfDocument.writeTo(outStream)
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                outStream.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun truncateForPdf(value: String, maxLen: Int): String =
        if (value.length > maxLen) value.take(maxLen - 2) + ".." else value
}
