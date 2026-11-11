package codes.tashif.paisa.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ai.AiTestResult
import codes.tashif.paisa.ai.CatalogState
import codes.tashif.paisa.ai.ProviderStatus
import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.Rung
import codes.tashif.paisa.core.SnackbarManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One provider: its key, base URL, connection test and models. */
class AiProviderViewModel(
    private val repository: AiRepository,
    private val runner: AiLadderRunner,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {

    val providers: StateFlow<List<ProviderStatus>> = repository.providers
    val catalog: StateFlow<CatalogState> = repository.catalog

    /** The last test, with the provider it was for, so another provider's page starts clean. */
    private val _test = MutableStateFlow<Pair<String, AiTestResult>?>(null)
    val test: StateFlow<Pair<String, AiTestResult>?> = _test.asStateFlow()

    private val _refreshing = MutableStateFlow<String?>(null)
    /** The provider whose live model list is loading. */
    val refreshing: StateFlow<String?> = _refreshing.asStateFlow()

    fun savedKey(providerId: String): String = repository.key(providerId).orEmpty()

    /**
     * Saves the base URL when it changed, then the key, then lists the
     * provider's models. [onError] gets the message when the URL is unsafe.
     */
    fun save(status: ProviderStatus, key: String, baseUrl: String?, onError: (String) -> Unit) {
        val id = status.info.id
        viewModelScope.launch {
            if (baseUrl != null && baseUrl.trim().trimEnd('/') != status.baseUrl) {
                try {
                    // Blank or the default clears the override, so the default can change later.
                    val override = baseUrl.takeUnless { it.isBlank() || it.trim().trimEnd('/') == status.info.defaultBaseUrl }
                    repository.setBaseUrl(id, if (status.info.custom) baseUrl.ifBlank { null } else override)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IllegalArgumentException) {
                    onError(e.message ?: "That base URL isn't allowed")
                    return@launch
                }
            }
            if (key.isNotBlank()) repository.saveKey(id, key.trim())
            snackbarManager.show("Saved ${status.info.name}")
            _test.value = null
            if (key.isNotBlank()) refreshModels(id)
        }
    }

    fun removeKey(status: ProviderStatus) {
        viewModelScope.launch {
            repository.removeKey(status.info.id)
            _test.value = null
            snackbarManager.show("Removed the ${status.info.name} key")
        }
    }

    fun testConnection(providerId: String) {
        viewModelScope.launch {
            _test.value = providerId to AiTestResult("Testing…", isError = false)
            val result = runner.testProvider(providerId)
            _test.value = providerId to result.fold(
                onSuccess = { AiTestResult(it, isError = false) },
                onFailure = { AiTestResult("Failed: ${it.message ?: "unknown error"}", isError = true) },
            )
        }
    }

    fun refreshModels(providerId: String) {
        viewModelScope.launch {
            _refreshing.value = providerId
            repository.refreshProviderModels(providerId).onFailure {
                snackbarManager.show("Couldn't list models: ${it.message ?: "unknown error"}")
            }
            _refreshing.value = null
        }
    }

    fun addModel(providerId: String, modelId: String) {
        viewModelScope.launch {
            repository.addModel(providerId, modelId)
            snackbarManager.show("Added ${modelId.trim()}")
        }
    }

    fun removeModel(providerId: String, modelId: String) {
        viewModelScope.launch { repository.removeModel(providerId, modelId) }
    }

    /** Adds a model to the Default ladder (null) or a service's ladder. */
    fun addToLadder(providerId: String, modelId: String, service: AiService?) {
        viewModelScope.launch {
            repository.addRungWithOptions(service, Rung(providerId, modelId))
            snackbarManager.show("Added to ${ladderTitle(service)}")
        }
    }

    fun deleteProvider(status: ProviderStatus, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCustomProvider(status.info.id)
            snackbarManager.show("Deleted ${status.info.name}")
            onDeleted()
        }
    }
}
