package codes.tashif.paisa.ai

import android.content.Context

/** Posts [StatementNotifications] with the application context. */
class AndroidStatementNotifier(private val context: Context) : StatementNotifier {

    override fun notifyExtractionDone(found: Int, duplicates: Int) =
        StatementNotifications.notifyExtractionDone(context, found, duplicates)

    override fun notifyExtractionFailed(message: String) =
        StatementNotifications.notifyExtractionFailed(context, message)
}
