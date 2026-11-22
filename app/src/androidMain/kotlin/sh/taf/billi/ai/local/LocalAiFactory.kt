package sh.taf.billi.ai.local

import android.content.Context
import android.os.Build
import sh.taf.billi.BuildConfig

/**
 * Picks the on-device engine. The AICore class is only touched on Android 8+,
 * so the app can keep its lower minSdk for everything else.
 */
internal fun provideLocalAiEngine(context: Context): LocalAiEngine = when {
    DebugLocalAi.enabled(context) -> DemoLocalAiEngine(inForeground = VisibleActivities.watch(context))
    Build.VERSION.SDK_INT < 26 -> UnsupportedLocalAiEngine(
        "Gemini Nano needs Android 8.0 or newer."
    )
    // Gemini Nano runs inside Google's AICore app. Without it there is nothing to
    // ask, so skip ML Kit entirely (Galaxy S23, older Pixels, most mid-range phones).
    !hasAiCore(context) -> UnsupportedLocalAiEngine(
        "This phone has no AICore, so Gemini Nano can't run here."
    )
    else -> MlKitLocalAiEngine()
}

private const val AICORE_PACKAGE = "com.google.android.aicore"

/** AICore is installed and enabled. Needs the <queries> entry in the manifest on Android 11+. */
private fun hasAiCore(context: Context): Boolean = try {
    context.packageManager.getApplicationInfo(AICORE_PACKAGE, 0).enabled
} catch (_: android.content.pm.PackageManager.NameNotFoundException) {
    false
}

/**
 * Counts Billi's started activities, so the demo engine can refuse to run in
 * the background like AICore does. Process importance can't tell: a bound
 * notification listener keeps the process at foreground importance.
 */
internal object VisibleActivities : android.app.Application.ActivityLifecycleCallbacks {
    private val started = java.util.concurrent.atomic.AtomicInteger()
    private var registered = false

    @Synchronized
    fun watch(context: Context): () -> Boolean {
        val app = context.applicationContext as? android.app.Application ?: return { true }
        if (!registered) {
            app.registerActivityLifecycleCallbacks(this)
            registered = true
        }
        return { started.get() > 0 }
    }

    override fun onActivityStarted(activity: android.app.Activity) { started.incrementAndGet() }
    override fun onActivityStopped(activity: android.app.Activity) { started.updateAndGet { maxOf(0, it - 1) } }
    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) = Unit
    override fun onActivityResumed(activity: android.app.Activity) = Unit
    override fun onActivityPaused(activity: android.app.Activity) = Unit
    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) = Unit
    override fun onActivityDestroyed(activity: android.app.Activity) = Unit
}

/**
 * Debug builds only: `adb shell am start -n sh.taf.billi/.MainActivity
 * --ez local_ai_demo true` swaps in a deterministic model so the whole local
 * path can be tested on an emulator without AICore. Release builds never see
 * this branch because [BuildConfig.DEBUG] is false.
 */
internal object DebugLocalAi {
    const val EXTRA = "local_ai_demo"
    private const val PREFS = "paisa_debug"
    private const val KEY = "local_ai_demo"

    fun handle(context: Context, intent: android.content.Intent?) {
        if (!BuildConfig.DEBUG || intent == null || !intent.hasExtra(EXTRA)) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY, intent.getBooleanExtra(EXTRA, false))
            .apply()
    }

    /** Starts counting visible activities before the first one starts. Debug builds only. */
    fun install(context: Context) {
        if (BuildConfig.DEBUG) VisibleActivities.watch(context)
    }

    fun enabled(context: Context): Boolean =
        BuildConfig.DEBUG &&
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
}
