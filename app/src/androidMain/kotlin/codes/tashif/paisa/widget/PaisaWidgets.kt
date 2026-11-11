package codes.tashif.paisa.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * Redraws the home screen widgets after the underlying data changes.
 *
 * Glance widgets don't observe Room flows — `provideGlance` reads a snapshot
 * once — so anything that moves a balance has to poke them explicitly. Safe to
 * call when no widget is placed; `updateAll` is then a no-op.
 */
object PaisaWidgets {
    suspend fun refresh(context: Context) {
        BalanceWidget().updateAll(context)
        QuickActionsWidget().updateAll(context)
    }
}
