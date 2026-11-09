package codes.tashif.paisa.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.core.AppNavigator
import codes.tashif.paisa.data.Settings
import codes.tashif.paisa.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The first-run setup: currency and look, then done. Also brings it back from About. */
class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val navigator: AppNavigator
) : ViewModel() {

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setCurrency(symbol: String) = edit { it.copy(currency = symbol) }

    fun setThemeMode(mode: String) = edit { it.copy(themeMode = mode) }

    fun setPalette(id: String) = edit { it.copy(colorPalette = id) }

    /**
     * Marks setup done. With a [destination] (one of AppDestinations), the
     * app opens that screen once it shows, the way a widget tap does.
     */
    fun completeOnboarding(destination: String? = null) {
        navigator.requestWidgetDestination(destination)
        edit { it.copy(onboardingCompleted = true) }
    }

    fun resetOnboarding() = edit { it.copy(onboardingCompleted = false) }

    private fun edit(change: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(change) }
    }
}
