package com.cmc.launcher.game.transfer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.DataInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlin.concurrent.thread

class InstanceTransferReceiverTest {

    companion object {
        private lateinit var certificate: X509Certificate
        private lateinit var privateKey: PrivateKey
        private var ready = false

        @JvmStatic
        @BeforeClass
        fun generateCertificate() {
            val directory = Files.createTempDirectory("transfer-cert").toFile()
            val keyFile = File(directory, "key.pem")
            val certFile = File(directory, "cert.pem")
            val process = runCatching {
                ProcessBuilder(
                    "openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "2",
                    "-subj", "/CN=Instance Transfer Test",
                    "-keyout", keyFile.absolutePath, "-out", certFile.absolutePath
                ).redirectErrorStream(true).start()
            }.getOrNull() ?: return
            if (!process.waitFor(40, TimeUnit.SECONDS) || process.exitValue() != 0) return
            val keyText = keyFile.readText()
                .replace(Regex("-----[^-]+-----"), "")
                .replace(Regex("\\s"), "")
            privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyText)))
            certificate = certFile.inputStream().use {
                CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            }
            ready = true
        }
    }

    @Test(timeout = 60000)
    fun receiveImportsWhitelistedContent() {
        assumeTrue(ready)
        val archive = archive(
            manifest = profile("Transfer Test"),
            entries = linkedMapOf(
                "saves/World/level.dat" to "level",
                "mods/example.jar" to "mod",
                "options.txt" to "lang:zh_cn",
                "unrelated/secret.txt" to "skip",
                "../escape.txt" to "skip"
            )
        )
        val sender = startSender(archive)
        val target = Files.createTempDirectory("transfer-target").toFile()
        val result = runBlocking {
            InstanceTransferReceiver.receive("127.0.0.1:${sender.port}", target)
        }
        sender.stop()
        assertEquals("Transfer Test", result.profile.name)
        assertEquals(3, result.files)
        assertTrue(File(target, "saves/World/level.dat").isFile)
        assertTrue(File(target, "mods/example.jar").isFile)
        assertTrue(File(target, "options.txt").isFile)
        assertFalse(File(target, "unrelated").exists())
        assertFalse(File(target.parentFile, "escape.txt").exists())
    }

    @Test(timeout = 60000)
    fun requestsDesktopApprovalAndConfirmsImport() {
        assumeTrue(ready)
        val expectedVerification = CompletableFuture<String>()
        val receipt = CompletableFuture<Int>()
        val payload = archive(manifest = profile("Desktop"), entries = mapOf("options.txt" to "lang:zh_cn"))
        val parent = Files.createTempDirectory("transfer-approval").toFile()
        val target = File(parent, "Desktop")
        val sender = startSender(
            archive = payload,
            approve = { nonce ->
                val digest = MessageDigest.getInstance("SHA-256")
                expectedVerification.complete(hex(digest.digest(digest.digest(certificate.encoded) + nonce).copyOf(6)))
            },
            onReceipt = { receipt.complete(it) }
        )
        try {
            val result = runBlocking {
                InstanceTransferReceiver.receive(
                    address = "127.0.0.1:${sender.port}",
                    resolveTarget = { profile ->
                        assertEquals("Desktop", profile.name)
                        assertEquals("1.20.1", profile.gameVersion)
                        assertEquals("fabric", profile.loader)
                        assertEquals("0.15.0", profile.loaderVersion)
                        assertFalse(target.exists())
                        target
                    },
                    onAwaitingApproval = { actual ->
                        assertEquals(expectedVerification.get(5, TimeUnit.SECONDS), actual)
                    }
                )
            }
            assertEquals(1, receipt.get(5, TimeUnit.SECONDS))
            assertEquals("Desktop", result.profile.name)
            assertEquals("lang:zh_cn", File(target, "options.txt").readText())
        } finally {
            sender.stop()
            parent.deleteRecursively()
        }
    }

    @Test(timeout = 60000)
    fun importsDesktopRuntimeAndOnlySeedsVerifiedCacheFiles() {
        assumeTrue(ready)
        val bytes = "test-client".encodeToByteArray()
        val hash = hex(MessageDigest.getInstance("SHA-1").digest(bytes)).lowercase()
        val asset = "assets/objects/${hash.take(2)}/$hash"
        val payload = archive(
            manifest = profile("Desktop Runtime").replace("}", ""","HasRuntime":true}"""),
            entries = mapOf(
                ".cmc-transfer-runtime/profile.json" to """{"id":"desktop","jar":"1.20.1","mainClass":"net.minecraft.client.main.Main","libraries":[]}""",
                ".cmc-transfer-runtime/versions/1.20.1/1.20.1.jar" to bytes.decodeToString(),
                ".cmc-transfer-runtime/$asset" to bytes.decodeToString(),
                ".cmc-transfer-runtime/libraries/unlisted/unlisted.jar" to "unlisted",
                "options.txt" to "lang:zh_cn"
            )
        )
        val home = Files.createTempDirectory("desktop-runtime").toFile()
        val destination = File(home, "versions/Imported").apply { mkdirs() }
        val sender = startSender(payload)
        try {
            val result = runBlocking {
                InstanceTransferReceiver.receiveArchive(
                    address = "127.0.0.1:${sender.port}",
                    resolveTarget = { received, zip ->
                        assertTrue(received.hasRuntime)
                        installDesktopRuntime(zip, received.copy(loader = "vanilla"), home, destination)
                        destination
                    },
                    onBytes = { _, _ -> },
                    onImport = {},
                    onAwaitingApproval = {}
                )
            }
            assertEquals(1, result.files)
            assertEquals(bytes.decodeToString(), File(destination, "Imported.jar").readText())
            val manifest = com.google.gson.JsonParser.parseString(File(destination, "Imported.json").readText()).asJsonObject
            assertEquals("Imported", manifest["id"].asString)
            assertEquals("1.20.1", manifest["clientVersion"].asString)
            assertFalse(manifest.has("jar"))
            assertEquals(bytes.decodeToString(), File(home, asset).readText())
            assertFalse(File(home, "libraries/unlisted/unlisted.jar").exists())
            assertEquals("lang:zh_cn", File(destination, "options.txt").readText())
        } finally {
            sender.stop()
            home.deleteRecursively()
        }
    }

    @Test(timeout = 60000)
    fun rejectsBatchArchive() {
        assumeTrue(ready)
        val archive = archive(
            manifest = null,
            batch = "[${profile("First")},${profile("Second")}]",
            entries = mapOf("saves/a.dat" to "a")
        )
        val sender = startSender(archive)
        val target = Files.createTempDirectory("transfer-batch").toFile()
        val failure = runCatching {
            runBlocking {
                InstanceTransferReceiver.receive("127.0.0.1:${sender.port}", target)
            }
        }.exceptionOrNull()
        sender.stop()
        assertNotNull(failure)
        assertEquals(
            TransferException.Reason.BATCH_UNSUPPORTED,
            (failure as TransferException).reason
        )
    }

    @Test(timeout = 60000)
    fun rejectsUnapprovedConnection() {
        assumeTrue(ready)
        val sender = startSender(archive(profile("Declined"), emptyMap()), approved = false)
        val target = Files.createTempDirectory("transfer-declined").toFile()
        try {
            val failure = runCatching {
                runBlocking { InstanceTransferReceiver.receive("127.0.0.1:${sender.port}", target) }
            }.exceptionOrNull()
            assertEquals(TransferException.Reason.PAIRING_REJECTED, (failure as TransferException).reason)
            assertTrue(target.listFiles().orEmpty().isEmpty())
        } finally {
            sender.stop()
            target.deleteRecursively()
        }
    }

    @Test(timeout = 180000)
    fun importsFromDesktopSender() {
        val address = System.getenv("CMC_INTEROP_ADDR").orEmpty()
        assumeTrue(address.isNotBlank())
        val target = Files.createTempDirectory("transfer-interop").toFile()
        val result = runBlocking {
            InstanceTransferReceiver.receive(address, target)
        }
        assertEquals("Interop", result.profile.name)
        assertTrue(File(target, "saves/interop/level.dat").isFile)
        assertTrue(File(target, "mods/interop.jar").isFile)
        assertTrue(File(target, "options.txt").isFile)
    }

    @Test
    fun parsesAddress() {
        assertEquals("192.168.1.2" to 45838, InstanceTransferReceiver.splitAddress(" 192.168.1.2:45838 "))
        assertEquals("fe80::1" to 9000, InstanceTransferReceiver.splitAddress("[fe80::1]:9000"))
        assertEquals(null, InstanceTransferReceiver.splitAddress("192.168.1.2"))
    }

    private class Sender(val port: Int, private val server: ServerSocket, private val worker: Thread) {
        fun stop() {
            runCatching { server.close() }
            worker.join(5000)
        }
    }

    private fun startSender(
        archive: ByteArray,
        approved: Boolean = true,
        approve: (ByteArray) -> Unit = {},
        onReceipt: (Int) -> Unit = {}
    ): Sender {
        val store = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry("sender", privateKey, PASSWORD, arrayOf(certificate))
        }
        val managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        managers.init(store, PASSWORD)
        val context = SSLContext.getInstance("TLS")
        context.init(managers.keyManagers, null, SecureRandom())
        val server = context.serverSocketFactory.createServerSocket(0) as SSLServerSocket
        val worker = thread(isDaemon = true, name = "transfer-sender") {
            runCatching {
                server.accept().use accept@{ raw ->
                    (raw as SSLSocket).use socket@{ socket ->
                        socket.startHandshake()
                        val input = DataInputStream(socket.inputStream)
                        val expected = "CMC.PairRequest2".encodeToByteArray()
                        val request = ByteArray(expected.size).also(input::readFully)
                        if (!request.contentEquals(expected)) return@socket
                        val nonce = ByteArray(32).also(input::readFully)
                        approve(nonce)
                        val output = socket.outputStream
                        output.write(if (approved) 1 else 0)
                        output.flush()
                        if (!approved) return@socket
                        output.write(ByteBuffer.allocate(8).putLong(archive.size.toLong()).array())
                        output.write(archive)
                        output.flush()
                        onReceipt(input.read())
                    }
                }
            }
            runCatching { server.close() }
        }
        return Sender(server.localPort, server, worker)
    }

    private fun profile(name: String): String =
        """{"Name":"$name","Description":"test","GameVersion":"1.20.1","Loader":"fabric","LoaderVersion":"0.15.0"}"""

    private fun archive(
        manifest: String?,
        entries: Map<String, String>,
        batch: String? = null
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            if (batch != null) {
                zip.putNextEntry(ZipEntry("instances.json"))
                zip.write(batch.toByteArray())
                zip.closeEntry()
            } else if (manifest != null) {
                zip.putNextEntry(ZipEntry("instance.json"))
                zip.write(manifest.toByteArray())
                zip.closeEntry()
            }
            entries.forEach { (name, body) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray())
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02X".format(it) }

    private val PASSWORD = "changeit".toCharArray()
}