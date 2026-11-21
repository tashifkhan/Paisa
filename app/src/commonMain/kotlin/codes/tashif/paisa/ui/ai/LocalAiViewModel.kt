package codes.tashif.paisa.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.ai.AiRepository
import codes.tashif.paisa.ai.AiTestResult
import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.local.LOCAL_AI_MODEL_ID
import codes.tashif.paisa.ai.local.LocalAiState
import codes.tashif.paisa.ai.local.LocalAiStatus
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.core.SnackbarManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The Local AI screen: availability, download progress and a live test. */
class LocalAiViewModel(
    private val repository: AiRepository,
    private val runner: AiLadderRunner,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {

    val status: StateFlow<LocalAiStatus> = repository.localStatus

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _test = MutableStateFlow<AiTestResult?>(null)
    val test: StateFlow<AiTestResult?> = _test.asStateFlow()

    init {
        viewModelScope.launch { repository.refreshLocalAi() }
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshLocalAi() }
    }

    fun download() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _test.value = null
            val result = repository.downloadLocalAi()
            _busy.value = false
            when (result.state) {
                LocalAiState.AVAILABLE -> snackbarManager.show("Gemini Nano is ready on this phone")
                LocalAiState.ERROR -> snackbarManager.show(result.detail ?: "The download didn't finish")
                LocalAiState.UNSUPPORTED -> snackbarManager.show(result.detail ?: "Local AI isn't supported here")
                else -> Unit
            }
        }
    }

    fun test() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _test.value = AiTestResult("Talking to the on-device model…", isError = false)
            val result = runner.testProvider(BuiltInProviders.LOCAL, LOCAL_AI_MODEL_ID)
            _test.value = result.fold(
                onSuccess = { AiTestResult("On-device · $it", isError = false) },
                onFailure = { e ->
                    if (e is CancellationException) throw e
                    AiTestResult(e.message ?: "The on-device model didn't answer", isError = true)
                },
            )
            _busy.value = false
        }
    }
}
