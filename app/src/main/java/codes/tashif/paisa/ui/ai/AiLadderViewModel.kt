package codes.tashif.paisa.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ai.CatalogState
import codes.tashif.paisa.ai.ProviderStatus
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.LadderState
import codes.tashif.paisa.ai.ladder.Rung
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Edits one ladder: the Default one ([AiService] null) or a service's. Edits
 * to a service that uses the default give it its own ladder.
 */
@HiltViewModel
class AiLadderViewModel @Inject constructor(
    private val repository: AiRepository,
) : ViewModel() {

    val providers: StateFlow<List<ProviderStatus>> = repository.providers
    val catalog: StateFlow<CatalogState> = repository.catalog
    val ladders: StateFlow<LadderState> = repository.ladders

    /** Levels for the rung's own model and, on RelayRoom, its own route mode. */
    fun thinkingChoices(rung: Rung): List<ThinkingLevel> =
        repository.thinkingChoices(rung.providerId, rung.modelId, rung.routeMode)

    fun rungs(service: AiService?): List<Rung> =
        if (service == null) ladders.value.default else ladders.value.forService(service)

    fun move(service: AiService?, index: Int, delta: Int) =
        edit(service) { LadderEdits.move(it, index, delta) }

    fun remove(service: AiService?, index: Int) = edit(service) { LadderEdits.remove(it, index) }

    fun setThinking(service: AiService?, index: Int, level: ThinkingLevel?) =
        edit(service) { rungs -> LadderEdits.update(rungs, index) { it.copy(thinking = level) } }

    fun setRouteMode(service: AiService?, index: Int, mode: RouteMode) {
        viewModelScope.launch {
            val rung = repository.editLadder(service) { rungs ->
                LadderEdits.update(rungs, index) { it.copy(routeMode = mode) }
            }.getOrNull(index) ?: return@launch
            if (rung.providerId == BuiltInProviders.RELAYROOM && rung.routeMode == mode) {
                repository.refreshRelayModelOptions(rung.modelId, mode)
            }
        }
    }

    // RelayRoom model and route pairs whose options this screen already asked for.
    private val optionsAsked = mutableSetOf<Pair<String, RouteMode>>()

    /**
     * Asks RelayRoom for the exact options of each of its rungs once per
     * session, so a ladder saved earlier shows current thinking and input choices.
     */
    fun loadRouteOptions(rungs: List<Rung>) {
        rungs.filter { it.providerId == BuiltInProviders.RELAYROOM }
            .map { it.modelId to it.routeMode }
            .filter { optionsAsked.add(it) }
            .forEach { (model, mode) ->
                viewModelScope.launch { repository.refreshRelayModelOptions(model, mode) }
            }
    }

    fun add(service: AiService?, providerId: String, modelId: String) {
        viewModelScope.launch { repository.addRungWithOptions(service, Rung(providerId, modelId)) }
    }

    fun useDefault(service: AiService) {
        viewModelScope.launch { repository.useDefaultLadder(service) }
    }

    /**
     * Every edit runs on the latest ladder inside the config store's lock.
     * Removing the last rung of a service's own ladder reads as "use
     * default", which is what an empty ladder means.
     */
    private fun edit(service: AiService?, change: (List<Rung>) -> List<Rung>) {
        viewModelScope.launch { repository.editLadder(service, change) }
    }
}
