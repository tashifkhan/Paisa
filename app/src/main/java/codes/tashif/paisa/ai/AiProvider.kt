package codes.tashif.paisa.ai

/**
 * Bring-your-own-key LLM providers for statement extraction.
 * SMS parsing never uses these — cloud is opt-in only.
 */
enum class AiProvider(
    val id: String,
    val label: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val requiresBaseUrl: Boolean
) {
    OPENAI_COMPATIBLE(
        id = "openai_compatible",
        label = "OpenAI-compatible",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        requiresBaseUrl = true
    ),
    GEMINI(
        id = "gemini",
        label = "Google Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-2.0-flash",
        requiresBaseUrl = false
    ),
    ANTHROPIC(
        id = "anthropic",
        label = "Anthropic",
        defaultBaseUrl = "https://api.anthropic.com",
        defaultModel = "claude-3-5-haiku-latest",
        requiresBaseUrl = false
    );

    companion object {
        fun fromId(id: String?): AiProvider =
            entries.firstOrNull { it.id == id } ?: OPENAI_COMPATIBLE
    }
}

data class AiCredentials(
    val provider: AiProvider = AiProvider.OPENAI_COMPATIBLE,
    val apiKey: String = "",
    val baseUrl: String = AiProvider.OPENAI_COMPATIBLE.defaultBaseUrl,
    val model: String = AiProvider.OPENAI_COMPATIBLE.defaultModel
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && model.isNotBlank() &&
            (!provider.requiresBaseUrl || baseUrl.isNotBlank())
}
