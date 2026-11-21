package codes.tashif.paisa.notifications

import kotlinx.coroutines.flow.StateFlow

/** One app whose payment notifications Paisa can read. */
data class NotificationSourceApp(
    val packageName: String,
    val name: String,
    /** A bank or card app, read with that bank's SMS parser. False for UPI apps. */
    val isBank: Boolean,
    val enabled: Boolean,
    val installed: Boolean,
    /** Added by the user from this phone's app list. */
    val custom: Boolean = false
)

/** An installed app offered in the "Add app" picker. */
data class InstalledNotificationApp(
    val packageName: String,
    val name: String
)

data class NotificationImportState(
    /** False where the platform has no notifications to read, such as the browser. */
    val supported: Boolean = false,
    /** The master switch in settings. */
    val enabled: Boolean = false,
    /** Whether the system granted Paisa notification access. */
    val hasAccess: Boolean = false,
    val apps: List<NotificationSourceApp> = emptyList(),
    /** Which account each app's payments go to. */
    val links: List<AccountLink> = emptyList()
)

/**
 * Reading transactions from bank and UPI app notifications. On Android a
 * NotificationListenerService does the reading; this holds its switches.
 */
interface NotificationImportService {
    val state: StateFlow<NotificationImportState>

    /** Rechecks notification access, which the user changes in system settings. */
    fun refreshAccess()

    fun setEnabled(enabled: Boolean)

    fun setAppEnabled(packageName: String, enabled: Boolean)

    /** Turns every app on or off at once. */
    fun setAllAppsEnabled(enabled: Boolean)

    /** Opens the system screen where notification access is granted. */
    fun openAccessSettings()

    /** Installed launchable apps, for the user to add one that isn't built in. */
    suspend fun installedApps(): List<InstalledNotificationApp>

    /** Adds a user-picked app. [isBank] picks the bank parser first, else the generic reader. */
    fun addApp(packageName: String, name: String, isBank: Boolean)

    fun removeApp(packageName: String)

    /** The account linked for exactly [packageName] and [last4] (null: the app's default), if any. */
    fun linkedAccount(packageName: String, last4: String?): Int?

    /** Links [packageName] (and [last4], if given) to [accountId]. Null removes the link. */
    fun setLink(packageName: String, last4: String?, accountId: Int?)
}
