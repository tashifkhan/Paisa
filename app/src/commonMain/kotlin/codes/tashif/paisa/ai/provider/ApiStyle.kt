package codes.tashif.paisa.ai.provider

/** The wire format a provider speaks. Each style has one request builder. */
enum class ApiStyle(val id: String, val label: String) {
    OPENAI("openai", "OpenAI-compatible"),
    ANTHROPIC("anthropic", "Anthropic-compatible"),
    GEMINI("gemini", "Gemini-compatible"),
    RELAYROOM("relayroom", "RelayRoom");

    /** Whether this style can carry a PDF attachment as a file. */
    val carriesPdf: Boolean get() = this != OPENAI

    /** Whether this style can carry a voice recording. Anthropic's API takes no audio. */
    val carriesAudio: Boolean get() = this != ANTHROPIC

    companion object {
        fun fromId(id: String?): ApiStyle? = entries.firstOrNull { it.id == id }

        /** The styles a user can pick for a custom provider. */
        val customStyles: List<ApiStyle> = listOf(OPENAI, ANTHROPIC, GEMINI)
    }
}

/**
 * How hard a model should think. Auto is "no level", so it is `null` wherever
 * a level is stored, and the provider default applies.
 */
enum class ThinkingLevel(val wire: String, val label: String) {
    NONE("none", "None"),
    MINIMAL("minimal", "Minimal"),
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High"),
    XHIGH("xhigh", "Extra high"),
    MAX("max", "Max");

    companion object {
        const val AUTO_LABEL = "Auto"

        fun fromWire(value: String?): ThinkingLevel? =
            entries.firstOrNull { it.wire == value?.trim()?.lowercase() }

        /** Offered when the catalogue says a model reasons but lists no levels. */
        val FALLBACK: List<ThinkingLevel> = listOf(LOW, MEDIUM, HIGH)

        fun label(level: ThinkingLevel?): String = level?.label ?: AUTO_LABEL
    }
}

/** RelayRoom credential routing. Auto is the gateway default and is never sent. */
enum class RouteMode(val wire: String, val label: String) {
    AUTO("auto", "Auto"),
    SUBSCRIPTION("sub", "Subscription"),
    API_KEY("api", "API key");

    companion object {
        fun fromWire(value: String?): RouteMode =
            entries.firstOrNull { it.wire == value?.trim()?.lowercase() } ?: AUTO
    }
}
