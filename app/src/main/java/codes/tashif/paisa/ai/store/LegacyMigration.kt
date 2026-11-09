package codes.tashif.paisa.ai.store

import codes.tashif.paisa.ai.AiCredentials
import codes.tashif.paisa.ai.AiProvider
import codes.tashif.paisa.ai.ladder.Rung
import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.CustomProvider
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Moves the old single [AiCredentials] (provider, key, base URL, model) to a
 * key for that provider plus a one-rung Default ladder. Also backs the old
 * settings screen, which still reads and writes [AiCredentials]. Pure.
 */
object LegacyMigration {

    data class Migration(val providerId: String, val apiKey: String, val config: AiConfig)

    /** Null when the old settings held no usable key; the caller still marks migration done. */
    fun migrate(legacy: AiCredentials, config: AiConfig): Migration? {
        if (!legacy.isConfigured) return null
        val (providerId, withProvider) = providerFor(legacy, config)
        val model = legacy.model.trim()
        val next = withProvider.copy(
            defaultLadder = withProvider.defaultLadder.ifEmpty { listOf(Rung(providerId, model)) },
            manualModels = addModel(withProvider.manualModels, providerId, model),
            legacyMigrated = true,
        )
        return Migration(providerId, legacy.apiKey.trim(), next)
    }

    /** What the old settings screen saves: its provider's key plus a one-rung Default ladder. */
    fun applyOldScreen(creds: AiCredentials, config: AiConfig): Migration {
        val (providerId, withProvider) = providerFor(creds, config)
        val model = creds.model.trim().ifEmpty { creds.provider.defaultModel }
        return Migration(
            providerId = providerId,
            apiKey = creds.apiKey.trim(),
            config = withProvider.copy(
                defaultLadder = listOf(Rung(providerId, model)),
                manualModels = addModel(withProvider.manualModels, providerId, model),
            ),
        )
    }

    /** Finds the provider id for old credentials, adding a custom provider when nothing matches. */
    fun providerFor(creds: AiCredentials, config: AiConfig): Pair<String, AiConfig> {
        val base = ProviderResolution.normalize(creds.baseUrl)
        return when (creds.provider) {
            AiProvider.GEMINI -> builtIn(BuiltInProviders.GOOGLE, BuiltInProviders.GEMINI_URL, base, config)
            AiProvider.ANTHROPIC -> builtIn(BuiltInProviders.ANTHROPIC, BuiltInProviders.ANTHROPIC_URL, base, config)
            AiProvider.OPENAI_COMPATIBLE -> openAiCompatible(base, config)
        }
    }

    private fun builtIn(id: String, defaultUrl: String, base: String, config: AiConfig): Pair<String, AiConfig> {
        if (base.isEmpty() || base == defaultUrl) return id to config
        return id to config.copy(baseUrls = config.baseUrls + (id to base))
    }

    private fun openAiCompatible(base: String, config: AiConfig): Pair<String, AiConfig> {
        if (base.isEmpty() || base == BuiltInProviders.OPENAI_URL) return BuiltInProviders.OPENAI to config
        BuiltInProviders.all
            .firstOrNull { it.style == ApiStyle.OPENAI && it.defaultBaseUrl == base }
            ?.let { return it.id to config }
        config.customProviders
            .firstOrNull { it.style == ApiStyle.OPENAI && ProviderResolution.normalize(it.baseUrl) == base }
            ?.let { return it.id to config }
        val host = base.toHttpUrlOrNull()?.host ?: base.substringAfter("://").substringBefore('/')
        val name = "Custom ($host)"
        val taken = config.customProviders.mapTo(HashSet()) { it.id }
        val custom = CustomProvider(ProviderResolution.newCustomId(host, taken), name, ApiStyle.OPENAI, base)
        return custom.id to config.copy(customProviders = config.customProviders + custom)
    }

    /** The rung the old screen shows and edits. */
    fun shownRung(config: AiConfig, keyFor: (String) -> String?): Rung? =
        config.defaultLadder.firstOrNull { keyFor(it.providerId) != null } ?: config.defaultLadder.firstOrNull()

    private fun addModel(manual: Map<String, List<String>>, providerId: String, model: String): Map<String, List<String>> {
        val existing = manual[providerId].orEmpty()
        return if (model in existing) manual else manual + (providerId to existing + model)
    }

    /**
     * The old [AiCredentials] view of the new settings: the Default ladder's
     * first rung with a key (or its first rung) and that key. RelayRoom shows as OpenAI-compatible, the
     * closest old choice.
     */
    fun toLegacy(
        config: AiConfig,
        keyFor: (String) -> String?,
        baseUrlFor: (AiProviderInfo) -> String,
    ): AiCredentials {
        val rung = shownRung(config, keyFor) ?: return AiCredentials()
        val provider = ProviderResolution.find(config, rung.providerId) ?: return AiCredentials()
        val old = when (provider.style) {
            ApiStyle.ANTHROPIC -> AiProvider.ANTHROPIC
            ApiStyle.GEMINI -> AiProvider.GEMINI
            ApiStyle.OPENAI, ApiStyle.RELAYROOM -> AiProvider.OPENAI_COMPATIBLE
        }
        return AiCredentials(
            provider = old,
            apiKey = keyFor(provider.id).orEmpty(),
            baseUrl = baseUrlFor(provider),
            model = rung.modelId,
        )
    }
}
