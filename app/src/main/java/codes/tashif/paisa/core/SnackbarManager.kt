package codes.tashif.paisa.core

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/** One snackbar stream for the whole app. The shell collects [events] and shows them. */
@Singleton
class SnackbarManager @Inject constructor() {

    // Messages wait here until the shell collects them, so one sent from a
    // full-screen page (like deleting a transaction) shows once you're back.
    private val _events = Channel<SnackbarEvent>(capacity = 16)
    val events: Flow<SnackbarEvent> = _events.receiveAsFlow()

    fun show(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        _events.trySend(SnackbarEvent(message, actionLabel, onAction))
    }
}
