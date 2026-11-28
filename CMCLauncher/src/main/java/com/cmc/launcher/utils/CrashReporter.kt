package com.cmc.launcher.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.cmc.launcher.path.PathManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object CrashReporter {

    private const val MAX_REPORTS = 5

    private const val MAX_ENTRY_BYTES = 2 * 1024 * 1024

    private val sensitiveLine = Regex(
        "(?i)(access[_-]?token|refresh[_-]?token|authorization|password|secret|session)\\s*[=:]"
    )

    private val timeFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT)

    fun directory(context: Context): File {
        val dir = File(PathManager.DIR_LAUNCHER_LOGS, "crash_reports")
        dir.mkdirs()
        return dir
    }

    fun record(context: Context, throwable: Throwable): File? {
        return runCatching {
            val dir = directory(context)
            val file = File(dir, "crash-${timeFormat.format(Date())}.log")
            val trace = StringWriter()
            throwable.printStackTrace(PrintWriter(trace))
            file.writeText(deviceSummary(context) + "\n\n" + trace.toString())
            trim(dir)
            file
        }.getOrNull()
    }

    fun createBundle(context: Context): File? {
        return runCatching {
            val output = File(PathManager.DIR_LAUNCHER_LOGS, "diagnostics-${timeFormat.format(Date())}.zip")
            ZipOutputStream(output.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("device.txt"))
                zip.write(deviceSummary(context).toByteArray())
                zip.closeEntry()

                val reports = directory(context).listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()
                reports.take(MAX_REPORTS).forEach { report ->
                    writeSanitized(zip, "crash_reports/${report.name}", report)
                }

                if (PathManager.FILE_CRASH_REPORT.isFile) {
                    writeSanitized(zip, "launcher_crash.log", PathManager.FILE_CRASH_REPORT)
                }

                PathManager.DIR_LAUNCHER_LOGS.listFiles()
                    ?.filter { it.isFile && it.extension == "log" && it != PathManager.FILE_CRASH_REPORT }
                    ?.sortedByDescending { it.lastModified() }
                    ?.take(3)
                    ?.forEach { log ->
                        writeSanitized(zip, "logs/${log.name}", log)
                    }
            }
            output
        }.getOrNull()
    }

    fun deviceSummary(context: Context): String {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val versionCode = when {
            packageInfo == null -> "unknown"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> packageInfo.longVersionCode.toString()
            else -> packageInfo.versionCode.toString()
        }
        val memory = ActivityManager.MemoryInfo()
        runCatching {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        }
        val storage = runCatching {
            StatFs(Environment.getDataDirectory().absolutePath)
        }.getOrNull()
        return buildString {
            appendLine("time=" + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date()))
            appendLine("package=" + context.packageName)
            appendLine("version=" + (packageInfo?.versionName ?: "unknown") + " (" + versionCode + ")")
            appendLine("manufacturer=" + Build.MANUFACTURER)
            appendLine("model=" + Build.MODEL)
            appendLine("device=" + Build.DEVICE)
            appendLine("android=" + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")")
            appendLine("abis=" + Build.SUPPORTED_ABIS.joinToString(","))
            appendLine("availableMemoryMb=" + memory.availMem / 1024 / 1024)
            appendLine("totalMemoryMb=" + memory.totalMem / 1024 / 1024)
            appendLine("lowMemory=" + memory.lowMemory)
            if (storage != null) {
                appendLine("freeStorageMb=" + storage.availableBytes / 1024 / 1024)
                appendLine("totalStorageMb=" + storage.totalBytes / 1024 / 1024)
            }
        }
    }

    private fun writeSanitized(zip: ZipOutputStream, entry: String, file: File) {
        zip.putNextEntry(ZipEntry(entry))
        var written = 0
        file.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (sensitiveLine.containsMatchIn(line)) continue
                val bytes = (line + "\n").toByteArray()
                if (written + bytes.size > MAX_ENTRY_BYTES) break
                zip.write(bytes)
                written += bytes.size
            }
        }
        zip.closeEntry()
    }

    private fun trim(dir: File) {
        dir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_REPORTS)
            ?.forEach { it.delete() }
    }
}