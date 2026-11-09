package codes.tashif.paisa.backup

import codes.tashif.paisa.testing.TinyHttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class NextcloudServerUrlTest {

    @Test
    fun httpsAndBareHostsAreFine() {
        assertNull(NextcloudStore.serverError("https://cloud.example.com"))
        assertNull(NextcloudStore.serverError("cloud.example.com"))
    }

    @Test
    fun plainHttpOnlyForLocalAddresses() {
        assertNull(NextcloudStore.serverError("http://192.168.1.5"))
        assertNull(NextcloudStore.serverError("http://10.0.2.2:8080"))
        assertNull(NextcloudStore.serverError("http://localhost"))
        assertNotNull(NextcloudStore.serverError("http://cloud.example.com"))
        assertNotNull(NextcloudStore.serverError("http://8.8.8.8"))
    }

    @Test
    fun credentialsInTheUrlAreRejected() {
        assertNotNull(NextcloudStore.serverError("https://me:pw@cloud.example.com"))
    }

    private fun flowJson(login: String, endpoint: String) =
        """{"poll":{"token":"t0k","endpoint":"$endpoint"},"login":"$login"}"""

    @Test
    fun loginFlowAddressesFollowTheServerRule() {
        val ok = NextcloudStore.parseLoginFlow(
            flowJson("https://cloud.example.com/login/v2/flow/abc", "https://cloud.example.com/login/v2/poll")
        )
        assertEquals("t0k", ok.token)
        for ((login, endpoint) in listOf(
            "https://cloud.example.com/login" to "http://evil.example.com/poll",
            "http://evil.example.com/login" to "https://cloud.example.com/poll",
            "https://cloud.example.com/login" to "https://me:pw@cloud.example.com/poll",
            "https://cloud.example.com/login" to "not a url",
        )) {
            try {
                NextcloudStore.parseLoginFlow(flowJson(login, endpoint))
                fail("accepted $login / $endpoint")
            } catch (_: IOException) {
            }
        }
    }

    @Test
    fun pollingRefusesAnUnsafeEndpointWithoutSending() = runBlocking {
        try {
            NextcloudStore.pollLogin(NextcloudStore.Companion.LoginFlow("https://x", "http://evil.example.com/poll", "t0k"))
            fail("expected failure")
        } catch (_: IOException) {
        }
    }

    @Test
    fun pollingNeverFollowsARedirectWithTheToken() = runBlocking {
        TinyHttpServer { TinyHttpServer.Reply(200, "{}") }.use { elsewhere ->
            TinyHttpServer { TinyHttpServer.Reply(307, headers = mapOf("Location" to "${elsewhere.url}/poll")) }.use { server ->
                try {
                    NextcloudStore.pollLogin(NextcloudStore.Companion.LoginFlow("${server.url}/login", "${server.url}/poll", "t0k"))
                    fail("expected failure")
                } catch (e: IOException) {
                    assertTrue(e.message!!.contains("redirected"))
                }
                assertEquals(1, server.requests.size)
                assertTrue(server.requests.single().body.contains("t0k"))
                assertEquals(0, elsewhere.requests.size)
            }
        }
    }
}
