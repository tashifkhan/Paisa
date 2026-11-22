package sh.taf.billi.assistant

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [AssistantAccess] in its own small SharedPreferences file. */
class PrefsAssistantAccess(context: Context) : AssistantAccess {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getBoolean(KEY, true))

    override val supported = true
    override val enabled: StateFlow<Boolean> = state.asStateFlow()

    override fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY, enabled) }
        state.value = enabled
    }

    private companion object {
        const val PREFS = "billi_assistant"
        const val KEY = "enabled"
    }
}
