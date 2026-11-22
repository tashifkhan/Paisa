package sh.taf.billi.assistant

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import sh.taf.billi.R
import sh.taf.billi.ui.app.AppDestinations
import sh.taf.billi.widget.WidgetDeepLink

/**
 * The shortcuts on Billi's launcher icon (long-press it), which also show up
 * in Android's search and can be pinned. They're published from code rather
 * than res/xml/shortcuts.xml so they point at whichever application id this
 * build has, and so the AI one goes away when AI is off. Each opens
 * MainActivity through the same deep link the widgets use.
 */
object AppShortcuts {

    private data class Shortcut(
        val id: String,
        val destination: String,
        val shortLabel: Int,
        val longLabel: Int,
        val icon: Int,
        val needsAi: Boolean = false
    )

    private val all = listOf(
        Shortcut("add", AppDestinations.ADD_TRANSACTION, R.string.shortcut_add_short, R.string.shortcut_add_long, R.drawable.ic_shortcut_add),
        Shortcut("add_ai", AppDestinations.ADD_WITH_AI, R.string.shortcut_ai_short, R.string.shortcut_ai_long, R.drawable.ic_shortcut_ai, needsAi = true),
        Shortcut("budgets", AppDestinations.BUDGETS, R.string.shortcut_budgets_short, R.string.shortcut_budgets_long, R.drawable.ic_shortcut_budgets),
        Shortcut("analytics", AppDestinations.ANALYTICS, R.string.shortcut_analytics_short, R.string.shortcut_analytics_long, R.drawable.ic_shortcut_analytics)
    )

    /** Replaces Billi's shortcuts with the ones that fit [aiEnabled]. Safe to call often. */
    fun publish(context: Context, aiEnabled: Boolean) {
        val shown = all.filter { aiEnabled || !it.needsAi }
        val infos = shown.mapIndexed { rank, s ->
            ShortcutInfoCompat.Builder(context, s.id)
                .setShortLabel(context.getString(s.shortLabel))
                .setLongLabel(context.getString(s.longLabel))
                .setIcon(IconCompat.createWithResource(context, s.icon))
                .setIntent(WidgetDeepLink.intent(context, s.destination))
                .setRank(rank)
                .build()
        }
        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(context, infos)
            // A pinned "Add with AI" stays on the home screen, so grey it out
            // with a reason instead of leaving a dead button.
            val aiIds = all.filter { it.needsAi }.map { it.id }
            if (aiEnabled) {
                ShortcutManagerCompat.enableShortcuts(context, infos.filter { it.id in aiIds })
            } else {
                ShortcutManagerCompat.disableShortcuts(context, aiIds, context.getString(R.string.shortcut_ai_off))
            }
        }
    }
}
