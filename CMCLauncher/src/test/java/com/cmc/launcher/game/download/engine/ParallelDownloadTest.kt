package com.cmc.launcher.game.download.engine

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

class ParallelDownloadTest {

    private val size = 9 * 1024 * 1024 + 123
    private val payload = ByteArray(size) { (it % 251).toByte() }
    private val sha1 = MessageDigest.getInstance("SHA-1").digest(payload).joinToString("") { "%02x".format(it) }
    private val rangeRequests = AtomicInteger(0)
    private var server: HttpServer? = null

    @After
    fun stop() {
        server?.stop(0)
    }

    @Test
    fun downloadsLargeFileWithRanges() {
        runBlocking {
            val base = start(true)
            val target = Files.createTempFile("parallel-range", ".bin").toFile()
            Fetcher.downloadFile(
                urls = listOf(base + "/range"),
                targetFile = target,
                sha1 = sha1,
                expectedSize = size.toLong()
            )
            assertEquals(size.toLong(), target.length())
            assertTrue(rangeRequests.get() >= 2)
            target.delete()
        }
    }

    @Test
    fun fallsBackWhenRangesAreIgnored() {
        runBlocking {
            val base = start(false)
            val target = Files.createTempFile("parallel-plain", ".bin").toFile()
            Fetcher.downloadFile(
                urls = listOf(base + "/plain"),
                targetFile = target,
                sha1 = sha1,
                expectedSize = size.toLong()
            )
            assertEquals(size.toLong(), target.length())
            assertEquals(0, rangeRequests.get())
            target.delete()
        }
    }

    private fun start(supportRanges: Boolean): String {
        val http = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        http.createContext("/") { exchange ->
            if (supportRanges) {
                handleRange(exchange)
            } else {
                respond(exchange, 200, null)
            }
        }
        http.start()
        server = http
        return "http://127.0.0.1:" + http.address.port
    }

    private fun handleRange(exchange: HttpExchange) {
        val header = exchange.requestHeaders.getFirst("Range")
        val match = header?.let { Regex("""bytes=([0-9]+)-([0-9]+)""").find(it) }
        if (match == null) {
            respond(exchange, 200, null)
            return
        }
        rangeRequests.incrementAndGet()
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toInt().coerceAtMost(size - 1)
        respond(exchange, 206, IntRange(start, end))
    }

    private fun respond(exchange: HttpExchange, code: Int, range: IntRange?) {
        val start = range?.first ?: 0
        val end = range?.last ?: size - 1
        exchange.responseHeaders.set("ETag", "\"parallel-test\"")
        exchange.responseHeaders.set("Accept-Ranges", "bytes")
        if (range != null) {
            exchange.responseHeaders.set("Content-Range", "bytes $start-$end/$size")
        }
        exchange.sendResponseHeaders(code, (end - start + 1).toLong())
        exchange.responseBody.use { body ->
            body.write(payload, start, end - start + 1)
        }
        exchange.close()
    }
}