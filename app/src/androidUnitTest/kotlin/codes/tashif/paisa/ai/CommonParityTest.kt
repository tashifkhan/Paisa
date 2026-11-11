package codes.tashif.paisa.ai

import codes.tashif.paisa.ai.catalog.formEncode
import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject
import codes.tashif.paisa.ai.json.JSONTokener
import codes.tashif.paisa.ai.provider.AiUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

/** The common replacements give what the JVM code they replaced gave. */
class CommonParityTest {

    @Test
    fun md5MatchesParserCore() {
        val inputs = listOf("", "a", "statement|3|2026-07-04|123.45|expense|swiggy||", "₹ ünïcødé".repeat(40))
        for (input in inputs) {
            assertEquals(com.pennywiseai.parser.core.md5Hex(input), md5Hex(input))
        }
    }

    @Test
    fun formEncodeMatchesUrlEncoder() {
        for (value in listOf("abc", "a b+c", "models/gemini-2.5:pro", "页面 token=/&?", "x.y-z*_~")) {
            assertEquals(URLEncoder.encode(value, "UTF-8"), formEncode(value))
        }
    }

    @Test
    fun urlPartsMatchOkHttp() {
        val urls = listOf(
            "https://api.openai.com/v1",
            "http://10.0.2.2:8317/v1",
            "http://192.168.1.5:8080/v1/chat",
            "http://[fd12:3456::1]/v1",
            "https://user:pass@api.example.com/v1",
            "http://user@10.0.2.2:8317/v1",
            "HTTPS://API.Example.COM/v1",
        )
        for (url in urls) {
            val ok = url.toHttpUrlOrNull()!!
            val ours = AiUrl.parse(url)!!
            assertEquals(url, ok.host, ours.host)
            assertEquals(url, ok.username, ours.username)
            assertEquals(url, ok.password, ours.password)
            assertEquals(url, ok.isHttps, ours.isHttps)
            assertEquals(url, ok.port, ours.port)
        }
        for (bad in listOf("ftp://10.0.2.2/v1", "api.openai.com/v1", "https://", "http://host:99999/v1", "http://ho st/v1")) {
            assertNull(bad, bad.toHttpUrlOrNull())
            assertNull(bad, AiUrl.parse(bad))
        }
    }

    @Test
    fun jsonFollowsAndroidOrgJsonRules() {
        val body = JSONObject().put("model", "m").put("max_tokens", 8192).put("temperature", 0.1)
            .put("whole", 2.0).put("on", true).put("gone", null)
            .put("messages", JSONArray().put(JSONObject().put("role", "user")))
        assertEquals(
            """{"model":"m","max_tokens":8192,"temperature":0.1,"whole":2,"on":true,"messages":[{"role":"user"}]}""",
            body.toString()
        )

        val parsed = JSONObject("""{"a":12,"b":12.5,"c":"7","d":null,"e":"true","big":12345678901} trailing""")
        assertEquals(12, parsed.opt("a"))
        assertEquals(12.5, parsed.opt("b"))
        assertEquals(12345678901L, parsed.opt("big"))
        assertEquals(7, parsed.optInt("c"))
        assertEquals("12", parsed.optString("a"))
        assertEquals("null", parsed.optString("d"))
        assertTrue(parsed.has("d"))
        assertTrue(parsed.isNull("d"))
        assertTrue(parsed.optBoolean("e"))
        assertEquals("", parsed.optString("missing"))
        assertEquals(listOf("a", "b", "c", "d", "e", "big"), parsed.keys().asSequence().toList())

        assertTrue(JSONTokener("""{"transactions":[]} and some words""").nextValue() is JSONObject)
        assertFalse(runCatching { JSONObject("not json") }.isSuccess)
        assertFalse(runCatching { JSONObject("") }.isSuccess)
    }
}
