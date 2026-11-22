package sh.taf.billi.data.repository

import sh.taf.billi.data.AppDatabase
import sh.taf.billi.data.Settings
import sh.taf.billi.data.sql.sqlTransaction
import kotlinx.coroutines.flow.Flow

/** The single settings row. Null until the database has seeded it. */
class SettingsRepository(private val db: AppDatabase) {

    private val settingsDao = db.settingsDao()

    val settings: Flow<Settings?> = settingsDao.getSettings()

    suspend fun getSettings(): Settings? = settingsDao.getSettingsDirect()

    suspend fun update(settings: Settings) = settingsDao.updateSettings(settings)

    /**
     * Reads the current row, applies [transform] and writes it back, in one
     * transaction, so two quick edits each land on the latest row instead of
     * overwriting each other. Returns false while there is no settings row yet.
     */
    suspend fun update(transform: (Settings) -> Settings): Boolean =
        db.sqlTransaction {
            val current = settingsDao.getSettingsDirect() ?: return@sqlTransaction false
            settingsDao.updateSettings(transform(current))
            true
        }
}
