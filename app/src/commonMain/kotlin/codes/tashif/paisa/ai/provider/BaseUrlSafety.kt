package codes.tashif.paisa.ai.provider

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The app allows cleartext traffic so RelayRoom can run on a laptop, so every
 * AI base URL passes through here first. https goes anywhere. http only goes
 * to this device, the emulator host or a private network, and no URL may
 * carry a user name or password.
 */
object BaseUrlSafety {

    class UnsafeUrlException(message: String) : IllegalArgumentException(message)

    /** Returns the URL without a trailing slash, or throws [UnsafeUrlException]. */
    fun check(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isEmpty()) throw UnsafeUrlException("Base URL is required.")
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            throw UnsafeUrlException("Base URL must start with https://")
        }
        val url = trimmed.toHttpUrlOrNull()
            ?: throw UnsafeUrlException("Base URL isn't valid. Use a full URL like https://api.example.com/v1")
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
            throw UnsafeUrlException("Don't put a user name or password in the base URL.")
        }
        if (!url.isHttps && !isLocalHost(url.host)) {
            throw UnsafeUrlException(
                "http only works for this device or your local network. Use https so your key " +
                    "isn't sent in plain text."
            )
        }
        return trimmed
    }

    fun isSafe(input: String): Boolean = runCatching { check(input) }.isSuccess

    /** Loopback, the emulator host (10.0.2.2), private IPv4 ranges and IPv6 local ranges. */
    fun isLocalHost(host: String): Boolean {
        val h = host.lowercase().removePrefix("[").removeSuffix("]")
        if (h == "localhost" || h.endsWith(".localhost")) return true
        ipv4(h)?.let { return isPrivateIpv4(it) }
        if (h.contains(':')) return isLocalIpv6(h)
        return false
    }

    private fun ipv4(host: String): IntArray? {
        val parts = host.split('.')
        if (parts.size != 4) return null
        val octets = parts.map { part ->
            if (part.isEmpty() || part.length > 3 || !part.all(Char::isDigit)) return null
            part.toInt().takeIf { it in 0..255 } ?: return null
        }
        return octets.toIntArray()
    }

    private fun isPrivateIpv4(o: IntArray): Boolean = when {
        o[0] == 127 -> true                                  // loopback
        o[0] == 10 -> true                                   // 10/8, includes the emulator host
        o[0] == 172 && o[1] in 16..31 -> true                // 172.16/12
        o[0] == 192 && o[1] == 168 -> true                   // 192.168/16
        o[0] == 169 && o[1] == 254 -> true                   // link-local
        o[0] == 100 && o[1] in 64..127 -> true               // 100.64/10, Tailscale and CGNAT
        else -> false
    }

    private fun isLocalIpv6(host: String): Boolean {
        if (host == "::1") return true
        val first = host.substringBefore(':').ifEmpty { return false }
        val word = first.toIntOrNull(16) ?: return false
        return (word and 0xfe00) == 0xfc00 ||  // fc00::/7 unique local
            (word and 0xffc0) == 0xfe80        // fe80::/10 link-local
    }
}
