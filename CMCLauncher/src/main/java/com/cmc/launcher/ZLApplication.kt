package com.cmc.launcher

import android.app.Application
import android.content.res.Configuration
import android.os.Process
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.kyant.fishnet.Fishnet
import com.cmc.launcher.context.GlobalContext
import com.cmc.launcher.context.refreshContext
import com.cmc.launcher.coroutine.TaskSystem
import com.cmc.launcher.keepalive.TaskKeepAlive
import com.cmc.launcher.path.IMAGE_LOADER_CLIENT
import com.cmc.launcher.path.PathManager
import com.cmc.launcher.setting.loadAllSettings
import com.cmc.launcher.ui.activities.showFatalError
import com.cmc.launcher.ui.activities.showLauncherCrash
import com.cmc.launcher.utils.device.Architecture
import com.cmc.launcher.utils.logging.Logger
import com.cmc.launcher.utils.network.TLSSecurity
import com.cmc.launcher.utils.writeCrashFile
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import kotlin.properties.Delegates

@HiltAndroidApp
class ZLApplication : Application(), SingletonImageLoader.Factory {
    companion object {
        @JvmStatic
        var DEVICE_ARCHITECTURE by Delegates.notNull<Int>()
    }

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        val startupClock = android.os.SystemClock.elapsedRealtime()
        fun mark(label: String) {
            android.util.Log.i("StartupTiming", label + "=" + (android.os.SystemClock.elapsedRealtime() - startupClock))
        }
        GlobalContext = this
        TLSSecurity.install()
        mark("tls")
        refreshContext(this)
        mark("context")

        TaskKeepAlive.initialize(this)
        mark("keepalive")

        Thread.setDefaultUncaughtExceptionHandler { _, th ->

            TaskSystem.stopAll()

            val throwable = if (th is SplashException) th.cause!!
            else th

            Logger.error("Startup", "An exception occurred", throwable)

            writeCrashFile(
                file = PathManager.FILE_CRASH_REPORT,
                throwable = throwable
            ) { t ->
                Logger.error("AppCrash", "An exception occurred while saving the crash report", t)
            }

            showLauncherCrash(this@ZLApplication, throwable, th !is SplashException)
            Process.killProcess(Process.myPid())
        }

        super.onCreate()
        mark("super")
        com.cmc.launcher.ui.theme.installStaticMotion()
        mark("staticMotion")
        runCatching {
            MMKV.initialize(this)
            mark("mmkv")
            loadAllSettings(this)
            mark("settings")
            com.cmc.launcher.setting.enums.applyLanguage(com.cmc.launcher.setting.AllSettings.launcherLanguage.state)
            mark("language")

            Logger.initialize(this)
            mark("logger")

            PathManager.DIR_FILES_PRIVATE = getDir("files", MODE_PRIVATE)
            DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture()
            mark("paths")

            if (Architecture.isx86Device() && Architecture.is32BitsDevice) {
                val originalJNIDirectory = applicationInfo.nativeLibraryDir
                applicationInfo.nativeLibraryDir = originalJNIDirectory.take(originalJNIDirectory.lastIndexOf("/")) + "/x86"
            }
        }.onFailure { launchTh ->
            writeCrashFile(
                file = PathManager.FILE_CRASH_REPORT,
                throwable = launchTh
            ) {
                Log.w("ZLApplication", "An exception occurred while saving the crash report", it)
            }
            showFatalError(this, launchTh)
        }

        scheduleDeferredStartup()
        mark("deferred")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshContext(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(20L * 1024 * 1024)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .maxSizeBytes(512L * 1024 * 1024)
                    .directory(PathManager.DIR_IMAGE_CACHE.toOkioPath())
                    .build()
            }
            .components {
                add(KtorNetworkFetcherFactory(IMAGE_LOADER_CLIENT))
                add(SvgDecoder.Factory())
            }
            .crossfade(false)
            .build()
    }

    private fun scheduleDeferredStartup() {
        startupScope.launch {
            runCatching {
                Fishnet.init(this@ZLApplication, PathManager.DIR_NATIVE_LOGS.absolutePath)
            }.onFailure { error ->
                Logger.warning("Startup", "Deferred initialization failed", error)
            }
        }
    }
}
