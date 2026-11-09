package codes.tashif.paisa.ai

/**
 * Tells the user a statement finished processing while they were elsewhere.
 * Android posts a notification; the web does nothing.
 */
interface StatementNotifier {
    fun notifyExtractionDone(found: Int, duplicates: Int)

    fun notifyExtractionFailed(message: String)
}
