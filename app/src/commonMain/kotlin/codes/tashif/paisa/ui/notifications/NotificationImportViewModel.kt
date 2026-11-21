package codes.tashif.paisa.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.tashif.paisa.ai.local.LocalAiEngine
import codes.tashif.paisa.ai.local.LocalAiStatus
import codes.tashif.paisa.ai.store.AiConfig
import codes.tashif.paisa.ai.store.AiConfigStore
import codes.tashif.paisa.ai.store.MessageReaderMode
import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.core.SnackbarManager
import codes.tashif.paisa.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import codes.tashif.paisa.notifications.InstalledNotificationApp
import codes.tashif.paisa.notifications.NotificationImportService
import codes.tashif.paisa.notifications.NotificationImportState
import codes.tashif.paisa.ingest.PendingAiQueue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationImportViewModel(
    private val service: NotificationImportService,
    private val localAi: LocalAiEngine,
    private val aiConfig: AiConfigStore,
    private val pendingAi: PendingAiQueue,
    private val ladders: AiLadderRunner,
    private val settings: SettingsRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    val state: StateFlow<NotificationImportState> = service.state
    val localAiStatus: StateFlow<LocalAiStatus> = localAi.status
    val aiConfigState: StateFlow<AiConfig> = aiConfig.config

    private val _cloudReady = MutableStateFlow(false)

    /** "Use AI" is on and the SMS and alerts ladder has a cloud model with a key. */
    val cloudReady: StateFlow<Boolean> = _cloudReady.asStateFlow()

    init {
        viewModelScope.launch { localAi.refresh() }
        refreshCloud()
    }

    /** Access and keys both change in other screens, so check again on the way back. */
    fun refreshAccess() {
        service.refreshAccess()
        refreshCloud()
    }

    private fun refreshCloud() {
        viewModelScope.launch {
            _cloudReady.value = settings.getSettings()?.aiEnabled != false &&
                ladders.canRun(AiService.MESSAGE_PARSE, cloudOnly = true)
        }
    }

    fun setEnabled(enabled: Boolean) = service.setEnabled(enabled)

    fun setAppEnabled(packageName: String, enabled: Boolean) = service.setAppEnabled(packageName, enabled)

    fun setAllAppsEnabled(enabled: Boolean) = service.setAllAppsEnabled(enabled)

    fun openAccessSettings() = service.openAccessSettings()

    suspend fun installedApps(): List<InstalledNotificationApp> = service.installedApps()

    fun addApp(packageName: String, name: String, isBank: Boolean) =
        service.addApp(packageName, name, isBank)

    fun removeApp(packageName: String) = service.removeApp(packageName)

    /** Sets which account [packageName]'s payments (from [last4], or by default) go to. Null clears it. */
    fun setLink(packageName: String, last4: String?, accountId: Int?) = service.setLink(packageName, last4, accountId)

    fun setMessageReader(mode: MessageReaderMode) {
        if (mode == MessageReaderMode.ON_DEVICE && !localAi.status.value.supported) {
            snackbar.show(localAi.status.value.detail ?: "Gemini Nano isn't available on this phone")
            return
        }
        viewModelScope.launch {
            aiConfig.update { it.copy(messageReader = mode) }
            // A new reader is the moment to retry everything kept for one.
            if (mode != MessageReaderMode.OFF) pendingAi.process()
        }
    }
}
