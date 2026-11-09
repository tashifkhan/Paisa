package codes.tashif.paisa.ai.catalog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import codes.tashif.paisa.ai.provider.ApiStyle
import codes.tashif.paisa.ai.provider.BuiltInProviders
import codes.tashif.paisa.ai.provider.ProviderClient
import codes.tashif.paisa.ai.provider.RouteMode
import codes.tashif.paisa.ai.provider.ThinkingLevel
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The catalogue's disk cache and RelayRoom options, against canned replies. No network. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModelCatalogTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dir = File(context.noBackupFilesDir, "ai")

    private fun catalog(reply: (okhttp3.Request) -> String): ModelCatalog {
        dir.deleteRecursively()
        val client = ProviderClient()
        val stub = OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request()
            // Uneven delays so writers finish out of order.
            Thread.sleep((req.url.host.hashCode() and 7).toLong())
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("x")
                .body(reply(req).toResponseBody("application/json".toMediaType())).build()
        }.build()
        client.http = ProviderClient.client(OkHttp.create { preconfigured = stub })
        return ModelCatalog(FileAiCacheStore(context), LiveModelLister(client), client)
    }

    @Test
    fun parallelRefreshesLeaveACompleteCache() = runBlocking {
        val catalog = catalog { req -> """{"data":[{"id":"m-${req.url.host}"}]}""" }
        val providers = BuiltInProviders.all.filter { it.style == ApiStyle.OPENAI }
        providers.map { p ->
            async(Dispatchers.Default) { catalog.refreshLive(p, p.defaultBaseUrl, "k").getOrThrow() }
        }.awaitAll()

        val onDisk = CatalogCodec.decodeLive(File(dir, "live-models.json").readText())
        assertEquals(providers.map { it.id }.toSet(), onDisk.keys)
        assertEquals(catalog.live.value, onDisk)
        assertTrue(dir.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test
    fun relayOptionsAreKeptPerModeAndReachHandAddedModels() = runBlocking {
        val catalog = catalog { req ->
            if (req.url.queryParameter("mode") == "sub") {
                """{"input_types":["text","image","pdf"],"reasoning":{"supported":true,"levels":["low","high","max"]}}"""
            } else {
                """{"input_types":["text"],"reasoning":{"supported":false}}"""
            }
        }
        val base = BuiltInProviders.RELAYROOM_DEFAULT_URL
        val model = "anthropic/claude-by-hand"
        catalog.refreshRelayOptions(base, "k", model, RouteMode.SUBSCRIPTION).getOrThrow()
        catalog.refreshRelayOptions(base, "k", model, RouteMode.AUTO).getOrThrow()
        val cache = catalog.relayOptions.value
        assertEquals(2, cache.size)

        val manual = ModelInfo(model, source = ModelSource.MANUAL)
        val sub = RelayOptions.forMode(manual, model, base, RouteMode.SUBSCRIPTION, cache)!!
        assertEquals(true, sub.acceptsPdf)
        assertEquals(
            listOf(ThinkingLevel.LOW, ThinkingLevel.HIGH, ThinkingLevel.MAX),
            sub.thinkingChoices(ApiStyle.RELAYROOM)
        )
        val auto = RelayOptions.forMode(manual, model, base, RouteMode.AUTO, cache)!!
        assertEquals(false, auto.acceptsPdf)
        assertTrue(auto.thinkingChoices(ApiStyle.RELAYROOM).isEmpty())
        // A mode never fetched keeps the catalogue entry.
        assertEquals(manual, RelayOptions.forMode(manual, model, base, RouteMode.API_KEY, cache))
        // Another gateway has its own answers.
        assertEquals(manual, RelayOptions.forMode(manual, model, "http://192.168.1.9:8317/v1", RouteMode.SUBSCRIPTION, cache))

        // The list entry, which is the hand-added one here, takes the Auto answer.
        val listed = RelayOptions.overlay(listOf(manual, ModelInfo("other")), "$base/", cache)
        assertEquals(setOf("text"), listed[0].input)
        assertEquals(ModelInfo("other"), listed[1])

        // Survives a restart through the disk cache.
        assertEquals(cache, RelayOptions.decode(File(dir, "relay-options.json").readText()))
    }

    @Test
    fun aFailedWriteKeepsTheOldFile() {
        dir.deleteRecursively()
        dir.mkdirs()
        val target = File(dir, "live-models.json")
        assertTrue(CacheFiles.writeAtomically(target, "old"))
        assertTrue(CacheFiles.writeAtomically(target, "new"))
        assertEquals("new", target.readText())

        // A rename onto a non-empty directory fails; what was there stays.
        val blocked = File(dir, "blocked.json").apply { mkdirs() }
        File(blocked, "child").writeText("keep")
        assertFalse(CacheFiles.writeAtomically(blocked, "x"))
        assertEquals("keep", File(blocked, "child").readText())
        assertTrue(dir.listFiles()!!.none { it.name.endsWith(".tmp") })
    }
}
