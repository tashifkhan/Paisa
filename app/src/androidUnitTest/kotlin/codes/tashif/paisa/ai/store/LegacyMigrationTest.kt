package codes.tashif.paisa.ai.store

import codes.tashif.paisa.ai.AiCredentials
import codes.tashif.paisa.ai.AiProvider
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.ladder.Rung
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.CustomProvider
import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyMigrationTest {

    private fun creds(provider: AiProvider, base: String, model: String, key: String = " sk-1 ") =
        AiCredentials(provider, key, base, model)

    @Test
    fun openai_default_url_becomes_the_openai_provider() {
        val m = LegacyMigration.migrate(
            creds(AiProvider.OPENAI_COMPATIBLE, "https://api.openai.com/v1/", "gpt-4o-mini"), AiConfig()
        )!!
        assertEquals(BuiltInProviders.OPENAI, m.providerId)
        assertEquals("sk-1", m.apiKey)
        assertEquals(listOf(Rung(BuiltInProviders.OPENAI, "gpt-4o-mini")), m.config.defaultLadder)
        assertEquals(listOf("gpt-4o-mini"), m.config.manualModels[BuiltInProviders.OPENAI])
        assertTrue(m.config.legacyMigrated)
        assertTrue(m.config.customProviders.isEmpty())
        assertTrue(m.config.serviceLadders.isEmpty())
    }

    @Test
    fun known_compatible_url_maps_to_its_preset() {
        val m = LegacyMigration.migrate(
            creds(AiProvider.OPENAI_COMPATIBLE, "https://openrouter.ai/api/v1/chat/completions", "x/y"), AiConfig()
        )!!
        assertEquals(BuiltInProviders.OPENROUTER, m.providerId)
    }

    @Test
    fun unknown_compatible_url_becomes_a_custom_provider() {
        val m = LegacyMigration.migrate(
            creds(AiProvider.OPENAI_COMPATIBLE, "http://192.168.1.5:11434/v1", "llama3"), AiConfig()
        )!!
        val custom = m.config.customProviders.single()
        assertEquals(custom.id, m.providerId)
        assertEquals(ApiStyle.OPENAI, custom.style)
        assertEquals("http://192.168.1.5:11434/v1", custom.baseUrl)
        assertEquals("Custom (192.168.1.5)", custom.name)
        assertEquals(Rung(custom.id, "llama3"), m.config.defaultLadder.single())
    }

    @Test
    fun gemini_and_anthropic_keep_a_non_default_base_url() {
        val g = LegacyMigration.migrate(
            creds(AiProvider.GEMINI, "https://generativelanguage.googleapis.com/v1beta", "gemini-2.0-flash"), AiConfig()
        )!!
        assertEquals(BuiltInProviders.GOOGLE, g.providerId)
        assertTrue(g.config.baseUrls.isEmpty())

        val a = LegacyMigration.migrate(
            creds(AiProvider.ANTHROPIC, "https://proxy.example.com/", "claude-3-5-haiku-latest"), AiConfig()
        )!!
        assertEquals(BuiltInProviders.ANTHROPIC, a.providerId)
        assertEquals("https://proxy.example.com", a.config.baseUrls[BuiltInProviders.ANTHROPIC])
    }

    @Test
    fun nothing_to_move_without_a_key() {
        assertNull(LegacyMigration.migrate(AiCredentials(), AiConfig()))
        assertNull(LegacyMigration.migrate(creds(AiProvider.GEMINI, "", "gemini-2.0-flash", key = ""), AiConfig()))
    }

    @Test
    fun an_existing_default_ladder_is_kept() {
        val existing = AiConfig(defaultLadder = listOf(Rung("anthropic", "claude-opus-5-5")))
        val m = LegacyMigration.migrate(creds(AiProvider.GEMINI, "", "gemini-2.0-flash"), existing)!!
        assertEquals(existing.defaultLadder, m.config.defaultLadder)
    }

    @Test
    fun old_screen_round_trip() {
        val saved = LegacyMigration.applyOldScreen(
            creds(AiProvider.OPENAI_COMPATIBLE, "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
            AiConfig(defaultLadder = listOf(Rung("openai", "gpt-4o-mini"), Rung("google", "gemini-2.5-flash")))
        )
        assertEquals(listOf(Rung(BuiltInProviders.GROQ, "llama-3.3-70b-versatile")), saved.config.defaultLadder)

        val view = LegacyMigration.toLegacy(
            saved.config,
            keyFor = { if (it == BuiltInProviders.GROQ) "gk" else null },
            baseUrlFor = { it.defaultBaseUrl }
        )
        assertEquals(AiProvider.OPENAI_COMPATIBLE, view.provider)
        assertEquals("gk", view.apiKey)
        assertEquals("https://api.groq.com/openai/v1", view.baseUrl)
        assertEquals("llama-3.3-70b-versatile", view.model)
        assertTrue(view.isConfigured)
        assertTrue(!LegacyMigration.toLegacy(AiConfig(), { null }, { it.defaultBaseUrl }).isConfigured)
    }

    @Test
    fun config_json_round_trip() {
        val config = AiConfig(
            customProviders = listOf(CustomProvider("custom-x", "X", ApiStyle.GEMINI, "https://x.example/v1beta")),
            baseUrls = mapOf("relayroom" to "http://192.168.1.9:8317/v1"),
            defaultLadder = listOf(
                Rung("relayroom", "google/gemini-2.5-flash", ThinkingLevel.HIGH, RouteMode.SUBSCRIPTION),
                Rung("openai", "gpt-4o-mini"),
            ),
            serviceLadders = mapOf(AiService.STATEMENT_PHOTOS to listOf(Rung("google", "gemini-3.6-flash", ThinkingLevel.MINIMAL))),
            manualModels = mapOf("custom-x" to listOf("m1", "m2")),
            legacyMigrated = true,
        )
        assertEquals(config, AiConfigCodec.decode(AiConfigCodec.encode(config)))
        assertEquals(AiConfig(), AiConfigCodec.decode("{broken"))
        assertEquals(AiConfig(), AiConfigCodec.decode(null))
    }
}
