package codes.tashif.paisa.ai

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

/**
 * Parses LLM output into [ExtractedTransaction] rows. Tolerant of markdown fences
 * and minor schema drift.
 */
object StatementJsonParser {

    fun parse(modelOutput: String): List<ExtractedTransaction> {
        val jsonText = extractJson(modelOutput) ?: return emptyList()
        return try {
            when {
                jsonText.trimStart().startsWith("[") -> parseArray(JSONArray(jsonText))
                else -> {
                    val obj = JSONObject(jsonText)
                    val arr = when {
                        obj.has("transactions") -> obj.getJSONArray("transactions")
                        obj.has("data") -> obj.getJSONArray("data")
                        else -> null
                    }
                    if (arr != null) parseArray(arr) else emptyList()
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseArray(arr: JSONArray): List<ExtractedTransaction> {
        val out = ArrayList<ExtractedTransaction>(arr.length())
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            parseOne(item)?.let { out.add(it) }
        }
        return out
    }

    private fun parseOne(obj: JSONObject): ExtractedTransaction? {
        val amount = parseAmount(
            obj.opt("amount") ?: obj.opt("Amt") ?: obj.opt("value")
        ) ?: return null
        if (amount <= 0.0 || !amount.isFinite()) return null

        val dateRaw = obj.optString("date")
            .ifBlank { obj.optString("transactionDate") }
            .ifBlank { obj.optString("txn_date") }
            .ifBlank { obj.optString("valueDate") }
        val date = normalizeDate(dateRaw) ?: return null

        val merchant = obj.optString("merchant")
            .ifBlank { obj.optString("description") }
            .ifBlank { obj.optString("narration") }
            .ifBlank { obj.optString("payee") }
            .ifBlank { "Unknown Merchant" }
            .trim()

        val typeRaw = obj.optString("type")
            .ifBlank { obj.optString("transactionType") }
            .ifBlank { obj.optString("debitCredit") }
            .lowercase(Locale.US)
        val type = when {
            typeRaw.contains("income") || typeRaw.contains("credit") ||
                typeRaw == "cr" || typeRaw.contains("deposit") -> "income"
            typeRaw.contains("expense") || typeRaw.contains("debit") ||
                typeRaw == "dr" || typeRaw.contains("withdrawal") -> "expense"
            obj.has("isIncome") && obj.optBoolean("isIncome") -> "income"
            else -> "expense"
        }

        val currency = obj.optString("currency")
            .ifBlank { "INR" }
            .uppercase(Locale.US)
            .take(8)

        val reference = obj.optString("reference")
            .ifBlank { obj.optString("ref") }
            .ifBlank { obj.optString("upiRef") }
            .takeIf { it.isNotBlank() }

        val rawLine = obj.optString("rawLine")
            .ifBlank { obj.optString("raw") }
            .takeIf { it.isNotBlank() }

        val category = obj.optString("category")
            .ifBlank { obj.optString("categoryName") }
            .takeIf { it.isNotBlank() }

        return ExtractedTransaction(
            date = date,
            amount = amount,
            type = type,
            merchant = merchant,
            currency = currency,
            reference = reference,
            rawLine = rawLine,
            categoryName = category
        )
    }

    private fun parseAmount(raw: Any?): Double? {
        return when (raw) {
            is Number -> abs(raw.toDouble())
            is String -> {
                val cleaned = raw
                    .replace(",", "")
                    .replace("₹", "")
                    .replace("INR", "", ignoreCase = true)
                    .replace("Rs.", "", ignoreCase = true)
                    .replace("Rs", "", ignoreCase = true)
                    .replace(" ", "")
                    .trim()
                cleaned.toDoubleOrNull()?.let { abs(it) }
            }
            else -> null
        }
    }

    /**
     * Accepts yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy, dd MMM yyyy, ISO datetime.
     */
    fun normalizeDate(raw: String): String? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        // ISO date or datetime
        val iso = Regex("""(\d{4})-(\d{2})-(\d{2})""").find(s)
        if (iso != null) {
            return "${iso.groupValues[1]}-${iso.groupValues[2]}-${iso.groupValues[3]}"
        }
        val dmySlash = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{4})""").find(s)
        if (dmySlash != null) {
            val d = dmySlash.groupValues[1].padStart(2, '0')
            val m = dmySlash.groupValues[2].padStart(2, '0')
            val y = dmySlash.groupValues[3]
            return "$y-$m-$d"
        }
        val months = mapOf(
            "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
            "may" to "05", "jun" to "06", "jul" to "07", "aug" to "08",
            "sep" to "09", "oct" to "10", "nov" to "11", "dec" to "12"
        )
        val dMonY = Regex(
            """(\d{1,2})\s+([A-Za-z]{3,9})\s+(\d{4})"""
        ).find(s)
        if (dMonY != null) {
            val d = dMonY.groupValues[1].padStart(2, '0')
            val monKey = dMonY.groupValues[2].take(3).lowercase(Locale.US)
            val m = months[monKey] ?: return null
            val y = dMonY.groupValues[3]
            return "$y-$m-$d"
        }
        return null
    }

    fun extractJson(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        // Strip ```json ... ``` fences
        val fence = Regex(
            """```(?:json)?\s*([\s\S]*?)```""",
            RegexOption.IGNORE_CASE
        ).find(trimmed)
        val candidate = fence?.groupValues?.get(1)?.trim() ?: trimmed

        val objStart = candidate.indexOf('{')
        val arrStart = candidate.indexOf('[')
        val start = when {
            objStart < 0 && arrStart < 0 -> return null
            objStart < 0 -> arrStart
            arrStart < 0 -> objStart
            else -> minOf(objStart, arrStart)
        }
        val open = candidate[start]
        val close = if (open == '{') '}' else ']'
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until candidate.length) {
            val c = candidate[i]
            if (inString) {
                when {
                    escape -> escape = false
                    c == '\\' -> escape = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) {
                        return candidate.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }
}
