package codes.tashif.paisa.ai.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import codes.tashif.paisa.ai.LlmImage
import codes.tashif.paisa.ai.provider.LlmRequest
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Candidate
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

/**
 * Gemini Nano through Android's AICore, using ML Kit's Prompt API. The model
 * is shared by every app on the phone: AICore downloads it, and nothing here
 * is sent over the network.
 *
 * The API only exists on Android 8.0 and newer. The factory checks the SDK
 * before this class is loaded, so older phones never touch these classes.
 */
internal class MlKitLocalAiEngine : LocalAiEngine {

    private val _status = MutableStateFlow(LocalAiStatus.checking())
    override val status: StateFlow<LocalAiStatus> = _status.asStateFlow()

    private val refreshMutex = Mutex()
    private val downloadMutex = Mutex()

    /**
     * AICore runs one inference per app at a time and answers BUSY to the rest.
     * Statement import sends chunks in parallel, so calls take turns here.
     */
    private val inferenceMutex = Mutex()

    private val model: GenerativeModel by lazy { Generation.getClient() }

    override suspend fun refresh(): LocalAiStatus = withContext(Dispatchers.Default) {
        refreshMutex.withLock { readStatus() }
    }

    private suspend fun readStatus(): LocalAiStatus {
        val next = try {
            when (val raw = model.checkStatus()) {
                FeatureStatus.AVAILABLE -> availableStatus()
                FeatureStatus.DOWNLOADABLE -> LocalAiStatus(
                    state = LocalAiState.DOWNLOADABLE,
                    detail = "Gemini Nano can be downloaded on this phone.",
                    checkedAt = now(),
                )
                FeatureStatus.DOWNLOADING -> LocalAiStatus(
                    state = LocalAiState.DOWNLOADING,
                    detail = "Gemini Nano is downloading.",
                    checkedAt = now(),
                )
                else -> unavailableStatus()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failedStatus(e)
        }
        _status.value = next
        return next
    }

    private suspend fun availableStatus(): LocalAiStatus {
        val name = runCatching { model.getBaseModelName() }.getOrNull()
        val maxInput = runCatching { model.getTokenLimit() }.getOrNull()
        val systemPrompt = runCatching { model.isSystemPromptAvailable() }.getOrDefault(true)
        val structured = runCatching { model.isStructuredOutputFeatureAvailable() }.getOrDefault(false)
        val thinking = runCatching { model.isThinkingModeAvailable() }.getOrDefault(false)
        val features = buildSet {
            add(LocalAiFeature.TEXT)
            // The Prompt API takes images on every supported Gemini Nano version.
            add(LocalAiFeature.IMAGE)
            if (systemPrompt) add(LocalAiFeature.SYSTEM_PROMPT)
            if (structured) add(LocalAiFeature.JSON)
            if (thinking) add(LocalAiFeature.THINKING)
        }
        return LocalAiStatus(
            state = LocalAiState.AVAILABLE,
            modelName = name,
            detail = "Gemini Nano is ready on this phone.",
            features = features,
            maxInputTokens = maxInput,
            maxOutputTokens = OUTPUT_TOKEN_LIMIT,
            checkedAt = now(),
        )
    }

    private fun unavailableStatus(): LocalAiStatus =
        LocalAiStatus.unsupported(
            if (isEmulator()) {
                LocalAiCompatibility.EMULATOR
            } else {
                "Gemini Nano isn't available on this phone yet. On a supported phone, update AICore and try again."
            }
        )

    private fun failedStatus(error: Throwable): LocalAiStatus {
        // "Not available" and "not supported" are facts about the phone, not failures to retry.
        val code = (error as? GenAiException)?.errorCode
        if (code == GenAiException.ErrorCode.NOT_AVAILABLE || code == GenAiException.ErrorCode.NOT_SUPPORTED) {
            return unavailableStatus()
        }
        val message = (error as? GenAiException)?.let(::genAiMessage) ?: error.message
        return LocalAiStatus.failed(message?.take(240) ?: "Couldn't reach AICore on this phone.")
    }

    override suspend fun download(): LocalAiStatus = withContext(Dispatchers.Default) {
        downloadMutex.withLock {
            val current = refresh()
            if (!current.downloadable && current.state != LocalAiState.DOWNLOADING) return@withLock current
            _status.value = current.copy(
                state = LocalAiState.DOWNLOADING,
                detail = "Starting the Gemini Nano download…",
            )
            try {
                var failed = false
                model.download().collect { event ->
                    when (event) {
                        is DownloadStatus.DownloadStarted -> _status.update {
                            it.copy(
                                state = LocalAiState.DOWNLOADING,
                                bytesDownloaded = 0,
                                bytesToDownload = event.bytesToDownload,
                                detail = "Downloading Gemini Nano…",
                            )
                        }
                        is DownloadStatus.DownloadProgress -> _status.update {
                            it.copy(
                                state = LocalAiState.DOWNLOADING,
                                bytesDownloaded = event.totalBytesDownloaded,
                                detail = "Downloading Gemini Nano…",
                            )
                        }
                        DownloadStatus.DownloadCompleted -> _status.update {
                            it.copy(
                                state = LocalAiState.DOWNLOADING,
                                bytesDownloaded = it.bytesToDownload ?: it.bytesDownloaded,
                                detail = "Finishing the Gemini Nano install…",
                            )
                        }
                        is DownloadStatus.DownloadFailed -> {
                            failed = true
                            _status.update {
                                it.copy(
                                    state = LocalAiState.ERROR,
                                    detail = genAiMessage(event.e),
                                    checkedAt = now(),
                                )
                            }
                        }
                    }
                }
                // AICore may need a moment to report the new model as available.
                if (failed) _status.value else refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val failed = failedStatus(e)
                _status.value = failed
                failed
            }
        }
    }

    override suspend fun complete(request: LlmRequest): String {
        val current = if (_status.value.ready) _status.value else refresh()
        if (!current.ready) {
            throw LlmException(
                current.skipReason ?: "Gemini Nano isn't ready on this phone.",
                LlmFailure.CONFIG,
            )
        }
        if (request.pdf != null) throw LlmException("Gemini Nano can't read PDFs on this phone.", LlmFailure.CONFIG)
        if (request.audio != null) throw LlmException("Gemini Nano can't listen to voice notes here.", LlmFailure.CONFIG)

        val system = request.system.takeIf { it.isNotBlank() }
        val useSystem = system != null && LocalAiFeature.SYSTEM_PROMPT in current.features
        val prompt = if (system != null && !useSystem) "${system}\n\n${request.prompt}" else request.prompt
        val image = request.image?.let(::decodeImage)

        val content = content {
            if (image != null) image(image)
            text(prompt)
        }
        val builder = GenerateContentRequest.Builder(listOf(content))
        if (system != null && useSystem) builder.systemInstruction = SystemInstruction(system)
        builder.temperature = 0.1f
        builder.candidateCount = 1
        builder.maxOutputTokens = current.maxOutputTokens ?: OUTPUT_TOKEN_LIMIT

        val response = try {
            inferenceMutex.withLock { generateWithBusyRetry(builder.build()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val message = (e as? GenAiException)?.let(::genAiMessage)
                ?: e.message?.take(240)
                ?: "The on-device model failed."
            throw LlmException(message, LlmFailure.CONFIG, cause = e)
        }
        val candidate = response.candidates.firstOrNull()
            ?: throw LlmException("The on-device model returned nothing.", LlmFailure.EMPTY)
        if (candidate.finishReason == Candidate.FinishReason.MAX_TOKENS) {
            throw LlmException(
                "Gemini Nano's reply was cut off. Try a smaller file or split the statement.",
                LlmFailure.CUT_OFF,
            )
        }
        val text = candidate.text.trim()
        if (text.isEmpty()) throw LlmException("The on-device model returned nothing.", LlmFailure.EMPTY)
        return text
    }

    /** AICore returns BUSY when another app is using the model. One quiet retry is usually enough. */
    private suspend fun generateWithBusyRetry(request: GenerateContentRequest): GenerateContentResponse {
        var attempt = 0
        while (true) {
            try {
                return model.generateContent(request)
            } catch (e: GenAiException) {
                if (e.errorCode == GenAiException.ErrorCode.BUSY && attempt < 1) {
                    attempt++
                    delay(1_500)
                    continue
                }
                throw e
            }
        }
    }

    private fun decodeImage(image: LlmImage): Bitmap {
        val bytes = try {
            Base64.decode(image.base64Data)
        } catch (_: IllegalArgumentException) {
            throw LlmException("Couldn't read that image for the on-device model.", LlmFailure.CONFIG)
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw LlmException("Couldn't read that image for the on-device model.", LlmFailure.CONFIG)
        return scaleDown(bitmap, MAX_IMAGE_SIDE)
    }

    /** Big photos cost tokens and memory. 1568 px keeps receipts readable. */
    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val side = maxOf(bitmap.width, bitmap.height)
        if (side <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / side
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun genAiMessage(error: GenAiException): String = when (error.errorCode) {
        GenAiException.ErrorCode.BACKGROUND_USE_BLOCKED ->
            "Gemini Nano only runs while Paisa is open. Keep the app in front and try again."
        GenAiException.ErrorCode.REQUEST_TOO_LARGE ->
            "That's too long for Gemini Nano (about 4,000 tokens). Split it and try again."
        GenAiException.ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED ->
            "Paisa reached the phone's on-device AI limit for now. Try again later."
        GenAiException.ErrorCode.BUSY ->
            "Gemini Nano is busy. Wait a moment and try again."
        GenAiException.ErrorCode.NOT_ENOUGH_DISK_SPACE ->
            "There isn't enough free space for Gemini Nano."
        GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE ->
            "Android needs an update before Gemini Nano can run."
        GenAiException.ErrorCode.AICORE_INCOMPATIBLE ->
            "AICore on this phone is too old for Paisa. Update it from the Play Store."
        GenAiException.ErrorCode.NOT_AVAILABLE, GenAiException.ErrorCode.NOT_SUPPORTED ->
            if (isEmulator()) LocalAiCompatibility.EMULATOR else LocalAiCompatibility.SUMMARY
        else -> error.message?.take(240) ?: "AICore returned an error (${error.errorCode})."
    }

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("sdk_gphone", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true)

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        /** Gemini Nano's documented output ceiling is about 4K tokens. */
        const val OUTPUT_TOKEN_LIMIT = 4_096
        const val MAX_IMAGE_SIDE = 1_568
    }
}
