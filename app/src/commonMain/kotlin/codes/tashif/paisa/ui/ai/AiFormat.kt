package codes.tashif.paisa.ui.ai

import codes.tashif.paisa.platform.formatInteger
import codes.tashif.paisa.platform.nowMillis
import codes.tashif.paisa.ai.CatalogState
import codes.tashif.paisa.ai.ProviderStatus
import codes.tashif.paisa.ai.catalog.CatalogSource
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.LadderState
import codes.tashif.paisa.ai.ladder.Rung

// Labels shared by the AI screens and the rows that link to them. Pure.

/** What More and statement import show about the AI setup. */
data class AiSummary(
    /** The first model statement import would run, or null when nothing can run. */
    val modelName: String? = null,
    val providerName: String? = null,
) {
    val ready: Boolean get() = modelName != null

    companion object {
        fun of(ladders: LadderState, providers: List<ProviderStatus>, catalog: CatalogState): AiSummary {
            val withKeys = providers.filter { it.hasKey }.associateBy { it.info.id }
            val rung = AiService.entries.asSequence()
                .flatMap { ladders.forService(it).asSequence() }
                .firstOrNull { it.providerId in withKeys }
                ?: return AiSummary()
            return AiSummary(
                modelName = modelName(rung, catalog),
                providerName = withKeys.getValue(rung.providerId).info.name,
            )
        }
    }
}

fun modelName(rung: Rung, catalog: CatalogState): String =
    catalog.model(rung.providerId, rung.modelId)?.name ?: rung.modelId

/** "Model · Provider" for a rung. */
fun rungLabel(rung: Rung, catalog: CatalogState, providers: List<ProviderStatus>): String {
    val provider = providers.firstOrNull { it.info.id == rung.providerId }?.info?.name ?: rung.providerId
    return "${modelName(rung, catalog)} · $provider"
}

/** The subtitle of a ladder row: its first rung, plus how many follow. */
fun ladderSummary(rungs: List<Rung>, catalog: CatalogState, providers: List<ProviderStatus>): String {
    val first = rungs.firstOrNull() ?: return "No models yet"
    val more = rungs.size - 1
    return rungLabel(first, catalog, providers) + if (more > 0) " · $more more" else ""
}

fun catalogSourceLabel(source: CatalogSource): String = when (source) {
    CatalogSource.NETWORK -> "models.dev"
    CatalogSource.CACHE -> "Saved copy of models.dev"
    CatalogSource.BUILT_IN -> "Built in"
}

fun catalogSubtitle(catalog: CatalogState, now: Long = nowMillis()): String {
    if (catalog.refreshing) return "Refreshing…"
    val parts = mutableListOf(
        "${formatInteger(catalog.modelCount.toLong())} models",
        catalogSourceLabel(catalog.source),
    )
    catalog.fetchedAt?.let { parts += ageLabel(it, now) }
    val line = parts.joinToString(" · ")
    return if (catalog.error != null) "$line\nCouldn't refresh: ${catalog.error}" else line
}

/** "just now", "5 min ago", "3 h ago", "2 days ago". */
fun ageLabel(epochMillis: Long, now: Long = nowMillis()): String {
    val minutes = ((now - epochMillis) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60} h ago"
        minutes < 60 * 48 -> "1 day ago"
        else -> "${minutes / (60 * 24)} days ago"
    }
}

fun providerSubtitle(status: ProviderStatus): String {
    val key = if (status.hasKey) "Key saved" else "No key"
    val models = when (status.modelCount) {
        0 -> "no models listed"
        1 -> "1 model"
        else -> "${status.modelCount} models"
    }
    return "$key · $models"
}
