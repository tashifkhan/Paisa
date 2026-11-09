package codes.tashif.paisa.ai.store

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import codes.tashif.paisa.ai.AiCredentials
import codes.tashif.paisa.ai.AiCredentialsStore
import codes.tashif.paisa.ai.AiProvider
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.LadderConfig
import codes.tashif.paisa.ai.ladder.Rung
import codes.tashif.paisa.ai.provider.BuiltInProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Migration commits and ladder edits, on real (Robolectric) SharedPreferences. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiStoresTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun prefs(name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE).also { it.edit().clear().commit() }

    /** Prefs whose writes never reach disk, once [failing] is on. */
    private class FlakyPrefs(private val real: SharedPreferences) : SharedPreferences by real {
        var failing = false
        override fun edit(): SharedPreferences.Editor = if (!failing) real.edit() else DroppedEditor()
    }

    /** Takes every change and saves none of them. */
    private class DroppedEditor : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?) = this
        override fun putStringSet(key: String?, values: MutableSet<String>?) = this
        override fun putInt(key: String?, value: Int) = this
        override fun putLong(key: String?, value: Long) = this
        override fun putFloat(key: String?, value: Float) = this
        override fun putBoolean(key: String?, value: Boolean) = this
        override fun remove(key: String?) = this
        override fun clear() = this
        override fun commit(): Boolean = false
        override fun apply() = Unit
    }

    private val legacy = AiCredentials(AiProvider.GEMINI, "sk-old", BuiltInProviders.GEMINI_URL, "gemini-2.0-flash")

    @Test
    fun aFailedKeyWriteKeepsTheOldSetupForNextLaunch() = runBlocking {
        val keyPrefs = FlakyPrefs(prefs("migr-keys-1"))
        val credentials = AiCredentialsStore(SharedPrefsAiStore(keyPrefs))
        assertTrue(credentials.saveLegacy(legacy))
        keyPrefs.failing = true
        val config = AiConfigStore(SharedPrefsAiStore(prefs("migr-config-1")))
        val migrator = LegacyMigrator(AiKeyStore(credentials), config, credentials)

        assertFalse(migrator.run())
        assertTrue(credentials.hasLegacy())
        assertFalse(config.config.value.legacyMigrated)

        // Next launch, the disk works again.
        keyPrefs.failing = false
        assertTrue(LegacyMigrator(AiKeyStore(credentials), config, credentials).run())
        assertFalse(credentials.hasLegacy())
        assertEquals("sk-old", AiKeyStore(credentials).key(BuiltInProviders.GOOGLE))
        assertEquals(listOf(Rung(BuiltInProviders.GOOGLE, "gemini-2.0-flash")), config.config.value.defaultLadder)
    }

    @Test
    fun aFailedConfigWriteKeepsTheOldSetup() = runBlocking {
        val credentials = AiCredentialsStore(SharedPrefsAiStore(prefs("migr-keys-2")))
        assertTrue(credentials.saveLegacy(legacy))
        val configPrefs = FlakyPrefs(prefs("migr-config-2")).apply { failing = true }
        val config = AiConfigStore(SharedPrefsAiStore(configPrefs))

        assertFalse(LegacyMigrator(AiKeyStore(credentials), config, credentials).run())
        assertTrue(credentials.hasLegacy())
        assertFalse(config.config.value.legacyMigrated)
        assertTrue(config.config.value.defaultLadder.isEmpty())
        assertNull(configPrefs.getString("config", null))
    }

    @Test
    fun parallelLadderEditsAllLand() = runBlocking {
        val store = AiConfigStore(SharedPrefsAiStore(prefs("ladder-config")))
        (1..40).map { i ->
            async(Dispatchers.Default) {
                store.update { c -> LadderConfig.edit(c, AiService.STATEMENT_PHOTOS) { it + Rung("openai", "m$i") } }
            }
        }.awaitAll()
        val photos = store.config.value.serviceLadders.getValue(AiService.STATEMENT_PHOTOS)
        assertEquals((1..40).map { "m$it" }.toSet(), photos.map { it.modelId }.toSet())
        assertEquals(40, photos.size)
    }

    @Test
    fun ladderEditRules() {
        val default = listOf(Rung("openai", "a"), Rung("google", "b"))
        val config = AiConfig(defaultLadder = default)
        // A service on the default starts its own ladder from a copy.
        val own = LadderConfig.edit(config, AiService.STATEMENT_IMPORT) { it + Rung("anthropic", "c") }
        assertEquals(default + Rung("anthropic", "c"), own.serviceLadders[AiService.STATEMENT_IMPORT])
        assertEquals(default, own.defaultLadder)
        // An empty result means "use default".
        val back = LadderConfig.edit(own, AiService.STATEMENT_IMPORT) { emptyList() }
        assertTrue(back.serviceLadders.isEmpty())
        // No change, same config.
        assertSame(config, LadderConfig.edit(config, null) { it })
        assertEquals(listOf(Rung("google", "b")), LadderConfig.edit(config, null) { it.drop(1) }.defaultLadder)
    }
}
