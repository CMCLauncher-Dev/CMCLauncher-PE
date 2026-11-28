package com.cmc.launcher.game.version.mod

enum class ModIssueType {
    MISSING_DEPENDENCY,
    VERSION_MISMATCH,
    DUPLICATE_ID
}

data class ModIssue(
    val type: ModIssueType,
    val modName: String,
    val detail: String,
    val versions: String = "",
    val filePath: String = ""
)

object ModIssueChecker {

    private val platformIds = setOf(
        "minecraft",
        "java",
        "forge",
        "neoforge",
        "fabricloader",
        "quilt_loader",
        "quilt_base"
    )

    fun check(mods: List<LocalMod>): List<ModIssue> {
        val enabled = mods.filter { !it.notMod && it.id.isNotBlank() && it.file.isEnabled() }
        val byId = enabled.groupBy { it.id }
        val issues = mutableListOf<ModIssue>()

        byId.forEach { (id, group) ->
            if (group.size > 1) {
                issues += ModIssue(
                    type = ModIssueType.DUPLICATE_ID,
                    modName = group.first().name,
                    detail = id,
                    versions = group.mapNotNull { it.version }.distinct().joinToString(" / "),
                    filePath = group.first().file.absolutePath
                )
            }
        }

        enabled.forEach { mod ->
            mod.dependencies.forEach { (dependency, version) ->
                if (dependency in platformIds) return@forEach
                if (mod.unlessDependencies[dependency]?.let { byId.containsKey(it) } == true) return@forEach
                val installed = byId[dependency]?.firstOrNull()
                if (installed == null) {
                    issues += ModIssue(
                        type = ModIssueType.MISSING_DEPENDENCY,
                        modName = mod.name,
                        detail = dependency,
                        versions = version,
                        filePath = mod.file.absolutePath
                    )
                } else if (version.isNotBlank() && !ModVersionRange.matches(installed.version, version)) {
                    issues += ModIssue(
                        type = ModIssueType.VERSION_MISMATCH,
                        modName = mod.name,
                        detail = dependency,
                        versions = (installed.version ?: "?") + " / " + version,
                        filePath = mod.file.absolutePath
                    )
                }
            }
        }

        return issues
    }
}