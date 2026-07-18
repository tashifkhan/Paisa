package codes.tashif.paisa.ai

/**
 * One row extracted from a bank / UPI statement (before user review & import).
 */
data class ExtractedTransaction(
    val date: String, // yyyy-MM-dd or ISO
    val amount: Double,
    val type: String, // expense | income
    val merchant: String,
    val currency: String = "INR",
    val reference: String? = null,
    val rawLine: String? = null,
    val categoryName: String? = null,
    val selected: Boolean = true,
    /** True when an already-logged transaction (SMS/manual) looks like the same payment. */
    val likelyDuplicate: Boolean = false,
    /** Human-readable hint about what it matched, e.g. "Matches SMS entry on 2026-07-04". */
    val duplicateNote: String? = null
)

data class StatementExtractionResult(
    val transactions: List<ExtractedTransaction>,
    val rawModelOutput: String = "",
    val chunksProcessed: Int = 1
)

data class AiTestResult(
    val message: String,
    val isError: Boolean
)
