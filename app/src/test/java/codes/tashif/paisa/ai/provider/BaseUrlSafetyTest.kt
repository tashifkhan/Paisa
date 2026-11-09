package codes.tashif.paisa.ai.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUrlSafetyTest {

    @Test
    fun https_is_allowed_anywhere() {
        assertEquals("https://api.openai.com/v1", BaseUrlSafety.check(" https://api.openai.com/v1/ "))
        assertTrue(BaseUrlSafety.isSafe("https://8.8.8.8/v1"))
    }

    @Test
    fun http_only_to_local_hosts() {
        val allowed = listOf(
            "http://10.0.2.2:8317/v1",
            "http://localhost:8317/v1",
            "http://127.0.0.1:8317/v1",
            "http://192.168.1.20:8317/v1",
            "http://172.16.0.5/v1",
            "http://172.31.255.1/v1",
            "http://10.1.2.3/v1",
            "http://100.101.102.103:8317/v1",
            "http://[::1]:8317/v1",
            "http://[fd12:3456::1]/v1",
            "http://[fe80::1]/v1",
        )
        allowed.forEach { assertTrue(it, BaseUrlSafety.isSafe(it)) }

        val blocked = listOf(
            "http://api.openai.com/v1",
            "http://8.8.8.8/v1",
            "http://172.32.0.1/v1",
            "http://192.169.0.1/v1",
            "http://10.0.2.2.evil.com/v1",
            "http://[2001:db8::1]/v1",
            "http://example.local/v1",
        )
        blocked.forEach { assertFalse(it, BaseUrlSafety.isSafe(it)) }
    }

    @Test
    fun credentials_in_the_url_are_refused() {
        assertFalse(BaseUrlSafety.isSafe("https://user:pass@api.example.com/v1"))
        assertFalse(BaseUrlSafety.isSafe("http://user@10.0.2.2:8317/v1"))
    }

    @Test
    fun other_schemes_and_junk_are_refused() {
        assertFalse(BaseUrlSafety.isSafe(""))
        assertFalse(BaseUrlSafety.isSafe("ftp://10.0.2.2/v1"))
        assertFalse(BaseUrlSafety.isSafe("api.openai.com/v1"))
        assertFalse(BaseUrlSafety.isSafe("https://"))
    }
}
