package codes.tashif.paisa.data.repository

import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.Settings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** The single settings row. Null until the database has seeded it. */
@Singleton
class SettingsRepository @Inject constructor(db: AppDatabase) {

    private val settingsDao = db.settingsDao()

    val settings: Flow<Settings?> = settingsDao.getSettings()

    suspend fun getSettings(): Settings? = settingsDao.getSettingsDirect()

    suspend fun update(settings: Settings) = settingsDao.updateSettings(settings)

    /**
     * Reads the current row, applies [transform] and writes it back.
     * Does nothing and returns false while there is no settings row yet.
     */
    suspend fun update(transform: (Settings) -> Settings): Boolean {
        val current = getSettings() ?: return false
        update(transform(current))
        return true
    }
}
