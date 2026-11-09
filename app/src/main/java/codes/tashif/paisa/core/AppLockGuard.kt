package codes.tashif.paisa.core

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers when we deliberately hand off to another app (file picker, export
 * sheet, browser sign-in). That stops our activity, which would otherwise trip
 * the app lock and dump the user back on the unlock screen mid-flow.
 *
 * Screens call [onExternalActivityLaunched] through their ViewModel right
 * before launching. The app-lock owner checks [isExternalActivityPending]
 * before locking in onStop, and calls [onReturnToForeground] in onStart.
 */
@Singleton
class AppLockGuard @Inject constructor() {

    @Volatile
    private var externalActivityStartedAt: Long? = null

    /** Call right before launching a picker or sheet so the resulting onStop doesn't lock. */
    fun onExternalActivityLaunched() {
        externalActivityStartedAt = System.currentTimeMillis()
    }

    /** True while a detour started by [onExternalActivityLaunched] hasn't come back yet. */
    val isExternalActivityPending: Boolean
        get() = externalActivityStartedAt != null

    /**
     * Ends the detour on return to the foreground. Returns true when it outlived
     * the grace period, so the caller should apply the lock it deferred. The user
     * may have wandered off from the picker rather than come back.
     */
    fun onReturnToForeground(): Boolean {
        val startedAt = externalActivityStartedAt ?: return false
        externalActivityStartedAt = null
        return System.currentTimeMillis() - startedAt > EXTERNAL_ACTIVITY_GRACE_MS
    }

    private companion object {
        /** How long a picker or export detour may last before we lock on return anyway. */
        const val EXTERNAL_ACTIVITY_GRACE_MS = 2 * 60 * 1000L
    }
}
