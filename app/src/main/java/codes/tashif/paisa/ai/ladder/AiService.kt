package codes.tashif.paisa.ai.ladder

import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel

/**
 * A job in the app that calls a model. Each can have its own ladder, or use
 * the default one. Add an entry here to add a service.
 */
enum class AiService(
    val id: String,
    val label: String,
    val description: String,
    /** Rungs whose model is known not to take images are skipped. */
    val needsImage: Boolean,
) {
    STATEMENT_IMPORT(
        id = "statement_import",
        label = "Statement import",
        description = "Text, CSV and PDF text",
        needsImage = false,
    ),
    STATEMENT_PHOTOS(
        id = "statement_photos",
        label = "Statement photos",
        description = "Photos of statements. Needs a model that reads images",
        needsImage = true,
    );

    companion object {
        fun fromId(id: String?): AiService? = entries.firstOrNull { it.id == id }
    }
}

/** One step of a ladder: a model on a provider, how hard it thinks, and the RelayRoom route. */
data class Rung(
    val providerId: String,
    val modelId: String,
    /** Null is Auto. */
    val thinking: ThinkingLevel? = null,
    /** Only used by RelayRoom rungs. */
    val routeMode: RouteMode = RouteMode.AUTO,
)

/** The ladder a service runs on: its own, or the default when it has none. */
data class ServiceLadder(
    val service: AiService,
    /** Null means "Uses default". */
    val own: List<Rung>?,
    val effective: List<Rung>,
) {
    val usesDefault: Boolean get() = own == null
}

data class LadderState(
    val default: List<Rung> = emptyList(),
    val services: List<ServiceLadder> = emptyList(),
) {
    fun forService(service: AiService): List<Rung> =
        services.firstOrNull { it.service == service }?.effective ?: default
}
