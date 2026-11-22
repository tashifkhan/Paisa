package sh.taf.billi.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Browsers can't read other apps' notifications. Reports unsupported and ignores every switch. */
class WebNotificationImportService : NotificationImportService {
    override val state: StateFlow<NotificationImportState> =
        MutableStateFlow(NotificationImportState(supported = false)).asStateFlow()

    override fun refreshAccess() = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun setAppEnabled(packageName: String, enabled: Boolean) = Unit
    override fun setAllAppsEnabled(enabled: Boolean) = Unit
    override fun openAccessSettings() = Unit
    override suspend fun installedApps(): List<InstalledNotificationApp> = emptyList()
    override fun addApp(packageName: String, name: String, isBank: Boolean) = Unit
    override fun removeApp(packageName: String) = Unit
    override fun linkedAccount(packageName: String, last4: String?): Int? = null
    override fun setLink(packageName: String, last4: String?, accountId: Int?) = Unit
}
