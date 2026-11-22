package sh.taf.billi.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sh.taf.billi.ai.AiRepository
import sh.taf.billi.ai.CatalogState
import sh.taf.billi.ai.ProviderStatus
import sh.taf.billi.ai.ladder.LadderState
import sh.taf.billi.ai.local.LocalAiStatus
import sh.taf.billi.ai.provider.AiProviderInfo
import sh.taf.billi.ai.provider.ApiStyle
import sh.taf.billi.core.SnackbarManager
import sh.taf.billi.data.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The AI overview: catalogue, services, the Default ladder, providers, and adding a custom provider. */
class AiViewModel(
    private val repository: AiRepository,
    private val snackbarManager: SnackbarManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Off hides every AI feature in the app. */
    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.update { it.copy(aiEnabled = enabled) } }
    }

    val providers: StateFlow<List<ProviderStatus>> = repository.providers
    val catalog: StateFlow<CatalogState> = repository.catalog
    val ladders: StateFlow<LadderState> = repository.ladders
    val localStatus: StateFlow<LocalAiStatus> = repository.localStatus

    /** True when the local model is in the Default ladder or a service's own ladder. */
    val useLocalAi: StateFlow<Boolean> = repository.config
        .map { sh.taf.billi.ai.local.LocalAiLadders.uses(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.usesLocalAi())

    /** Call when the AI screen opens. Refreshes only a stale catalogue. */
    fun onOpen() {
        viewModelScope.launch {
            repository.refreshIfStale()
            repository.refreshLocalAi()
        }
    }

    fun setUseLocalAi(enabled: Boolean) {
        viewModelScope.launch { repository.setUseLocalAi(enabled) }
    }

    fun refreshLocalAi() {
        viewModelScope.launch { repository.refreshLocalAi() }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            repository.refreshCatalog().onFailure {
                snackbarManager.show("Couldn't refresh the catalogue: ${it.message ?: "unknown error"}")
            }
        }
    }

    /** Adds a provider. [onError] gets a message ready to show, such as an unsafe base URL. */
    fun addCustomProvider(
        name: String,
        style: ApiStyle,
        baseUrl: String,
        onAdded: (AiProviderInfo) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val info = repository.addCustomProvider(name, style, baseUrl)
                snackbarManager.show("Added ${info.name}")
                onAdded(info)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                onError(e.message ?: "Couldn't add the provider")
            }
        }
    }
}
