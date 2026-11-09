package codes.tashif.paisa.ai

/** An image sent inline to a vision model. */
data class LlmImage(
    val mimeType: String,
    val base64Data: String
)

/** A PDF sent as a file to a model that reads PDFs. */
data class LlmPdf(
    val base64Data: String,
    val name: String? = null
)

/** What went wrong with one model call. The ladder moves on for every kind. */
enum class LlmFailure { HTTP, NETWORK, TIMEOUT, EMPTY, BAD_JSON, CUT_OFF, CONFIG }

open class LlmException(
    message: String,
    val failure: LlmFailure = LlmFailure.CONFIG,
    /** HTTP status, or 0 when there was no response. */
    val status: Int = 0,
    cause: Throwable? = null
) : Exception(message, cause) {
    /** 429 and 5xx are worth one more try on the same model. */
    val retryable: Boolean get() = failure == LlmFailure.HTTP && (status == 429 || status >= 500)
}
