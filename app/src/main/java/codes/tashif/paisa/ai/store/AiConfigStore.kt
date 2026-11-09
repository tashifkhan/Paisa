package codes.tashif.paisa.ai.store

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AiConfig] as one JSON string in plain SharedPreferences. It holds no keys.
 * Backup rules exclude the file, and sync never reads it.
 */
@Singleton
class AiConfigStore internal constructor(private val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) :
        this(context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    private val mutex = Mutex()

    private val _config = MutableStateFlow(AiConfigCodec.decode(prefs.getString(KEY, null)))
    val config: StateFlow<AiConfig> = _config.asStateFlow()

    /**
     * Applies [change] to the latest config, inside the lock, and saves it.
     * Every edit goes through here so none works from stale state. Returns
     * the new config.
     */
    suspend fun update(change: (AiConfig) -> AiConfig): AiConfig = mutex.withLock {
        val next = change(_config.value)
        if (next != _config.value) {
            save(next)
            _config.value = next
        }
        next
    }

    /**
     * Like [update], but only takes the new config when the write reached
     * disk. Returns false, and keeps the old config, when it didn't.
     */
    suspend fun tryUpdate(change: (AiConfig) -> AiConfig): Boolean = mutex.withLock {
        val next = change(_config.value)
        if (next == _config.value) return@withLock true
        if (!save(next)) return@withLock false
        _config.value = next
        true
    }

    private suspend fun save(config: AiConfig): Boolean =
        withContext(Dispatchers.IO) { prefs.edit().putString(KEY, AiConfigCodec.encode(config)).commit() }

    companion object {
        const val PREFS_NAME = "paisa_ai_config"
        private const val KEY = "config"
    }
}
