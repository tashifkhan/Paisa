package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel

/** Where a catalogue entry came from. */
enum class ModelSource { LIVE, MODELS_DEV, MANUAL, BUILT_IN }

/**
 * One model a provider can run. Capability fields are null when nobody told
 * us, which is different from "no".
 */
data class ModelInfo(
    val id: String,
    val name: String = id,
    /** Input kinds such as text, image, pdf. Null when unknown. */
    val input: Set<String>? = null,
    val reasoning: Boolean? = null,
    /**
     * Levels from models.dev `reasoning_options` (effort values, plus None for
     * a toggle) or RelayRoom `/model-options`. Null when the source listed none.
     */
    val reasoningLevels: List<ThinkingLevel>? = null,
    /** models.dev listed a `budget_tokens` option. */
    val budgetThinking: Boolean = false,
    val budgetMin: Int? = null,
    val budgetMax: Int? = null,
    val contextWindow: Int? = null,
    val outputLimit: Int? = null,
    /** RelayRoom: the route modes this model accepts. */
    val routeModes: List<RouteMode> = emptyList(),
    /** RelayRoom: an operator alias such as `opus` rather than one model. */
    val alias: Boolean = false,
    /** RelayRoom: the upstream provider's display name. */
    val upstream: String? = null,
    val source: ModelSource = ModelSource.MODELS_DEV,
) {
    /** True, false, or null when the catalogue doesn't know. */
    val acceptsImage: Boolean? get() = input?.contains("image")
    val acceptsPdf: Boolean? get() = input?.contains("pdf")
    val acceptsAudio: Boolean? get() = input?.contains("audio")

    /**
     * The levels to offer for this model on a provider of [style], not
     * counting Auto. Anthropic never gets budget-derived levels, because
     * Paisa only sends adaptive thinking there.
     */
    fun thinkingChoices(style: ApiStyle): List<ThinkingLevel> {
        if (reasoning != true) return emptyList()
        val listed = reasoningLevels ?: return ThinkingLevel.FALLBACK
        val levels = listed.toMutableSet()
        if (budgetThinking && style != ApiStyle.ANTHROPIC) {
            levels += ThinkingLevel.FALLBACK
            if (budgetMin == 0) levels += ThinkingLevel.NONE
        }
        return ThinkingLevel.entries.filter { it in levels }
    }

    /** Fills unknown fields of this entry from [other], keeping what this one knows. */
    fun enrichedBy(other: ModelInfo?): ModelInfo {
        if (other == null) return this
        return copy(
            name = if (name == id) other.name else name,
            input = input ?: other.input,
            reasoning = reasoning ?: other.reasoning,
            reasoningLevels = reasoningLevels ?: other.reasoningLevels,
            budgetThinking = budgetThinking || other.budgetThinking,
            budgetMin = budgetMin ?: other.budgetMin,
            budgetMax = budgetMax ?: other.budgetMax,
            contextWindow = contextWindow ?: other.contextWindow,
            outputLimit = outputLimit ?: other.outputLimit,
        )
    }
}
