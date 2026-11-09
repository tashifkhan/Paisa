package codes.tashif.paisa.ui.ai

import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.Rung
import codes.tashif.paisa.ai.provider.BuiltInProviders

/** List edits for the ladder screen. Pure, so they're easy to test. */
object LadderEdits {

    /** Moves the rung at [index] by [delta] places. Out-of-range moves return the list unchanged. */
    fun move(rungs: List<Rung>, index: Int, delta: Int): List<Rung> {
        val target = index + delta
        if (index !in rungs.indices || target !in rungs.indices) return rungs
        return rungs.toMutableList().apply { add(target, removeAt(index)) }
    }

    fun remove(rungs: List<Rung>, index: Int): List<Rung> =
        if (index in rungs.indices) rungs.filterIndexed { i, _ -> i != index } else rungs

    fun update(rungs: List<Rung>, index: Int, change: (Rung) -> Rung): List<Rung> =
        rungs.mapIndexed { i, rung -> if (i == index) change(rung) else rung }
}

/**
 * Adds a rung, then for RelayRoom fetches the model's exact thinking levels
 * and inputs. A failed lookup is fine: the catalogue keeps what it knew.
 */
suspend fun AiRepository.addRungWithOptions(service: AiService?, rung: Rung) {
    addRung(service, rung)
    if (rung.providerId == BuiltInProviders.RELAYROOM) {
        refreshRelayModelOptions(rung.modelId, rung.routeMode)
    }
}

/** Where a model can be added: the Default ladder (null) or a service. */
fun ladderTargets(): List<AiService?> = listOf<AiService?>(null) + AiService.entries

fun ladderTitle(service: AiService?): String = service?.label ?: "Default ladder"
