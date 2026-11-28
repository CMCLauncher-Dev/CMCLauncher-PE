package com.cmc.launcher.game.transfer

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.cmc.launcher.game.addons.modloader.ModLoader
import com.cmc.launcher.game.version.installed.Version
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedKeyManager
import javax.security.auth.x500.X500Principal
import kotlin.coroutines.coroutineContext

internal object InstanceTransferSender {
    suspend fun send(
        context: Context,
        version: Version,
        onReady: (List<String>) -> Unit,
        confirm: suspend (String, String) -> Boolean,
        onProgress: (Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val info = version.getVersionInfo() ?: throw TransferException(TransferException.Reason.VERSION_UNSUPPORTED)
        val loader = info.primaryLoader
        val loaderName = when (loader?.loader) {
            null -> "vanilla"
            ModLoader.FABRIC -> "fabric"
            ModLoader.FORGE -> "forge"
            ModLoader.NEOFORGE -> "neoforge"
            else -> throw TransferException(TransferException.Reason.VERSION_UNSUPPORTED)
        }
        val alias = "cmcl-transfer-${UUID.randomUUID()}"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val archive = File.createTempFile("cmcl-send", ".zip", context.cacheDir)
        try {
            val manifest = JsonObject().apply {
                val cleaned = version.getVersionName()
                    .replace(Regex("""[/\\:*?"<>|\p{Cc}]"""), "_")
                    .take(150).trim().trimEnd('.').ifBlank { "Minecraft ${info.minecraftVersion}" }
                val reserved = Regex("""(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\..*)?""")
                addProperty("Name", if (reserved.matches(cleaned)) "_$cleaned" else cleaned)
                addProperty("Description", version.getVersionSummary())
                addProperty("GameVersion", info.minecraftVersion)
                addProperty("Loader", loaderName)
                loader?.version?.takeIf { it.isNotBlank() }?.let { addProperty("LoaderVersion", it) }
            }
            pack(version.getGameDir().canonicalFile, manifest, archive)
            val (tls, certificate) = createServerIdentity(alias, store)
            val server = tls.serverSocketFactory.createServerSocket(0) as SSLServerSocket
            server.useTransferSocket {
                val port = server.localPort
                coroutineScope {
                    val discovery = launch { advertise(port) }
                    try {
                        val addresses = TransferProtocol.interfaces().map { "${it.address.hostAddress}:$port" }.distinct()
                        onReady(addresses)
                        while (true) {
                            coroutineContext.ensureActive()
                            val socket = server.accept() as SSLSocket
                            if (socket.useTransferSocket {
                                sendTo(socket, archive, certificate, confirm, onProgress)
                            }) break
                        }
                    } finally {
                        discovery.cancelAndJoin()
                    }
                }
            }
        } finally {
            archive.delete()
            runCatching { store.deleteEntry(alias) }
        }
    }

    private suspend fun sendTo(
        socket: SSLSocket,
        archive: File,
        certificate: X509Certificate,
        confirm: suspend (String, String) -> Boolean,
        onProgress: (Long, Long) -> Unit
    ): Boolean {
        val input = DataInputStream(socket.inputStream)
        val output = DataOutputStream(socket.outputStream)
        try {
            socket.soTimeout = 5000
            socket.startHandshake()
            val request = ByteArray(TransferProtocol.connectionRequest.size)
            input.readFully(request)
            if (!request.contentEquals(TransferProtocol.connectionRequest)) return false
            val nonce = ByteArray(32)
            input.readFully(nonce)
            val verification = TransferProtocol.verification(certificate, nonce)
            val approved = withTimeoutOrNull(60_000) {
                confirm(socket.inetAddress.hostAddress.orEmpty(), verification)
            } == true
            output.writeByte(if (approved) 1 else 0)
            output.flush()
            if (!approved) return false
        } catch (_: IOException) {
            coroutineContext.ensureActive()
            return false
        }
        val size = archive.length()
        output.writeLong(size)
        onProgress(0, size)
        archive.inputStream().use { source ->
            val buffer = ByteArray(64 * 1024)
            var sent = 0L
            while (true) {
                coroutineContext.ensureActive()
                val count = source.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                sent += count
                onProgress(sent, size)
            }
        }
        output.flush()
        socket.soTimeout = 30 * 60 * 1000
        if (input.readUnsignedByte() != 1) throw IOException("The receiving device could not import the instance.")
        return true
    }

    private suspend fun pack(root: File, manifest: JsonObject, archive: File) {
        ZipOutputStream(archive.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("instance.json"))
            zip.write(manifest.toString().encodeToByteArray())
            zip.closeEntry()
            val buffer = ByteArray(64 * 1024)
            var packed = 0L
            for (name in TransferProtocol.contentRoots + TransferProtocol.contentFiles) {
                val path = root.toPath().resolve(name)
                if (!Files.exists(path)) continue
                Files.walk(path).use { files ->
                    for (file in files.iterator()) {
                        coroutineContext.ensureActive()
                        if (!Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) continue
                        val relative = root.toPath().relativize(file).joinToString("/")
                        zip.putNextEntry(ZipEntry(relative))
                        Files.newInputStream(file, java.nio.file.LinkOption.NOFOLLOW_LINKS).use { input ->
                            while (true) {
                                coroutineContext.ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                packed += count
                                if (packed > TransferProtocol.maxArchiveBytes) throw IOException("Instance is too large.")
                                zip.write(buffer, 0, count)
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
        }
        if (archive.length() > TransferProtocol.maxArchiveBytes) throw IOException("Archive is too large.")
    }

    private suspend fun advertise(port: Int) {
        try {
            DatagramSocket(TransferProtocol.discoveryPort).use { socket ->
                socket.useTransferSocket {
                    val request = TransferProtocol.discoveryRequest
                    val response = ByteBuffer.allocate(request.size + 2).put(request).putShort(port.toShort()).array()
                    val buffer = ByteArray(request.size + 1)
                    while (true) {
                        coroutineContext.ensureActive()
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        if (packet.length == request.size && buffer.copyOf(request.size).contentEquals(request)) {
                            socket.send(DatagramPacket(response, response.size, packet.socketAddress))
                        }
                    }
                }
            }
        } catch (_: IOException) {
            coroutineContext.ensureActive()
        }
    }

    private fun createServerIdentity(alias: String, store: KeyStore): Pair<SSLContext, X509Certificate> {
        val keyPair = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").run {
            initialize(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                    .setKeySize(2048)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                    .setCertificateSubject(X500Principal("CN=CMCLauncher Instance Transfer"))
                    .build()
            )
            generateKeyPair()
        }
        val certificate = store.getCertificate(alias) as X509Certificate
        val manager = object : X509ExtendedKeyManager() {
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = null
            override fun chooseClientAlias(keyTypes: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = null
            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?) =
                if (keyType == "RSA") arrayOf(alias) else null
            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?) =
                getServerAliases(keyType, issuers)?.first()
            override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?) =
                getServerAliases(keyType, issuers)?.first()
            override fun getCertificateChain(requestedAlias: String?) =
                if (requestedAlias == alias) arrayOf(certificate) else null
            override fun getPrivateKey(requestedAlias: String?): PrivateKey? =
                if (requestedAlias == alias) keyPair.private else null
        }
        val tls = SSLContext.getInstance("TLS").apply { init(arrayOf(manager), null, null) }
        return tls to certificate
    }
}
