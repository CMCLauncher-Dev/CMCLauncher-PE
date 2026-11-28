package com.cmc.launcher.game.transfer

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext

private const val runtimeFolder = ".cmc-transfer-runtime/"
private val runtimeId = Regex("[A-Za-z0-9][A-Za-z0-9._+-]{0,149}")
private val sha1 = Regex("[a-fA-F0-9]{40}")
private val assetObject = Regex("assets/objects/([a-f0-9]{2})/([a-f0-9]{40})")

internal suspend fun installDesktopRuntime(
    archive: ZipFile,
    profile: TransferProfile,
    gameHome: File,
    destination: File
) {
    val metadata = archive.getEntry(runtimeFolder + "profile.json") ?: invalidRuntime()
    if (metadata.size !in 1..4_194_304) invalidRuntime()
    val manifest = archive.getInputStream(metadata).use { input ->
        val bytes = ByteArray(metadata.size.toInt())
        java.io.DataInputStream(input).readFully(bytes)
        if (input.read() != -1) invalidRuntime()
        runCatching { JsonParser.parseString(bytes.decodeToString()).asJsonObject }
            .getOrElse { invalidRuntime() }
    }
    if (manifest.string("mainClass").isNullOrBlank()) invalidRuntime()
    if (!runtimeId.matches(profile.gameVersion) ||
        profile.loader.lowercase() !in setOf("vanilla", "fabric", "forge", "neoforge")
    ) invalidRuntime()
    val jarId = manifest.string("jar") ?: invalidRuntime()
    if (jarId.length > 150 || jarId.contains('/') || !validRelativePath(jarId)) invalidRuntime()

    // Only seed shared cache entries described by the manifest and verified by hash.
    val shared = mutableMapOf<String, String>()
    fun track(path: String, hash: String?) {
        if (!validRelativePath(path)) invalidRuntime()
        if (hash != null && sha1.matches(hash)) shared[path] = hash
    }
    val libraries = manifest.get("libraries")
    if (libraries != null && !libraries.isJsonArray) invalidRuntime()
    for (library in (libraries?.asJsonArray ?: JsonArray())) {
        if (!library.isJsonObject) invalidRuntime()
        val model = library.asJsonObject
        val name = model.string("name") ?: invalidRuntime()
        if (!name.split(':').all { it.matches(Regex("[A-Za-z0-9._+@-]+")) && it != "." && it != ".." }) {
            invalidRuntime()
        }
        val downloads = model.objectField("downloads")
        downloads?.objectField("artifact")?.let { artifact ->
            artifact.string("path")?.let { track("libraries/$it", artifact.string("sha1")) }
        }
        downloads?.objectField("classifiers")?.entrySet()?.forEach { (_, value) ->
            if (!value.isJsonObject) invalidRuntime()
            value.asJsonObject.string("path")?.let { track("libraries/$it", value.asJsonObject.string("sha1")) }
        }
    }
    manifest.objectField("assetIndex")?.let { index ->
        val id = index.string("id") ?: invalidRuntime()
        if (!runtimeId.matches(id)) invalidRuntime()
        track("assets/indexes/$id.json", index.string("sha1"))
    }
    manifest.objectField("logging")?.objectField("client")?.objectField("file")?.let { log ->
        val id = log.string("id") ?: invalidRuntime()
        if (!runtimeId.matches(id)) invalidRuntime()
        track("assets/log_configs/$id", log.string("sha1"))
    }

    val jar = archive.getEntry("${runtimeFolder}versions/$jarId/$jarId.jar") ?: invalidRuntime()
    val clientHash = manifest.objectField("downloads")?.objectField("client")?.string("sha1")
    archive.copyRuntimeEntry(jar, File(destination, "${destination.name}.jar"), clientHash)
    val cacheRoot = gameHome.canonicalFile
    for (entry in archive.entries()) {
        if (entry.isDirectory || !entry.name.startsWith(runtimeFolder)) continue
        val relative = entry.name.removePrefix(runtimeFolder)
        val objectHash = assetObject.matchEntire(relative)?.let { match ->
            match.groupValues[2].takeIf { it.startsWith(match.groupValues[1]) }
        }
        val hash = shared[relative] ?: objectHash ?: continue
        val file = File(cacheRoot, relative).canonicalFile
        if (!file.path.startsWith(cacheRoot.path + File.separator)) invalidRuntime()
        if (!file.exists()) archive.copyRuntimeEntry(entry, file, hash)
    }
    manifest.remove("jar")
    manifest.remove("inheritsFrom")
    manifest.addProperty("id", destination.name)
    manifest.addProperty("clientVersion", profile.gameVersion)
    File(destination, "${destination.name}.json").writeText(manifest.toString())
}

private fun validRelativePath(path: String): Boolean =
    !path.contains('\\') && !path.contains(':') && path.split('/').all { segment ->
        segment.isNotBlank() && segment != "." && segment != ".." && segment.none(Char::isISOControl)
    }

private fun JsonObject.objectField(name: String): JsonObject? =
    get(name)?.takeUnless(JsonElement::isJsonNull)?.let { if (it.isJsonObject) it.asJsonObject else invalidRuntime() }

private fun JsonObject.string(name: String): String? =
    get(name)?.takeUnless(JsonElement::isJsonNull)?.let { if (it.isJsonPrimitive && it.asJsonPrimitive.isString) it.asString else invalidRuntime() }

private fun invalidRuntime(): Nothing = throw TransferException(TransferException.Reason.INVALID_ARCHIVE)

private suspend fun ZipFile.copyRuntimeEntry(entry: ZipEntry, target: File, expectedHash: String?) {
    if (entry.isDirectory || entry.size <= 0) invalidRuntime()
    target.parentFile?.mkdirs()
    val temporary = File.createTempFile(".cmc-runtime-", ".part", target.parentFile)
    try {
        val digest = MessageDigest.getInstance("SHA-1")
        getInputStream(entry).use { source ->
            temporary.outputStream().buffered().use { sink ->
                var remaining = entry.size
                val buffer = ByteArray(64 * 1024)
                while (remaining > 0) {
                    coroutineContext.ensureActive()
                    val count = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (count < 0) invalidRuntime()
                    digest.update(buffer, 0, count)
                    sink.write(buffer, 0, count)
                    remaining -= count
                }
                if (source.read() != -1) invalidRuntime()
            }
        }
        if (expectedHash != null && !digest.digest().joinToString("") { "%02x".format(it) }.equals(expectedHash, ignoreCase = true)) {
            invalidRuntime()
        }
        try {
            Files.move(temporary.toPath(), target.toPath())
        } catch (_: FileAlreadyExistsException) {
            // Another installation may have populated the shared cache.
        }
    } finally {
        temporary.delete()
    }
}
