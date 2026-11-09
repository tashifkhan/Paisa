package codes.tashif.paisa.ai

/** The web shows the result on the import screen only, so this does nothing. */
class WebStatementNotifier : StatementNotifier {
    override fun notifyExtractionDone(found: Int, duplicates: Int) = Unit

    override fun notifyExtractionFailed(message: String) = Unit
}
