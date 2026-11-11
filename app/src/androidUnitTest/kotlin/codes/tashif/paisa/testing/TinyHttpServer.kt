package codes.tashif.paisa.testing

import java.io.BufferedInputStream
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.Collections
import kotlin.concurrent.thread

/**
 * A loopback HTTP/1.1 server for tests that need real OkHttp behaviour, such
 * as redirects. Each connection gets one canned reply, then closes.
 * MockWebServer isn't on the test classpath, and this is all the tests need.
 */
class TinyHttpServer(private val reply: (Request) -> Reply) : Closeable {

    data class Request(val method: String, val path: String, val headers: Map<String, String>, val body: String)
    data class Reply(val status: Int, val body: String = "", val headers: Map<String, String> = emptyMap())

    private val socket = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
    val requests: MutableList<Request> = Collections.synchronizedList(mutableListOf())

    /** `http://127.0.0.1:<port>`, with no trailing slash. */
    val url: String = "http://127.0.0.1:${socket.localPort}"

    init {
        thread(isDaemon = true, name = "tiny-http-${socket.localPort}") {
            while (!socket.isClosed) {
                val client = try {
                    socket.accept()
                } catch (_: SocketException) {
                    break
                }
                client.use { conn ->
                    val input = BufferedInputStream(conn.getInputStream())
                    val requestLine = readLine(input) ?: return@use
                    val headers = LinkedHashMap<String, String>()
                    while (true) {
                        val line = readLine(input) ?: break
                        if (line.isEmpty()) break
                        val colon = line.indexOf(':')
                        if (colon > 0) headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
                    }
                    val length = headers["content-length"]?.toIntOrNull() ?: 0
                    val body = ByteArray(length)
                    var read = 0
                    while (read < length) {
                        val n = input.read(body, read, length - read)
                        if (n < 0) break
                        read += n
                    }
                    val parts = requestLine.split(' ')
                    val request = Request(parts[0], parts.getOrElse(1) { "/" }, headers, String(body, 0, read))
                    requests += request
                    val out = reply(request)
                    val bytes = out.body.toByteArray()
                    val text = buildString {
                        append("HTTP/1.1 ${out.status} X\r\n")
                        out.headers.forEach { (k, v) -> append("$k: $v\r\n") }
                        append("Content-Type: application/json\r\n")
                        append("Content-Length: ${bytes.size}\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    conn.getOutputStream().apply {
                        write(text.toByteArray())
                        write(bytes)
                        flush()
                    }
                }
            }
        }
    }

    private fun readLine(input: BufferedInputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c < 0) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) return sb.toString().removeSuffix("\r")
            sb.append(c.toChar())
        }
    }

    override fun close() = socket.close()
}
