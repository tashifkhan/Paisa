package sh.taf.billi.ai.local

import sh.taf.billi.ai.LlmException
import sh.taf.billi.ai.LlmFailure
import sh.taf.billi.ai.catalog.ModelInfo
import sh.taf.billi.ai.catalog.ModelSource
import sh.taf.billi.ai.ladder.Rung
import sh.taf.billi.ai.provider.BuiltInProviders
import sh.taf.billi.ai.provider.LlmRequest
import sh.taf.billi.ai.store.AiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The stable id of the single model AICore exposes to apps. */
const val LOCAL_AI_MODEL_ID = "gemini-nano"

/** What the on-device model can do, as far as the platform tells us. */
enum class LocalAiFeature(val label: String) {
    TEXT("Text"),
    IMAGE("Images"),
    JSON("JSON"),
    SYSTEM_PROMPT("System prompts"),
    THINKING("Thinking"),
    FUNCTION_CALLING("Tools"),
}

enum class LocalAiState {
    /** The first check hasn't finished yet. */
    CHECKING,
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNSUPPORTED,
    ERROR,
}

/**
 * The on-device model as the app sees it. [detail] is ready to show and
 * explains an unsupported or failed state.
 */
data class LocalAiStatus(
    val state: LocalAiState = LocalAiState.CHECKING,
    /** The platform's name for the active model, such as `nano-v4`. */
    val modelName: String? = null,
    val detail: String? = null,
    val bytesDownloaded: Long = 0,
    /** Null until the platform reports how large the download is. */
    val bytesToDownload: Long? = null,
    val features: Set<LocalAiFeature> = emptySet(),
    val maxInputTokens: Int? = null,
    val maxOutputTokens: Int? = null,
    /** Epoch millis of the last status check. */
    val checkedAt: Long? = null,
) {
    val ready: Boolean get() = state == LocalAiState.AVAILABLE

    /** False once the phone is known to have no on-device model. Local AI UI hides then. */
    val supported: Boolean get() = state != LocalAiState.UNSUPPORTED
    val downloadable: Boolean get() = state == LocalAiState.DOWNLOADABLE

    /** 0..1 while downloading, else null. */
    val progress: Float?
        get() {
            val total = bytesToDownload ?: return null
            if (total <= 0) return null
            return (bytesDownloaded.toDouble() / total).toFloat().coerceIn(0f, 1f)
        }

    /** Why a ladder step on this model is skipped, or null when it can run. */
    val skipReason: String?
        get() = when (state) {
            LocalAiState.AVAILABLE -> null
            LocalAiState.CHECKING -> "on-device AI is still being checked"
            LocalAiState.DOWNLOADABLE -> "Gemini Nano isn't downloaded"
            LocalAiState.DOWNLOADING -> "Gemini Nano is still downloading"
            LocalAiState.UNSUPPORTED -> detail ?: "on-device AI isn't supported on this device"
            LocalAiState.ERROR -> detail ?: "on-device AI couldn't start"
        }

    companion object {
        fun checking() = LocalAiStatus(LocalAiState.CHECKING, detail = "Checking this device…")

        fun unsupported(reason: String) = LocalAiStatus(LocalAiState.UNSUPPORTED, detail = reason)

        fun failed(reason: String) = LocalAiStatus(LocalAiState.ERROR, detail = reason)
    }
}

/**
 * One on-device model implementation. Android binds this to AICore through
 * ML Kit; the web build has no engine yet. Nothing here touches the network.
 */
interface LocalAiEngine {
    val status: StateFlow<LocalAiStatus>

    /** Re-reads availability, model version and capabilities. */
    suspend fun refresh(): LocalAiStatus

    /**
     * Asks the platform to download the model. Progress lands in [status].
     * Returns the state after the download ends or fails.
     */
    suspend fun download(): LocalAiStatus

    /** Runs one prompt. Throws [LlmException] when the model can't answer. */
    suspend fun complete(request: LlmRequest): String
}

/** Used on the web and on Android versions older than the AICore API. */
class UnsupportedLocalAiEngine(private val reason: String) : LocalAiEngine {
    private val _status = MutableStateFlow(LocalAiStatus.unsupported(reason))
    override val status: StateFlow<LocalAiStatus> = _status.asStateFlow()

    override suspend fun refresh(): LocalAiStatus = _status.value

    override suspend fun download(): LocalAiStatus = _status.value

    override suspend fun complete(request: LlmRequest): String =
        throw LlmException(reason, LlmFailure.CONFIG)
}

/** The catalogue entry used by the ladder picker and by saved local rungs. */
object LocalAiModels {
    fun info(status: LocalAiStatus): ModelInfo = ModelInfo(
        id = LOCAL_AI_MODEL_ID,
        name = status.modelName?.takeIf { it.isNotBlank() }?.let { "Gemini Nano · $it" } ?: "Gemini Nano",
        // The Prompt API reads text and images. It does not carry PDFs or audio.
        input = buildSet {
            add("text")
            if (LocalAiFeature.IMAGE in status.features || status.features.isEmpty()) add("image")
        },
        reasoning = false,
        contextWindow = status.maxInputTokens ?: 4_000,
        outputLimit = status.maxOutputTokens ?: 4_096,
        source = ModelSource.BUILT_IN,
    )
}

/** Adding and removing the local rung. Pure, so it is easy to test. */
object LocalAiLadders {
    val rung = Rung(BuiltInProviders.LOCAL, LOCAL_AI_MODEL_ID)

    /** True when the Default ladder or any service's own ladder uses the local model. */
    fun uses(config: AiConfig): Boolean =
        config.defaultLadder.any { it.providerId == BuiltInProviders.LOCAL } ||
            config.serviceLadders.values.any { rungs -> rungs.any { it.providerId == BuiltInProviders.LOCAL } }

    /** Puts the local model first on the Default ladder, once. */
    fun enable(config: AiConfig): AiConfig = config.copy(
        defaultLadder = listOf(rung) + config.defaultLadder.filterNot { it.providerId == BuiltInProviders.LOCAL }
    )

    /** Removes every local rung. A service left empty falls back to the Default ladder. */
    fun disable(config: AiConfig): AiConfig = config.copy(
        defaultLadder = config.defaultLadder.filterNot { it.providerId == BuiltInProviders.LOCAL },
        serviceLadders = config.serviceLadders
            .mapValues { (_, rungs) -> rungs.filterNot { it.providerId == BuiltInProviders.LOCAL } }
            .filterValues { it.isNotEmpty() },
    )
}

/** Short, user-facing compatibility copy. The long list lives in docs/local-ai.md. */
object LocalAiCompatibility {
    const val SUMMARY =
        "Pixel 9 and newer, Galaxy S25 and newer, and recent OnePlus, Xiaomi, OPPO and vivo flagships."

    const val EMULATOR =
        "The emulator has no Gemini Nano."

    const val APPLE =
        "Apple Intelligence uses a different on-device model and API, and Billi has no iOS build yet."
}
