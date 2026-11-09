package codes.tashif.paisa.ai.store

import codes.tashif.paisa.ai.provider.AiProviderInfo
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.BuiltInProviders

/** Looks up providers and their base URLs from the config. Pure. */
object ProviderResolution {

    fun all(config: AiConfig): List<AiProviderInfo> =
        BuiltInProviders.all + config.customProviders.map { it.toInfo() }

    fun find(config: AiConfig, id: String): AiProviderInfo? =
        BuiltInProviders.byId(id) ?: config.customProviders.firstOrNull { it.id == id }?.toInfo()

    /**
     * The user's override first. For compatible presets, then the URL
     * models.dev lists, which follows a provider that moves its API. Then
     * the built-in default.
     */
    fun baseUrl(provider: AiProviderInfo, config: AiConfig, modelsDevApi: String? = null): String {
        config.baseUrls[provider.id]?.takeIf { it.isNotBlank() }?.let { return it }
        val preset = !provider.custom && !provider.nativeOpenAi && provider.style == ApiStyle.OPENAI &&
            provider.followsModelsDevUrl
        if (preset && modelsDevApi != null) return modelsDevApi.trimEnd('/')
        return provider.defaultBaseUrl
    }

    /** Normalises a pasted base URL: no trailing slash, no endpoint path. */
    fun normalize(url: String): String =
        url.trim().trimEnd('/').removeSuffix("/chat/completions").trimEnd('/')

    /** A readable, unique id for a new custom provider. */
    fun newCustomId(name: String, taken: Set<String>): String {
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(24).ifEmpty { "provider" }
        var id = "custom-$slug"
        var n = 2
        while (id in taken || BuiltInProviders.byId(id) != null) id = "custom-$slug-${n++}"
        return id
    }
}
