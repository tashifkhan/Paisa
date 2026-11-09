package codes.tashif.paisa.ai.provider

/**
 * The parts of an http(s) URL the AI settings check, parsed the way OkHttp's
 * `toHttpUrlOrNull` did: scheme, user info, a lowercase host (IPv6 without
 * brackets) and a port. [parse] gives null for anything OkHttp rejected
 * that these checks rely on: another scheme, an empty or invalid host, or a
 * bad port.
 */
internal data class AiUrl(
    val scheme: String,
    val username: String,
    val password: String,
    val host: String,
    val port: Int,
) {
    val isHttps: Boolean get() = scheme == "https"

    companion object {
        /** Characters OkHttp refuses in a host name. */
        private const val INVALID_HOST_CHARS = " #%/:?@[\\]"

        fun parse(input: String): AiUrl? {
            val text = input.trim()
            val colon = text.indexOf(':')
            if (colon <= 0) return null
            val scheme = text.substring(0, colon).lowercase()
            if (scheme != "http" && scheme != "https") return null
            var rest = text.substring(colon + 1)
            // OkHttp takes any mix of slashes and backslashes after the scheme.
            rest = rest.trimStart('/', '\\')
            val authorityEnd = rest.indexOfFirst { it == '/' || it == '\\' || it == '?' || it == '#' }
            val authority = if (authorityEnd < 0) rest else rest.substring(0, authorityEnd)

            val at = authority.lastIndexOf('@')
            val userInfo = if (at >= 0) authority.substring(0, at) else ""
            val hostPort = if (at >= 0) authority.substring(at + 1) else authority
            val username = userInfo.substringBefore(':')
            val password = if (':' in userInfo) userInfo.substringAfter(':') else ""

            val (rawHost, rawPort) = splitHostPort(hostPort) ?: return null
            val host = canonicalHost(rawHost) ?: return null
            val port = if (rawPort == null) {
                if (scheme == "https") 443 else 80
            } else {
                rawPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            }
            return AiUrl(scheme, username, password, host, port)
        }

        /** Host and the text after its port colon, or null when the brackets don't close. */
        private fun splitHostPort(hostPort: String): Pair<String, String?>? {
            if (hostPort.startsWith('[')) {
                val close = hostPort.indexOf(']')
                if (close < 0) return null
                val after = hostPort.substring(close + 1)
                val port = when {
                    after.isEmpty() -> null
                    after.startsWith(':') -> after.substring(1)
                    else -> return null
                }
                return hostPort.substring(0, close + 1) to port
            }
            val colon = hostPort.lastIndexOf(':')
            return if (colon < 0) hostPort to null else hostPort.substring(0, colon) to hostPort.substring(colon + 1)
        }

        private fun canonicalHost(raw: String): String? {
            if (raw.isEmpty()) return null
            if (raw.startsWith('[')) {
                val inner = raw.substring(1, raw.length - 1).lowercase()
                if (inner.isEmpty() || inner.count { it == ':' } < 2) return null
                if (!inner.all { it.isDigit() || it in 'a'..'f' || it == ':' || it == '.' }) return null
                return inner
            }
            val host = raw.lowercase()
            if (host.any { it.code <= 0x1f || it.code == 0x7f || it in INVALID_HOST_CHARS }) return null
            return host
        }
    }
}
