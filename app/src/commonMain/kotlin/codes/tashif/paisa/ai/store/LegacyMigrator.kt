package codes.tashif.paisa.ai.store

import codes.tashif.paisa.ai.AiCredentialsStore

/**
 * Runs [LegacyMigration] once. The old credentials are cleared only after the
 * key and the config have both reached disk. If either write fails they stay,
 * and the next launch tries again.
 */
class LegacyMigrator(
    private val keys: AiKeyStore,
    private val configStore: AiConfigStore,
    private val legacyStore: AiCredentialsStore,
) {
    /** Returns true when migration is done, false when it should run again next launch. */
    suspend fun run(): Boolean {
        if (configStore.config.value.legacyMigrated) {
            if (legacyStore.hasLegacy()) legacyStore.clearLegacy()
            return true
        }
        val migration = LegacyMigration.migrate(legacyStore.load(), configStore.config.value)
        if (migration != null && !keys.save(migration.providerId, migration.apiKey)) return false
        val saved = configStore.tryUpdate { (migration?.config ?: it).copy(legacyMigrated = true) }
        if (!saved) return false
        legacyStore.clearLegacy()
        return true
    }
}
