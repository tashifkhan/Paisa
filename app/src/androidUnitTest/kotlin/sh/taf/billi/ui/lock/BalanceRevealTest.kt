package sh.taf.billi.ui.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceRevealTest {
    @Test
    fun revealsOnlyAfterSuccessfulAuthentication() {
        val biometrics = TestBiometrics()
        var revealed = false
        biometrics.authenticateToShowBalances(
            onSuccess = { revealed = true },
            onError = { error(it) }
        )

        assertFalse(revealed)
        biometrics.onFailed()
        assertFalse(revealed)
        biometrics.onSuccess()
        assertTrue(revealed)
    }

    @Test
    fun keepsBalancesHiddenWhenDeviceAuthenticationIsUnavailable() {
        val biometrics = TestBiometrics(available = false)
        var message: String? = null
        biometrics.authenticateToShowBalances(
            onSuccess = { error("Balances should stay hidden") },
            onError = { message = it }
        )

        assertEquals(biometrics.statusMessage, message)
        assertFalse(biometrics.prompted)
    }

    @Test
    fun preservesRevealOnPlatformsWithoutDeviceAuthentication() {
        val biometrics = TestBiometrics(supported = false, available = false)
        var revealed = false
        biometrics.authenticateToShowBalances(
            onSuccess = { revealed = true },
            onError = { error(it) }
        )

        assertTrue(revealed)
        assertFalse(biometrics.prompted)
    }

    private class TestBiometrics(
        override val supported: Boolean = true,
        override val available: Boolean = true
    ) : Biometrics {
        override val statusMessage = "Device authentication is unavailable"
        var prompted = false
        lateinit var onSuccess: () -> Unit
        lateinit var onFailed: () -> Unit

        override fun authenticate(
            title: String,
            subtitle: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
            onFailed: () -> Unit
        ) {
            prompted = true
            this.onSuccess = onSuccess
            this.onFailed = onFailed
        }
    }
}
