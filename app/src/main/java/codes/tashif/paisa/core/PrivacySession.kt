package codes.tashif.paisa.core

import codes.tashif.paisa.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether balances are masked right now. Starts hidden, then follows
 * Settings.hideBalancesByDefault once per session; after that only the
 * user's toggles change it.
 */
@Singleton
class PrivacySession @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

    private val _balancesHidden = MutableStateFlow(true)
    val balancesHidden: StateFlow<Boolean> = _balancesHidden.asStateFlow()

    fun toggle() {
        _balancesHidden.value = !_balancesHidden.value
    }

    fun set(hidden: Boolean) {
        _balancesHidden.value = hidden
    }

    /**
     * Starts a session: hides balances, waits for the first settings row and
     * seeds from hideBalancesByDefault. Call once from the activity-scoped
     * ViewModel's init, in viewModelScope, so a new MainActivity starts hidden
     * again just as it did when this state lived in that ViewModel.
     */
    suspend fun startSession() {
        _balancesHidden.value = true
        val settings = settingsRepository.settings.filterNotNull().first()
        _balancesHidden.value = settings.hideBalancesByDefault
    }
}
