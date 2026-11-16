package codes.tashif.paisa.ai

import codes.tashif.paisa.ai.json.JSONObject
import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.provider.LlmRequest
import codes.tashif.paisa.statement.StatementImageReader
import codes.tashif.paisa.statement.StatementJsonParser
import codes.tashif.paisa.statement.StatementSource
import codes.tashif.paisa.statement.StatementTextExtractor

/** One line of a bill: what it was and what it cost in all. */
data class ScannedItem(val name: String, val amount: Double)

/** A bill read by the model. [total] is what was paid, after tax, tip and discounts. */
data class ScannedBill(val items: List<ScannedItem>, val total: Double?)

/**
 * Reads the line items off a restaurant bill or receipt, so a split can give
 * each item to the people who had it. Runs on the "Quick add" ladder.
 */
class BillScanService(private val ladderRunner: AiLadderRunner) {

    fun canRun(): Boolean = ladderRunner.canRun(AiService.QUICK_ADD)

    suspend fun scan(file: StatementSource): ScannedBill {
        val reply = when {
            StatementImageReader.isImage(file) ->
                complete("Read the attached bill.", image = StatementImageReader.read(file))
            else -> {
                val text = StatementTextExtractor.extract(file)
                when {
                    text.isNotBlank() -> complete("The bill's text:\n\n${text.take(MAX_TEXT_CHARS)}")
                    StatementTextExtractor.isPdf(file) -> complete(
                        "Read the attached bill.",
                        pdf = StatementTextExtractor.readPdf(file) ?: throw LlmException("Couldn't open this PDF")
                    )
                    else -> throw LlmException("Couldn't read this file. Try a photo of the bill.")
                }
            }
        }
        val bill = BillScanParser.parse(reply)
        if (bill.items.isEmpty()) throw LlmException("No items found on that bill")
        return bill
    }

    private suspend fun complete(prompt: String, image: LlmImage? = null, pdf: LlmPdf? = null): String =
        ladderRunner.run(
            AiService.QUICK_ADD,
            LlmRequest(
                system = SYSTEM_PROMPT,
                prompt = prompt,
                json = true,
                image = image,
                pdf = pdf,
                feature = "bill_scan"
            )
        ).text

    companion object {
        private const val MAX_TEXT_CHARS = 24_000

        val SYSTEM_PROMPT = """
            You read restaurant bills, grocery receipts and invoices so friends can split them.
            Return ONLY valid JSON (no markdown, no commentary) with this shape:
            {
              "items": [
                { "name": "Paneer tikka", "amount": 320 }
              ],
              "total": 1180
            }
            Rules:
            - One entry per line item, in the order printed. Keep names short, as printed.
            - amount is what that line cost in all: quantity times unit price. A positive plain
              number with no currency symbol or thousands separators.
            - Items only. Leave out tax, GST, VAT, service charge, tip, delivery fee, packing
              charges, round off and discounts. They are worked out from the total.
            - total is the final amount paid, after every tax, charge and discount. Leave it out
              if the bill doesn't show one.
            - Never invent items. If you can't read any, return {"items": []}.
        """.trimIndent()
    }
}

/** Reads a [ScannedBill] out of model output. Tolerant of fences and loose types. */
object BillScanParser {
    fun parse(modelOutput: String): ScannedBill {
        val jsonText = StatementJsonParser.extractJson(modelOutput) ?: return ScannedBill(emptyList(), null)
        return try {
            val obj = JSONObject(jsonText)
            val array = obj.optJSONArray("items")
            val items = if (array == null) {
                emptyList()
            } else {
                (0 until array.length()).mapNotNull { i ->
                    val item = array.optJSONObject(i) ?: return@mapNotNull null
                    val amount = number(item.opt("amount") ?: item.opt("price") ?: item.opt("total"))
                        ?: return@mapNotNull null
                    if (amount <= 0.0 || !amount.isFinite()) return@mapNotNull null
                    val name = item.optString("name").ifBlank { item.optString("item") }.trim()
                    ScannedItem(name.ifBlank { "Item ${i + 1}" }.take(60), amount)
                }
            }
            ScannedBill(items, number(obj.opt("total"))?.takeIf { it > 0.0 && it.isFinite() })
        } catch (_: Exception) {
            ScannedBill(emptyList(), null)
        }
    }

    private fun number(raw: Any?): Double? = when (raw) {
        null -> null
        is Number -> raw.toDouble()
        else -> raw.toString().replace(Regex("""[^\d.]"""), "").toDoubleOrNull()
    }
}
