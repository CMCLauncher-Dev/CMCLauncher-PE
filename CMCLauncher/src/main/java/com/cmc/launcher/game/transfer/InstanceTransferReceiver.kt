package com.cmc.launcher.game.transfer

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.zip.ZipFile
import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.coroutineContext

class TransferException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason {
        INVALID_ADDRESS,
        CONNECT_FAILED,
        PAIRING_REJECTED,
        INVALID_ARCHIVE,
        BATCH_UNSUPPORTED,
        NO_SPACE,
        IO_ERROR,
        VERSION_UNSUPPORTED,
        INSTALL_FAILED
    }
}

data class TransferProfile(
    val name: String,
    val description: String,
    val gameVersion: String,
    val loader: String,
    val loaderVersion: String?,
    val hasRuntime: Boolean = false
)

data class TransferResult(
    val profile: TransferProfile,
    val target: File,
    val files: Int,
    val bytes: Long
)

object InstanceTransferReceiver {
    private const val MANIFEST = "instance.json"
    private const val BATCH_MANIFEST = "instances.json"
    private const val BUFFER = 64 * 1024

    fun splitAddress(raw: String): Pair<String, Int>? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val separator = text.lastIndexOf(':')
        if (separator <= 0 || separator == text.lastIndex) return null
        val host = text.substring(0, separator).trim().removeSurrounding("[", "]")
        val port = text.substring(separator + 1).trim().toIntOrNull() ?: return null
        if (host.isEmpty() || host.any(Char::isWhitespace) || port !in 1..65535) return null
        return host to port
    }

    suspend fun receive(
        address: String,
        target: File,
        onBytes: (Long, Long) -> Unit = { _, _ -> },
        onImport: (Int) -> Unit = {},
        onAwaitingApproval: (String) -> Unit = {},
        socketFactory: SocketFactory = SocketFactory.getDefault()
    ): TransferResult = receive(address, { target }, onBytes, onImport, onAwaitingApproval, socketFactory)

    suspend fun receive(
        address: String,
        resolveTarget: suspend (TransferProfile) -> File,
        onBytes: (Long, Long) -> Unit = { _, _ -> },
        onImport: (Int) -> Unit = {},
        onAwaitingApproval: (String) -> Unit = {},
        socketFactory: SocketFactory = SocketFactory.getDefault()
    ): TransferResult = receiveArchive(
        address, { profile, _ -> resolveTarget(profile) }, onBytes, onImport, onAwaitingApproval, socketFactory
    )

    internal suspend fun receiveArchive(
        address: String,
        resolveTarget: suspend (TransferProfile, ZipFile) -> File,
        onBytes: (Long, Long) -> Unit,
        onImport: (Int) -> Unit,
        onAwaitingApproval: (String) -> Unit,
        socketFactory: SocketFactory = SocketFactory.getDefault()
    ): TransferResult = withContext(Dispatchers.IO) {
        val endpoint = splitAddress(address) ?: throw TransferException(TransferException.Reason.INVALID_ADDRESS)
        val temporary = File.createTempFile("cmc-transfer", ".zip")
        try {
            val stream = openStream(endpoint.first, endpoint.second, socketFactory)
            stream.useTransferSocket {
                val output = stream.outputStream
                val input = DataInputStream(BufferedInputStream(stream.inputStream, BUFFER))
                val length = try {
                    val nonce = ByteArray(32).also { SecureRandom().nextBytes(it) }
                    stream.soTimeout = 65000
                    output.write(TransferProtocol.connectionRequest)
                    output.write(nonce)
                    output.flush()
                    onAwaitingApproval(TransferProtocol.verification(stream.session.peerCertificates.first(), nonce))
                    if (input.readUnsignedByte() != 1) {
                        throw TransferException(TransferException.Reason.PAIRING_REJECTED)
                    }
                    input.readLong()
                } catch (error: java.io.IOException) {
                    throw TransferException(TransferException.Reason.PAIRING_REJECTED, error)
                }
                stream.soTimeout = 30000
                val result = try {
                    if (length <= 0L || length > TransferProtocol.maxArchiveBytes) {
                        throw TransferException(TransferException.Reason.INVALID_ARCHIVE)
                    }
                    if (temporary.usableSpace > 0 && temporary.usableSpace < length) {
                        throw TransferException(TransferException.Reason.NO_SPACE)
                    }
                    writePayload(input, length, temporary, onBytes)
                    importPayload(temporary, resolveTarget, onImport)
                } catch (error: Exception) {
                    runCatching { output.write(0); output.flush() }
                    throw error
                }
                runCatching { output.write(1); output.flush() }
                result
            }
        } finally {
            temporary.delete()
        }
    }

    private fun openStream(host: String, port: Int, socketFactory: SocketFactory): SSLSocket {
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (chain.isNullOrEmpty()) throw CertificateException("Missing certificate")
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf(trust), SecureRandom())
        val socket = socketFactory.createSocket()
        try {
            socket.connect(InetSocketAddress(host, port), 10000)
            return (context.socketFactory.createSocket(socket, host, port, true) as SSLSocket).apply {
                soTimeout = 30000
                startHandshake()
            }
        } catch (error: Exception) {
            runCatching { socket.close() }
            throw TransferException(TransferException.Reason.CONNECT_FAILED, error)
        }
    }

    private suspend fun writePayload(
        input: InputStream,
        length: Long,
        target: File,
        onBytes: (Long, Long) -> Unit
    ) {
        val buffer = ByteArray(BUFFER)
        var received = 0L
        FileOutputStream(target).use { output ->
            val sink = BufferedOutputStream(output, BUFFER)
            while (received < length) {
                coroutineContext.ensureActive()
                val wanted = minOf(buffer.size.toLong(), length - received).toInt()
                val read = input.read(buffer, 0, wanted)
                if (read < 0) throw TransferException(TransferException.Reason.IO_ERROR)
                sink.write(buffer, 0, read)
                received += read
                onBytes(received, length)
            }
            sink.flush()
        }
    }

    private suspend fun importPayload(
        archive: File,
        resolveTarget: suspend (TransferProfile, ZipFile) -> File,
        onImport: (Int) -> Unit
    ): TransferResult = withContext(Dispatchers.IO) {
        ZipFile(archive).use { zip ->
            val manifest = zip.getEntry(MANIFEST)
            if (manifest == null) {
                if (zip.getEntry(BATCH_MANIFEST) != null) {
                    throw TransferException(TransferException.Reason.BATCH_UNSUPPORTED)
                }
                throw TransferException(TransferException.Reason.INVALID_ARCHIVE)
            }
            if (manifest.size > 64 * 1024) throw TransferException(TransferException.Reason.INVALID_ARCHIVE)
            val profile = parseProfile(zip.getInputStream(manifest).use { it.readBytes().decodeToString() })
            var remaining = TransferProtocol.maxArchiveBytes
            for (entry in zip.entries()) {
                if (entry.size < 0 || entry.size > remaining) {
                    throw TransferException(TransferException.Reason.INVALID_ARCHIVE)
                }
                remaining -= entry.size
            }
            val root = resolveTarget(profile, zip).canonicalFile
            if (!root.isDirectory && !root.mkdirs()) throw TransferException(TransferException.Reason.IO_ERROR)
            var files = 0
            var bytes = 0L
            zip.entries().asSequence().forEach { entry ->
                if (entry.name == MANIFEST || entry.isDirectory) return@forEach
                val relative = normalizeEntry(entry.name) ?: return@forEach
                if (!isAllowed(relative)) return@forEach
                coroutineContext.ensureActive()
                val destination = File(root, relative).canonicalFile
                if (!destination.path.startsWith(root.path + File.separator)) return@forEach
                destination.parentFile?.mkdirs()
                zip.getInputStream(entry).use { source ->
                    destination.outputStream().use { sink -> source.copyTo(sink, BUFFER) }
                }
                files++
                bytes += entry.size.coerceAtLeast(0)
                onImport(files)
            }
            TransferResult(profile, root, files, bytes)
        }
    }

    private fun parseProfile(text: String): TransferProfile {
        val root = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
            ?: throw TransferException(TransferException.Reason.INVALID_ARCHIVE)
        fun field(name: String): String? = runCatching { root.get(name)?.asString }.getOrNull()
        val name = field("Name")?.trim().orEmpty()
        if (name.isEmpty() || name.length > 150) throw TransferException(TransferException.Reason.INVALID_ARCHIVE)
        return TransferProfile(
            name = name,
            description = field("Description").orEmpty(),
            gameVersion = field("GameVersion").orEmpty(),
            loader = field("Loader").orEmpty(),
            loaderVersion = field("LoaderVersion")?.takeIf { it.isNotBlank() },
            hasRuntime = root.get("HasRuntime")?.let { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean && it.asBoolean } == true
        )
    }

    private fun normalizeEntry(name: String): String? {
        val cleaned = name.replace('\\', '/').trimStart('/')
        if (cleaned.isEmpty() || cleaned.endsWith("/")) return null
        val segments = cleaned.split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) return null
        return segments.joinToString(File.separator)
    }

    private fun isAllowed(relative: String): Boolean {
        val segments = relative.split(File.separator)
        if (segments.size == 1) return segments.first() in TransferProtocol.contentFiles
        return segments.first() in TransferProtocol.contentRoots
    }
}
