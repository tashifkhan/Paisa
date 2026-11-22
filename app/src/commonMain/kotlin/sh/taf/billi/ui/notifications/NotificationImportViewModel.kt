package sh.taf.billi.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sh.taf.billi.ai.local.LocalAiEngine
import sh.taf.billi.ai.local.LocalAiStatus
import sh.taf.billi.ai.store.AiConfig
import sh.taf.billi.ai.store.AiConfigStore
import sh.taf.billi.ai.store.MessageReaderMode
import sh.taf.billi.ai.ladder.AiLadderRunner
import sh.taf.billi.ai.ladder.AiService
import sh.taf.billi.core.SnackbarManager
import sh.taf.billi.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import sh.taf.billi.notifications.InstalledNotificationApp
import sh.taf.billi.notifications.NotificationImportService
import sh.taf.billi.notifications.NotificationImportState
import sh.taf.billi.ingest.PendingAiQueue
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
