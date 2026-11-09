package codes.tashif.paisa.ai.provider

/**
 * One place models can come from. Built-ins live in [BuiltInProviders];
 * custom ones are saved in the AI config.
 */
data class AiProviderInfo(
    val id: String,
    val name: String,
    val style: ApiStyle,
    val defaultBaseUrl: String,
    /** The provider's key in models.dev, when it has one. */
    val modelsDevId: String? = null,
    /** True for api.openai.com itself, which has a few rules compatible servers don't. */
    val nativeOpenAi: Boolean = false,
    val custom: Boolean = false,
    /** Whether the settings screen shows a base URL field. */
    val baseUrlEditable: Boolean = true,
    /**
     * Whether a compatible preset follows the API URL models.dev lists. Off
     * when that URL is wrong for keys made in the provider's own console.
     */
    val followsModelsDevUrl: Boolean = true,
)

object BuiltInProviders {
    const val OPENAI = "openai"
    const val ANTHROPIC = "anthropic"
    const val GOOGLE = "google"
    const val RELAYROOM = "relayroom"
    const val OPENROUTER = "openrouter"
    const val DEEPSEEK = "deepseek"
    const val GROQ = "groq"
    const val NVIDIA = "nvidia"
    const val OPENCODE = "opencode"

    /** The emulator's address for the host computer. A phone needs the computer's LAN address. */
    const val RELAYROOM_DEFAULT_URL = "http://10.0.2.2:8317/v1"
    const val OPENAI_URL = "https://api.openai.com/v1"
    const val ANTHROPIC_URL = "https://api.anthropic.com"
    const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta"
    const val OPENCODE_URL = "https://opencode.ai/zen/v1"

    val all: List<AiProviderInfo> = listOf(
        AiProviderInfo(
            OPENAI, "OpenAI", ApiStyle.OPENAI, OPENAI_URL,
            modelsDevId = "openai", nativeOpenAi = true, baseUrlEditable = false
        ),
        AiProviderInfo(
            ANTHROPIC, "Anthropic", ApiStyle.ANTHROPIC, ANTHROPIC_URL,
            modelsDevId = "anthropic", baseUrlEditable = false
        ),
        AiProviderInfo(
            GOOGLE, "Google Gemini", ApiStyle.GEMINI, GEMINI_URL,
            modelsDevId = "google", baseUrlEditable = false
        ),
        AiProviderInfo(RELAYROOM, "RelayRoom", ApiStyle.RELAYROOM, RELAYROOM_DEFAULT_URL),
        AiProviderInfo(
            OPENROUTER, "OpenRouter", ApiStyle.OPENAI, "https://openrouter.ai/api/v1",
            modelsDevId = "openrouter"
        ),
        AiProviderInfo(
            DEEPSEEK, "DeepSeek", ApiStyle.OPENAI, "https://api.deepseek.com",
            modelsDevId = "deepseek"
        ),
        AiProviderInfo(
            GROQ, "Groq", ApiStyle.OPENAI, "https://api.groq.com/openai/v1",
            modelsDevId = "groq"
        ),
        AiProviderInfo(
            NVIDIA, "NVIDIA", ApiStyle.OPENAI, "https://integrate.api.nvidia.com/v1",
            modelsDevId = "nvidia"
        ),
        AiProviderInfo(
            OPENCODE, "OpenCode Zen", ApiStyle.OPENAI, OPENCODE_URL,
            modelsDevId = "opencode", followsModelsDevUrl = false
        ),
    )

    fun byId(id: String): AiProviderInfo? = all.firstOrNull { it.id == id }
}

/** A provider the user added. Stored in the AI config. */
data class CustomProvider(
    val id: String,
    val name: String,
    val style: ApiStyle,
    val baseUrl: String,
) {
    fun toInfo(): AiProviderInfo = AiProviderInfo(
        id = id,
        name = name,
        style = style,
        defaultBaseUrl = baseUrl,
        custom = true,
    )
}
