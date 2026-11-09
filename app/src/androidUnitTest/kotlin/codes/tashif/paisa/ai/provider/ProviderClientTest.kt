package codes.tashif.paisa.ai.provider

import codes.tashif.paisa.ai.LlmException
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import codes.tashif.paisa.testing.TinyHttpServer

/** Runs ProviderClient against canned replies from an interceptor. No network. */
class ProviderClientTest {

    private val provider = BuiltInProviders.byId(BuiltInProviders.OPENAI)!!
    private val call = LlmCall(provider, provider.defaultBaseUrl, "k", "gpt-4o-mini")
    private val request = LlmRequest("sys", "hi", json = true)
    private val ok = """{"choices":[{"message":{"content":"{}"},"finish_reason":"stop"}]}"""

    private fun client(vararg replies: Pair<Int, String>): Pair<ProviderClient, MutableList<String>> {
        val bodies = mutableListOf<String>()
        var index = 0
        val client = ProviderClient()
        val stub = OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request()
            val buffer = okio.Buffer()
            req.body?.writeTo(buffer)
            bodies += buffer.readUtf8()
            val (code, body) = replies[minOf(index++, replies.size - 1)]
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("x")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        client.http = ProviderClient.client(OkHttp.create { preconfigured = stub })
        return client to bodies
    }

    @Test
    fun one_retry_on_429_then_success() = runTest {
        val (client, bodies) = client(429 to "{}", 200 to ok)
        assertEquals("{}", client.complete(call, request))
        assertEquals(2, bodies.size)
    }

    @Test
    fun at_most_one_retry_on_5xx() = runTest {
        val (client, bodies) = client(500 to """{"error":{"message":"down"}}""")
        try {
            client.complete(call, request)
            fail("expected failure")
        } catch (e: LlmException) {
            assertEquals(500, e.status)
            assertTrue(e.message!!.contains("down"))
        }
        assertEquals(2, bodies.size)
    }

    @Test
    fun no_retry_on_4xx_and_json_mode_falls_back_once() = runTest {
        val (client, bodies) = client(400 to "response_format not supported", 200 to ok)
        assertEquals("{}", client.complete(call, request))
        assertEquals(2, bodies.size)
        assertTrue(bodies[0].contains("response_format"))
        assertTrue(!bodies[1].contains("response_format"))

        val (strict, sent) = client(401 to """{"error":{"message":"bad key"}}""")
        runCatching { strict.complete(call, request) }
        assertEquals(1, sent.size)
    }

    @Test
    fun unsafe_base_url_never_sends() = runTest {
        val (client, bodies) = client(200 to ok)
        val bad = call.copy(baseUrl = "http://api.example.com/v1")
        assertTrue(runCatching { client.complete(bad, request) }.isFailure)
        assertEquals(0, bodies.size)
    }

    @Test
    fun redirects_are_never_followed_with_the_key() = runTest {
        TinyHttpServer { TinyHttpServer.Reply(200, ok) }.use { elsewhere ->
            TinyHttpServer { TinyHttpServer.Reply(302, headers = mapOf("Location" to "${elsewhere.url}/v1/chat/completions")) }
                .use { server ->
                    val client = ProviderClient()
                    val local = call.copy(baseUrl = "${server.url}/v1")
                    try {
                        client.complete(local, request)
                        fail("expected failure")
                    } catch (e: LlmException) {
                        assertEquals(302, e.status)
                        assertTrue(e.message!!.contains("redirected"))
                    }
                    // A model listing GET is refused the same way.
                    assertTrue(runCatching { client.get("${server.url}/v1/models", mapOf("x-api-key" to "k")) }.isFailure)
                    assertEquals(2, server.requests.size)
                    assertEquals(0, elsewhere.requests.size)
                }
        }
    }
}
