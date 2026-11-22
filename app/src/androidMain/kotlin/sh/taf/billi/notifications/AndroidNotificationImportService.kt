package sh.taf.billi.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import com.pennywiseai.parser.notification.NotificationApp
import com.pennywiseai.parser.notification.NotificationAppKind
import com.pennywiseai.parser.notification.NotificationApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The notification switches, kept in SharedPreferences on this phone. Off by
 * default. Each app is on until the user turns it off, so the stored set lists
 * the apps switched off and a new app added to [NotificationApps] starts on.
 *
 * User-added apps are kept here too, in a small JSON list, and registered with
 * [NotificationApps] when the service starts so the listener and the parser
 * see them like built-ins.
 */
class AndroidNotificationImportService(context: Context) : NotificationImportService {

    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state: MutableStateFlow<NotificationImportState>

    init {
        restoreCustomApps()
        _state = MutableStateFlow(load())
    }

    override val state: StateFlow<NotificationImportState> = _state.asStateFlow()

    /** Read by the listener for every notification, so it goes to prefs, not the flow. */
    fun isReading(packageName: String): Boolean =
        prefs.getBoolean(KEY_ENABLED, false) &&
            NotificationApps.find(packageName) != null &&
            packageName !in disabled()

    override fun refreshAccess() {
        _state.value = load()
    }

    override fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = load()
        if (enabled) {
            // Ask the system to bind the listener now, rather than at the next boot.
            if (hasAccess()) {
                runCatching { NotificationListenerService.requestRebind(component()) }
            }
        } else {
            // Stop the callbacks too, not just the parsing. Best effort: the
            // system can bind again, and isReading still refuses every message.
            PaymentNotificationListener.stopReading()
        }
    }

    override fun setAppEnabled(packageName: String, enabled: Boolean) {
        val off = disabled().toMutableSet().apply { if (enabled) remove(packageName) else add(packageName) }
        prefs.edit().putStringSet(KEY_DISABLED, off).apply()
        _state.value = load()
    }

    override fun setAllAppsEnabled(enabled: Boolean) {
        val off = if (enabled) emptySet() else NotificationApps.all.map { it.packageName }.toSet()
        prefs.edit().putStringSet(KEY_DISABLED, off).apply()
        _state.value = load()
    }

    override fun openAccessSettings() {
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component().flattenToString())
        } else {
            null
        }
        val list = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        for (intent in listOfNotNull(detail, list)) {
            val opened = runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (opened) return
        }
    }

    override suspend fun installedApps(): List<InstalledNotificationApp> = withContext(Dispatchers.IO) {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        context.packageManager.queryIntentActivities(launcher, 0)
            .asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { info ->
                InstalledNotificationApp(
                    packageName = info.packageName,
                    name = info.loadLabel(context.packageManager).toString()
                )
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    override fun addApp(packageName: String, name: String, isBank: Boolean) {
        val added = NotificationApps.addCustom(
            NotificationApp(
                packageName = packageName,
                name = name,
                kind = if (isBank) NotificationAppKind.BANK else NotificationAppKind.PAYMENT
            )
        )
        if (!added) return
        val off = disabled().toMutableSet().apply { remove(packageName) }
        prefs.edit().putStringSet(KEY_DISABLED, off).apply()
        saveCustomApps()
        _state.value = load()
    }

    override fun linkedAccount(packageName: String, last4: String?): Int? =
        loadLinks().firstOrNull { it.packageName == packageName && it.last4 == last4 }?.accountId

    override fun setLink(packageName: String, last4: String?, accountId: Int?) {
        val digits = last4?.takeIf { it.isNotBlank() }
        val kept = loadLinks().filterNot { it.packageName == packageName && it.last4 == digits }
        val next = if (accountId == null) kept else kept + AccountLink(packageName, digits, accountId)
        val array = JSONArray()
        next.forEach { link ->
            array.put(
                JSONObject()
                    .put("package", link.packageName)
                    .put("last4", link.last4 ?: JSONObject.NULL)
                    .put("account", link.accountId)
            )
        }
        prefs.edit().putString(KEY_LINKS, array.toString()).apply()
        _state.value = load()
    }

    private fun loadLinks(): List<AccountLink> {
        val raw = prefs.getString(KEY_LINKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val packageName = item.optString("package").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val accountId = item.optInt("account", -1).takeIf { it > 0 } ?: return@mapNotNull null
                val last4 = if (item.isNull("last4")) null else item.optString("last4").takeIf { it.isNotBlank() }
                AccountLink(packageName, last4, accountId)
            }
        }.getOrDefault(emptyList())
    }

    override fun removeApp(packageName: String) {
        if (!NotificationApps.removeCustom(packageName)) return
        saveCustomApps()
        _state.value = load()
    }

    private fun hasAccess(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    private fun component() = ComponentName(context, PaymentNotificationListener::class.java)

    private fun disabled(): Set<String> = prefs.getStringSet(KEY_DISABLED, emptySet()).orEmpty()

    private fun installed(packageName: String): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun load(): NotificationImportState {
        val off = disabled()
        val apps = NotificationApps.all.map { app ->
            NotificationSourceApp(
                packageName = app.packageName,
                name = app.name,
                isBank = app.kind == NotificationAppKind.BANK,
                enabled = app.packageName !in off,
                installed = installed(app.packageName),
                custom = app.custom
            )
        }.sortedWith(compareByDescending<NotificationSourceApp> { it.installed }.thenBy { it.custom })
        return NotificationImportState(
            supported = true,
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            hasAccess = hasAccess(),
            apps = apps,
            links = loadLinks()
        )
    }

    private fun restoreCustomApps() {
        val raw = prefs.getString(KEY_CUSTOM, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val packageName = item.optString("package").takeIf { it.isNotBlank() } ?: continue
                NotificationApps.addCustom(
                    NotificationApp(
                        packageName = packageName,
                        name = item.optString("name").takeIf { it.isNotBlank() } ?: packageName,
                        kind = if (item.optBoolean("bank", true)) {
                            NotificationAppKind.BANK
                        } else {
                            NotificationAppKind.PAYMENT
                        }
                    )
                )
            }
        }
    }

    private fun saveCustomApps() {
        val array = JSONArray()
        NotificationApps.customApps.forEach { app ->
            array.put(
                JSONObject()
                    .put("package", app.packageName)
                    .put("name", app.name)
                    .put("bank", app.kind == NotificationAppKind.BANK)
            )
        }
        prefs.edit().putString(KEY_CUSTOM, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "notification_import"
        const val KEY_ENABLED = "enabled"
        const val KEY_DISABLED = "disabled_packages"
        const val KEY_CUSTOM = "custom_apps"
        const val KEY_LINKS = "account_links"
    }
}
