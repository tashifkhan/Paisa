package codes.tashif.paisa.ui.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Semantic haptic vocabulary for the app. Use these instead of raw
 * [HapticFeedbackType]s so every screen speaks the same physical language:
 * - [tick] for moving between options (tabs, chips, segmented buttons, pickers)
 * - [click] for neutral one-off actions (FAB, opening a sheet or menu)
 * - [toggle] for on/off state changes (switches, checkboxes, selection)
 * - [confirm] for successfully committing something (save, add, merge)
 * - [reject] for destructive or failed actions (delete, validation error)
 * - [longPress] for entering a selection/contextual mode
 */
class Haptics(private val feedback: HapticFeedback) {
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    fun click() = feedback.performHapticFeedback(HapticFeedbackType.ContextClick)
    fun toggle(on: Boolean) = feedback.performHapticFeedback(
        if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
    )
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)
    fun longPress() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
