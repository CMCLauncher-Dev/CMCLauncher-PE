package com.cmc.launcher.game.transfer

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.cert.Certificate
import kotlin.coroutines.coroutineContext

internal object TransferProtocol {
    const val discoveryPort = 45837
    const val maxArchiveBytes = 64L * 1024 * 1024 * 1024
    val discoveryRequest = "CMCLauncher.InstanceTransfer.1?".encodeToByteArray()
    val connectionRequest = "CMC.PairRequest2".encodeToByteArray()
    val contentRoots = setOf(
        "saves", "mods", "config", "defaultconfigs", "scripts", "kubejs",
        "resourcepacks", "shaderpacks", "screenshots", "schematics"
    )
    val contentFiles = setOf("options.txt", "optionsof.txt", "optionsshaders.txt", "servers.dat")

    fun verification(certificate: Certificate, nonce: ByteArray): String {
        val sha = MessageDigest.getInstance("SHA-256")
        return sha.digest(sha.digest(certificate.encoded) + nonce)
            .take(6).joinToString("") { "%02X".format(it) }
    }

    fun interfaces(interfaceName: String? = null) = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        .filter { interfaceName == null || it.name == interfaceName }
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.interfaceAddresses }
        .filter { it.address is Inet4Address }

    suspend fun discover(
        interfaceName: String? = null,
        bindSocket: (DatagramSocket) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {
        val devices = sortedSetOf<String>()
        try {
            DatagramSocket().use { socket ->
                bindSocket(socket)
                socket.broadcast = true
                socket.soTimeout = 250
                val destinations = interfaces(interfaceName).mapNotNull { it.broadcast }.toMutableSet()
                destinations.add(InetAddress.getByName("255.255.255.255"))
                for (destination in destinations) {
                    runCatching {
                        socket.send(DatagramPacket(discoveryRequest, discoveryRequest.size, destination, discoveryPort))
                    }
                }
                val buffer = ByteArray(discoveryRequest.size + 3)
                val deadline = System.nanoTime() + 3_000_000_000L
                while (System.nanoTime() < deadline) {
                    coroutineContext.ensureActive()
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    if (packet.length != discoveryRequest.size + 2 ||
                        !buffer.copyOfRange(0, discoveryRequest.size).contentEquals(discoveryRequest)
                    ) continue
                    val port = ByteBuffer.wrap(buffer, discoveryRequest.size, 2).short.toInt() and 0xffff
                    if (port != 0) devices.add("${packet.address.hostAddress}:$port")
                }
            }
        } catch (_: IOException) {
            // Manual entry remains available when the network blocks broadcast.
        }
        devices.toList()
    }
}

internal suspend fun <T> Closeable.useTransferSocket(block: suspend () -> T): T = coroutineScope {
    val closer = launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            runCatching { close() }
        }
    }
    try {
        block()
    } finally {
        closer.cancel()
    }
}
