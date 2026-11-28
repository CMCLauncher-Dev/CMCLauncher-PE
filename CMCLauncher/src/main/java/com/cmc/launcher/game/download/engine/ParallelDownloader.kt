package com.cmc.launcher.game.download.engine

import com.cmc.launcher.path.URL_USER_AGENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.Semaphore

internal object ParallelDownloader {

    private const val THRESHOLD_BYTES = 8L * 1024 * 1024
    private const val SEGMENTS = 4
    private const val SEGMENT_RETRY = 2
    private const val TIMEOUT_MILLIS = 15_000
    private const val BUFFER_SIZE = 64 * 1024
    private const val MAX_REDIRECTS = 20
    private const val MAX_PARALLEL_CONNECTIONS = 16

    private val slots = Semaphore(MAX_PARALLEL_CONNECTIONS, true)

    private val CONTENT_RANGE = Regex("""bytes ([0-9]+)-([0-9]+)/([0-9]+)""")

    private class Probe(val length: Long, val validator: String)

    suspend fun tryDownload(
        url: URL,
        targetFile: File,
        sha1: String?,
        expectedSize: Long,
        onBytes: (Long) -> Unit
    ): Boolean {
        if (expectedSize in 1 until THRESHOLD_BYTES) return false
        val probe = probe(url) ?: return false
        if (probe.length < THRESHOLD_BYTES) return false

        val parent = targetFile.toPath().toAbsolutePath().parent
        val part = Files.createTempFile(parent, null, null)
        var channel: FileChannel? = null
        var moved = false
        try {
            val file = FileChannel.open(
                part,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            channel = file
            coroutineScope {
                (0 until SEGMENTS).map { index ->
                    async(Dispatchers.IO) {
                        val start = probe.length * index / SEGMENTS
                        val end = probe.length * (index + 1) / SEGMENTS - 1
                        downloadSegment(url, probe, file, start, end, onBytes)
                    }
                }.awaitAll()
            }
            currentCoroutineContext().ensureActive()
            if (part.toFile().length() != probe.length) {
                throw IOException("Parallel download wrote ${part.toFile().length()} of ${probe.length} bytes")
            }
            if (sha1 != null) {
                val actual = sha1Of(part.toFile())
                if (!sha1.equals(actual, ignoreCase = true)) {
                    throw ChecksumMismatchException("SHA-1", sha1, actual)
                }
            }
            channel.close()
            channel = null
            Files.move(part, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            moved = true
            return true
        } finally {
            runCatching { channel?.close() }
            if (!moved) {
                runCatching { Files.deleteIfExists(part) }
            }
        }
    }

    private fun probe(url: URL): Probe? {
        var current = url
        repeat(MAX_REDIRECTS) {
            var connection: HttpURLConnection? = null
            try {
                connection = openConnection(current)
                connection.setRequestProperty("accept-encoding", "identity")
                connection.setRequestProperty("range", "bytes=0-0")
                val code = connection.responseCode
                if (code in 300..308 && code != HttpURLConnection.HTTP_NOT_MODIFIED && code != 306) {
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrBlank()) return null
                    current = current.toURI().resolve(location).toURL()
                    return@repeat
                }
                if (code != HttpURLConnection.HTTP_PARTIAL) return null
                val info = ResponseInfo.of(code, connection.url, connection)
                if (ContentEncoding.fromHeaders(info) != ContentEncoding.IDENTITY) return null
                val match = CONTENT_RANGE.find(info.header("content-range").orEmpty()) ?: return null
                val (start, _, total) = match.destructured
                if (start.toLongOrNull() != 0L) return null
                val length = total.toLongOrNull() ?: return null
                if (length <= 0L) return null
                val etag = info.header("etag")
                val validator = etag?.takeIf { it.isNotBlank() && !it.startsWith("W/", ignoreCase = true) }
                    ?: info.header("last-modified")?.takeIf { it.isNotBlank() }
                    ?: return null
                return Probe(length, validator)
            } catch (e: IOException) {
                return null
            } finally {
                connection?.disconnect()
            }
        }
        return null
    }

    private fun downloadSegment(
        url: URL,
        probe: Probe,
        channel: FileChannel,
        start: Long,
        end: Long,
        onBytes: (Long) -> Unit
    ) {
        slots.acquire()
        var reported = 0L
        try {
            var attempt = 0
            while (true) {
                var connection: HttpURLConnection? = null
                try {
                    connection = openRange(url, start, end, probe.validator)
                    val code = connection.responseCode
                    if (code != HttpURLConnection.HTTP_PARTIAL) {
                        throw IOException("Range request rejected with HTTP $code")
                    }
                    val info = ResponseInfo.of(code, connection.url, connection)
                    if (ContentEncoding.fromHeaders(info) != ContentEncoding.IDENTITY) {
                        throw IOException("Range response uses content encoding")
                    }
                    val match = CONTENT_RANGE.find(info.header("content-range").orEmpty())
                        ?: throw IOException("Range response has no content range")
                    val (rangeStart, rangeEnd, rangeTotal) = match.destructured
                    if (rangeStart.toLongOrNull() != start || rangeEnd.toLongOrNull() != end ||
                        rangeTotal.toLongOrNull() != probe.length
                    ) {
                        throw IOException("Range response does not match the requested segment")
                    }
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var position = start
                        while (true) {
                            if (Thread.currentThread().isInterrupted) {
                                throw InterruptedIOException("Cancelled")
                            }
                            val read = input.read(buffer)
                            if (read < 0) {
                                break
                            }
                            val slice = ByteBuffer.wrap(buffer, 0, read)
                            while (slice.hasRemaining()) {
                                val written = channel.write(slice, position)
                                if (written <= 0) {
                                    throw IOException("Unable to write segment data")
                                }
                                position += written
                            }
                            reported += read
                            onBytes(read.toLong())
                        }
                        if (position != end + 1) {
                            throw IOException("Segment ended at $position, expected ${end + 1}")
                        }
                    }
                    return
                } catch (e: IOException) {
                    if (reported != 0L) {
                        onBytes(-reported)
                        reported = 0L
                    }
                    attempt++
                    if (attempt > SEGMENT_RETRY) {
                        throw e
                    }
                    Thread.sleep(200L * attempt)
                } finally {
                    connection?.disconnect()
                }
            }
        } finally {
            slots.release()
        }
    }

    private fun openRange(url: URL, start: Long, end: Long, validator: String): HttpURLConnection {
        var current = url
        repeat(MAX_REDIRECTS) {
            val connection = openConnection(current)
            connection.setRequestProperty("accept-encoding", "identity")
            connection.setRequestProperty("range", "bytes=$start-$end")
            connection.setRequestProperty("if-range", validator)
            val code = connection.responseCode
            if (code in 300..308 && code != HttpURLConnection.HTTP_NOT_MODIFIED && code != 306) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) {
                    throw IOException("Redirected to an empty location")
                }
                current = current.toURI().resolve(location).toURL()
                return@repeat
            }
            return connection
        }
        throw IOException("Too much redirects")
    }

    private fun openConnection(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", URL_USER_AGENT)
        return connection
    }

    private fun sha1Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}