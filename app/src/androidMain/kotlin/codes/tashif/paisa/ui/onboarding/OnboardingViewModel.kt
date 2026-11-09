package codes.tashif.paisa.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.data.repository.SettingsRepository
import kotlinx.coroutines.launch

/** Marks the feature tour done, or brings it back from About. */
class OnboardingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            val current = settingsRepository.getSettings() ?: return@launch
            settingsRepository.update(current.copy(onboardingCompleted = true))
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            val current = settingsRepository.getSettings() ?: return@launch
            settingsRepository.update(current.copy(onboardingCompleted = false))
        }
    }
}
