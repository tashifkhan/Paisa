package codes.tashif.paisa.ui.statement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.ai.AiCredentials
import codes.tashif.paisa.ai.AiProvider
import codes.tashif.paisa.ai.AiTestResult
import codes.tashif.paisa.ai.LlmClient
import codes.tashif.paisa.core.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The BYOK provider, key, base URL and model, plus the connection test. */
@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val credentialsHolder: AiCredentialsHolder,
    private val llmClient: LlmClient,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    val aiCredentials: StateFlow<AiCredentials> = credentialsHolder.credentials

    private val _aiTestResult = MutableStateFlow<AiTestResult?>(null)
    val aiTestResult: StateFlow<AiTestResult?> = _aiTestResult.asStateFlow()

    fun saveAiCredentials(credentials: AiCredentials) {
        credentialsHolder.save(credentials)
        snackbarManager.show("AI settings saved")
    }

    fun clearAiCredentials() {
        credentialsHolder.clear()
        snackbarManager.show("AI key cleared")
    }

    fun testAiConnection(credentials: AiCredentials) {
        viewModelScope.launch {
            _aiTestResult.value = AiTestResult("Testing…", isError = false)
            val result = llmClient.testConnection(credentials)
            _aiTestResult.value = result.fold(
                onSuccess = {
                    AiTestResult("Connected · reply: ${it.take(80)}", isError = false)
                },
                onFailure = {
                    AiTestResult(
                        "Failed: ${it.message ?: "unknown error"}",
                        isError = true
                    )
                }
            )
        }
    }

    fun clearAiTestResult() {
        _aiTestResult.value = null
    }

    fun defaultModelFor(provider: AiProvider): String = provider.defaultModel

    fun defaultBaseUrlFor(provider: AiProvider): String = provider.defaultBaseUrl
}
