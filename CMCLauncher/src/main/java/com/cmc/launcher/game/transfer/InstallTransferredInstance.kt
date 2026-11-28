package com.cmc.launcher.game.transfer

import android.content.Context
import com.cmc.launcher.coroutine.TaskFlowExecutor
import com.cmc.launcher.game.addons.modloader.ModLoader
import com.cmc.launcher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.cmc.launcher.game.download.game.GameDownloadInfo
import com.cmc.launcher.game.download.game.GameInstaller
import com.cmc.launcher.game.download.modpack.install.retrieveLoader
import com.cmc.launcher.game.path.getGameHome
import com.cmc.launcher.game.path.getVersionsHome
import com.cmc.launcher.game.version.installed.VersionConfig
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.keepalive.TaskKeepAlive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files
import javax.net.SocketFactory

private val versionIdPattern = Regex("[A-Za-z0-9][A-Za-z0-9._+-]{0,99}")

suspend fun receiveTransferredInstance(
    context: Context,
    address: String,
    target: File?,
    onBytes: (Long, Long) -> Unit,
    onImport: (Int) -> Unit,
    onAwaitingApproval: (String) -> Unit,
    onInstalling: (String) -> Unit,
    socketFactory: SocketFactory = SocketFactory.getDefault()
): TransferResult = coroutineScope {
    if (target != null) {
        return@coroutineScope InstanceTransferReceiver.receive(
            address, target, onBytes, onImport, onAwaitingApproval, socketFactory
        )
    }

    val gameHome = File(getGameHome())
    var created: File? = null
    var imported = false
    TaskKeepAlive.acquire()
    try {
        val result = InstanceTransferReceiver.receiveArchive(
            address = address,
            resolveTarget = { profile, archive ->
                onInstalling(profile.gameVersion)
                val versions = File(getVersionsHome(gameHome.absolutePath))
                val destination = reserveInstanceDirectory(versions, profile.name)
                created = destination
                try {
                    if (profile.hasRuntime) {
                        installDesktopRuntime(archive, profile, gameHome, destination)
                    } else {
                        val info = profile.downloadInfo(destination.name)
                        val work = Files.createTempDirectory(context.cacheDir.toPath(), "transfer-install").toFile()
                        try {
                            val installer = GameInstaller(
                                context = context,
                                info = info,
                                scope = this@coroutineScope,
                                targetGameFolder = gameHome,
                                workDirectory = work
                            )
                            val executor = TaskFlowExecutor(this@coroutineScope)
                            executor.addPhases(installer.getTaskPhase(createIsolation = false))
                            executor.executePhases(
                                onError = { throw it },
                                onCancel = { throw CancellationException("Instance installation cancelled") }
                            )
                            check(File(destination, "${destination.name}.json").isFile)
                            check(File(destination, "${destination.name}.jar").isFile)
                        } finally {
                            FileUtils.deleteQuietly(work)
                        }
                    }
                    VersionConfig.createIsolation(destination).saveWithThrowable()
                    check(VersionsManager.loadVersion(gameHome.absolutePath, destination.name)?.isValid() == true) {
                        "The imported version manifest could not be loaded."
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: TransferException) {
                    throw failure
                } catch (failure: Exception) {
                    throw TransferException(TransferException.Reason.INSTALL_FAILED, failure)
                }
                destination
            },
            onBytes = onBytes,
            onImport = onImport,
            onAwaitingApproval = onAwaitingApproval,
            socketFactory = socketFactory
        )
        imported = true
        if (getGameHome() == gameHome.path) {
            VersionsManager.refresh("InstanceTransfer", result.target.name)
        }
        result
    } finally {
        if (!imported) withContext(NonCancellable + Dispatchers.IO) {
            created?.let { FileUtils.deleteQuietly(it) }
        }
        TaskKeepAlive.release()
    }
}

private fun reserveInstanceDirectory(versions: File, name: String): File {
    if (!versions.isDirectory && !versions.mkdirs() && !versions.isDirectory) {
        throw TransferException(TransferException.Reason.IO_ERROR)
    }
    val safeName = name.map { character ->
        if (character.isISOControl() || character == '\\' || character in "/:*?\"<>|") '_' else character
    }.joinToString("").take(60).trim().trim('.').ifBlank { "Imported instance" }
    for (suffix in 1..1000) {
        val destination = File(versions, if (suffix == 1) safeName else "$safeName ($suffix)")
        if (destination.mkdir()) return destination
        if (!destination.exists()) throw TransferException(TransferException.Reason.IO_ERROR)
    }
    throw TransferException(TransferException.Reason.IO_ERROR)
}

private suspend fun TransferProfile.downloadInfo(name: String): GameDownloadInfo {
    fun unavailable(): Nothing = throw TransferException(TransferException.Reason.VERSION_UNSUPPORTED)
    if (!versionIdPattern.matches(gameVersion)) unavailable()
    val version = loaderVersion
    if (version != null && !versionIdPattern.matches(version)) unavailable()
    val info = GameDownloadInfo(gameVersion, name)
    if (loader.isBlank() || loader.equals("vanilla", ignoreCase = true)) return info

    val type = when (loader.lowercase()) {
        "fabric" -> ModLoader.FABRIC
        "forge" -> ModLoader.FORGE
        "neoforge" -> ModLoader.NEOFORGE
        "quilt" -> ModLoader.QUILT
        else -> unavailable()
    }
    if (type == ModLoader.FABRIC && version == null) {
        val available = FabricVersions.fetchFabricLoaderList(gameVersion).orEmpty()
        val selected = available.firstOrNull { it.stable } ?: available.firstOrNull() ?: unavailable()
        return info.copy(fabric = selected)
    }

    var resolved: GameDownloadInfo? = null
    (type to (version ?: unavailable())).retrieveLoader(gameVersion, info) { resolved = it }
    return resolved ?: unavailable()
}
