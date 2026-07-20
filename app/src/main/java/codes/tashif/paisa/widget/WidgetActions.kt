package codes.tashif.paisa.widget

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import codes.tashif.paisa.MainActivity
import codes.tashif.paisa.sms.SmsReaderWorker

/**
 * Deep links from widgets into [MainActivity].
 *
 * The app navigates with local composable state rather than a NavHost, so a
 * widget can't address a route directly. Instead it passes an intent extra that
 * MainActivity hands to the ViewModel as a pending destination, which survives
 * the biometric lock screen and is consumed once shown.
 */
object WidgetDeepLink {
    const val EXTRA_DESTINATION = "codes.tashif.paisa.widget.DESTINATION"

    /** Just brings the app to the front, no specific destination. */
    const val DEST_HOME = "home"

    /** Opens the add-transaction sheet. */
    const val DEST_ADD_TRANSACTION = "add_transaction"

    /** Opens the AI statement import screen (needs a file pick + API key). */
    const val DEST_STATEMENT_IMPORT = "statement_import"

    /** Opens SMS setup, used when a rescan is tapped without READ_SMS granted. */
    const val DEST_SMS_SETUP = "sms_setup"

    /** Opens the Accounts tab. */
    const val DEST_ACCOUNTS = "accounts"

    fun intent(context: Context, destination: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            // Distinct data per destination so PendingIntents aren't deduplicated
            // into each other by the launcher (extras alone don't differentiate).
            data = "paisa://widget/$destination".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_DESTINATION, destination)
        }

    private fun String.toUri() = android.net.Uri.parse(this)
}

/** Per-widget key for the balance privacy toggle. */
internal val BalanceHiddenKey = booleanPreferencesKey("balance_hidden")

/** Per-widget key for expanded account breakdown under income/expense. */
internal val BalanceExpandedKey = booleanPreferencesKey("balance_expanded")

/**
 * Flips the balance widget between visible and masked, so a balance isn't left
 * readable on the home screen by default.
 */
class ToggleBalanceVisibilityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val fallback = loadWidgetSnapshot(context).hideBalancesByDefault
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[BalanceHiddenKey] = !(prefs[BalanceHiddenKey] ?: fallback)
            }
        }
        BalanceWidget().update(context, glanceId)
    }
}

/** Expands or collapses the per-account breakdown on the balance widget. */
class ToggleBalanceExpandedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[BalanceExpandedKey] = !(prefs[BalanceExpandedKey] ?: false)
            }
        }
        BalanceWidget().update(context, glanceId)
    }
}

/**
 * Starts an incremental SMS rescan straight from the home screen — no activity
 * needed. Falls back to opening SMS setup when the permission isn't granted,
 * since a silently failing button would look like a broken widget.
 */
class RescanSmsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (granted) {
            SmsReaderWorker.enqueue(context, forceFull = false)
        } else {
            context.startActivity(
                WidgetDeepLink.intent(context, WidgetDeepLink.DEST_SMS_SETUP)
            )
        }
    }
}
