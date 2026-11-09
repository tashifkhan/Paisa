package codes.tashif.paisa.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ai.CatalogState
import codes.tashif.paisa.ai.ProviderStatus
import codes.tashif.paisa.ai.ladder.LadderState
import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.core.SnackbarManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The AI overview: catalogue, services, the Default ladder, providers, and adding a custom provider. */
class AiViewModel(
    private val repository: AiRepository,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {

    val providers: StateFlow<List<ProviderStatus>> = repository.providers
    val catalog: StateFlow<CatalogState> = repository.catalog
    val ladders: StateFlow<LadderState> = repository.ladders

    /** Call when the AI screen opens. Refreshes only a stale catalogue. */
    fun onOpen() {
        viewModelScope.launch { repository.refreshIfStale() }
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
