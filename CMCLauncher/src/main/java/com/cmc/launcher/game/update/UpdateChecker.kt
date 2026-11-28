package com.cmc.launcher.game.update

import com.cmc.launcher.setting.enums.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

data class UpdateRelease(
    val tag: String,
    val name: String,
    val pageUrl: String,
    val apkUrl: String?,
    val prerelease: Boolean,
    val publishedAt: Long
)

object UpdateChecker {

    private const val RELEASES_URL = "https://api.github.com/repos/CMCLauncher-Dev/CMCLauncher-PE/releases?per_page=20"

    suspend fun latest(channel: UpdateChannel): UpdateRelease? = withContext(Dispatchers.IO) {
        val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "CMCLauncher")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val releases = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
            for (index in 0 until releases.length()) {
                val release = releases.getJSONObject(index)
                if (release.optBoolean("draft")) continue
                val prerelease = release.optBoolean("prerelease")
                if (channel == UpdateChannel.STABLE && prerelease) continue
                return@withContext parse(release, prerelease)
            }
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(release: JSONObject, prerelease: Boolean): UpdateRelease {
        val assets = release.optJSONArray("assets") ?: JSONArray()
        var apkUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            if (asset.optString("name").endsWith(".apk")) {
                apkUrl = asset.optString("browser_download_url")
                break
            }
        }
        return UpdateRelease(
            tag = release.optString("tag_name"),
            name = release.optString("name").ifBlank { release.optString("tag_name") },
            pageUrl = release.optString("html_url"),
            apkUrl = apkUrl,
            prerelease = prerelease,
            publishedAt = runCatching {
                Instant.parse(release.optString("published_at")).toEpochMilli()
            }.getOrDefault(0L)
        )
    }
}