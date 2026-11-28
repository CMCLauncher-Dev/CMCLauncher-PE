package com.cmc.launcher.game.download.engine

import com.cmc.launcher.setting.AllSettings

object DownloadThrottle {

    private var nextAvailableNanos = 0L

    @Synchronized
    fun acquire(bytes: Long) {
        val kbps = AllSettings.downloadSpeedLimitKbps.state
        if (kbps <= 0 || bytes <= 0L) return
        val now = System.nanoTime()
        val start = maxOf(now, nextAvailableNanos)
        nextAvailableNanos = start + bytes * 1_000_000_000L / (kbps.toLong() * 1024L)
        val waitMillis = (nextAvailableNanos - now) / 1_000_000L
        if (waitMillis > 0L) {
            Thread.sleep(waitMillis)
        }
    }
}