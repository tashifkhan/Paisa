package sh.taf.billi.assistant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether assistants such as Gemini may use Billi's actions. On by default.
 * Turning it off makes every action refuse, without uninstalling anything.
 * The setting stays on this device and is not backed up or synced.
 */
interface AssistantAccess {
    /** False where no assistant can reach the app, such as the web. */
    val supported: Boolean
    val enabled: StateFlow<Boolean>
    fun setEnabled(enabled: Boolean)
}

/** For platforms without assistant actions. */
object NoAssistantAccess : AssistantAccess {
    override val supported = false
    override val enabled: StateFlow<Boolean> = MutableStateFlow(false)
    override fun setEnabled(enabled: Boolean) = Unit
}
