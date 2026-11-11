package codes.tashif.paisa.ai.catalog

import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.ThinkingLevel

/** Builds one provider's model list from models.dev, the live listing and hand-added ids. */
object CatalogMerge {

    /**
     * A live listing, when there is one, decides which models exist; models.dev
     * fills in what each can do. Without a listing, models.dev's list is used,
     * then the small built-in list. Hand-added ids always appear.
     */
    fun modelsFor(
        provider: AiProviderInfo,
        modelsDev: ModelsDevSnapshot,
        live: LiveList?,
        manual: List<String>,
    ): List<ModelInfo> {
        val devList = provider.modelsDevId?.let { modelsDev.providers[it]?.models }.orEmpty()
        val devById = devList.associateBy { it.id }
        fun known(id: String): ModelInfo? = devById[id] ?: modelsDev.lookup(id)

        val base = when {
            live != null && live.models.isNotEmpty() ->
                live.models.map { if (it.alias) it else it.enrichedBy(known(it.id)) }
            devList.isNotEmpty() -> devList
            else -> BuiltInModels.forProvider(provider.id)
        }
        val ids = base.mapTo(HashSet()) { it.id }
        val added = manual.filter { it.isNotBlank() && it !in ids }.distinct().map { id ->
            ModelInfo(id = id, source = ModelSource.MANUAL).enrichedBy(known(id))
        }
        return base + added
    }
}

/** A few known-good models per provider, for when models.dev can't be reached. */
object BuiltInModels {
    private val vision = setOf("text", "image")
    private val visionPdf = setOf("text", "image", "pdf")

    private val all: Map<String, List<ModelInfo>> = mapOf(
        BuiltInProviders.OPENAI to listOf(
            ModelInfo("gpt-4o-mini", "GPT-4o mini", visionPdf, reasoning = false, source = ModelSource.BUILT_IN),
            ModelInfo("gpt-4.1-mini", "GPT-4.1 mini", visionPdf, reasoning = false, source = ModelSource.BUILT_IN),
            ModelInfo(
                "gpt-5-mini", "GPT-5 mini", vision, reasoning = true,
                reasoningLevels = listOf(ThinkingLevel.MINIMAL, ThinkingLevel.LOW, ThinkingLevel.MEDIUM, ThinkingLevel.HIGH),
                source = ModelSource.BUILT_IN
            ),
        ),
        BuiltInProviders.ANTHROPIC to listOf(
            ModelInfo(
                "claude-haiku-4-5", "Claude Haiku 4.5", visionPdf, reasoning = true,
                reasoningLevels = emptyList(), budgetThinking = true, outputLimit = 64_000,
                source = ModelSource.BUILT_IN
            ),
            ModelInfo(
                "claude-sonnet-4-5", "Claude Sonnet 4.5", visionPdf, reasoning = true,
                reasoningLevels = emptyList(), budgetThinking = true, outputLimit = 64_000,
                source = ModelSource.BUILT_IN
            ),
        ),
        BuiltInProviders.GOOGLE to listOf(
            ModelInfo(
                "gemini-2.5-flash", "Gemini 2.5 Flash", visionPdf, reasoning = true,
                reasoningLevels = listOf(ThinkingLevel.NONE), budgetThinking = true,
                budgetMin = 0, budgetMax = 24_576, source = ModelSource.BUILT_IN
            ),
            ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", visionPdf, reasoning = false, source = ModelSource.BUILT_IN),
        ),
    )

    fun forProvider(id: String): List<ModelInfo> = all[id].orEmpty()
}
