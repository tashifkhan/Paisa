package codes.tashif.paisa.ui.statement

import codes.tashif.paisa.ai.AiCredentials
import codes.tashif.paisa.ai.AiCredentialsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The saved AI credentials as a flow. AI settings writes them and statement
 * import reads them, from two ViewModels, so the store's snapshot lives here
 * where both see the same value.
 */
@Singleton
class AiCredentialsHolder @Inject constructor(
    private val store: AiCredentialsStore
) {

    private val _credentials = MutableStateFlow(store.load())
    val credentials: StateFlow<AiCredentials> = _credentials.asStateFlow()

    fun save(credentials: AiCredentials) {
        store.save(credentials)
        _credentials.value = store.load()
    }

    fun clear() {
        store.clear()
        _credentials.value = store.load()
    }
}
