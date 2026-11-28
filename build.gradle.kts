plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp.plugin) apply false
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
}

buildscript {
    repositories {
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.buildkeys)
    }
}

run {
    val manifestFile = file("gradle/vendored-deps.json")
    if (manifestFile.isFile) {
        val entries = (groovy.json.JsonSlurper().parse(manifestFile) as Map<*, *>)["files"] as List<*>
        entries.forEach { raw ->
            val entry = raw as Map<*, *>
            val path = entry["path"] as String
            val size = (entry["size"] as Number).toLong()
            val expected = entry["sha256"] as String
            val target = file(path)
            if (target.isFile && target.length() == size) {
                return@forEach
            }
            val temporary = File(target.parentFile, target.name + ".part")
            target.parentFile.mkdirs()
            java.net.URI(entry["url"] as String).toURL().openStream().use { input ->
                temporary.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            temporary.inputStream().use { stream ->
                val buffer = ByteArray(1 shl 20)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) {
                        break
                    }
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != expected || temporary.length() != size) {
                temporary.delete()
                throw GradleException("Vendored dependency is corrupted: " + path)
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
        }
    }
}
