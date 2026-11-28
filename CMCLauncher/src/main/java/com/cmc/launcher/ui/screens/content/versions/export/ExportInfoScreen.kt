package com.cmc.launcher.ui.screens.content.versions.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.version.export.ExportInfo
import com.cmc.launcher.game.version.export.PackType
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.settings.layouts.CardPosition
import com.cmc.launcher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.cmc.launcher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.utils.platform.getMaxMemoryForSettings

@Composable
fun ExportInfoScreen(
    info: ExportInfo,
    onInfoEdited: (ExportInfo) -> Unit,
    onFinishClick: () -> Unit,
    mainScreenKey: TitledNavKey?,
    exportScreenKey: TitledNavKey?
) {
    if (mainScreenKey !is NestedNavKey.VersionExport || exportScreenKey != NormalNavKey.VersionExports.EditInfo) return
    val latest by rememberUpdatedState(info)
    val update by rememberUpdatedState(onInfoEdited)
    fun edit(change: (ExportInfo) -> ExportInfo) { update(change(latest)) }
    val options = info.packType.options
    val context = LocalContext.current
    val maxMemory = remember(context) { getMaxMemoryForSettings(context).coerceAtLeast(1) }
    val valid = info.name.isNotBlank() && info.version.isNotBlank() && (!options.requireAuthor || info.author.isNotBlank())
    ExportStepLayout(ExportStep.Info, stringResource(R.string.versions_export_pack_select_files), valid, onFinishClick) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            item(key = "format") {
                Text(stringResource(info.packType.titleResource()), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            item(key = "name") {
                ExportField(R.string.versions_export_pack_name, info.name, required = true) { value ->
                    val safe = cleanPackName(value)
                    edit { it.copy(name = safe) }
                }
            }
            item(key = "version") {
                ExportField(R.string.versions_export_pack_version, info.version, required = true) { value -> edit { it.copy(version = value) } }
            }
            if (options.requireAuthor) item(key = "author") {
                ExportField(R.string.versions_export_pack_author, info.author, required = true) { value -> edit { it.copy(author = value) } }
            }
            if (options.requireSummary) item(key = "summary") {
                ExportField(R.string.versions_export_pack_summary, info.summary.orEmpty(), multiline = true,
                    hint = stringResource(R.string.versions_export_pack_summary_hint)) { value ->
                    edit { it.copy(summary = value.takeIf(String::isNotBlank)) }
                }
            }
            if (options.requireWebsiteUrl) item(key = "website") {
                ExportField(R.string.versions_export_pack_website, info.url) { value -> edit { it.copy(url = value) } }
            }
            if (options.requireGameArgs) item(key = "game-args") {
                ExportField(R.string.versions_export_pack_game_args, info.gameArgs) { value -> edit { it.copy(gameArgs = value) } }
            }
            if (options.requireJavaArgs) item(key = "java-args") {
                ExportField(R.string.versions_export_pack_java_args, info.javaArgs) { value -> edit { it.copy(javaArgs = value) } }
            }
            if (options.requireMinMemory) item(key = "memory") {
                IntSliderSettingsCard(value = info.minMemory, title = stringResource(R.string.versions_export_pack_min_memory),
                    position = CardPosition.Single, valueRange = 0f..maxOf(maxMemory, info.minMemory).toFloat(), suffix = " MB",
                    fineTuningControl = true, onValueChange = { value -> edit { it.copy(minMemory = value) } })
            }
            if (options.requirePackModrinth || options.requirePackCurseForge) item(key = "remote-divider") { HorizontalDivider() }
            if (options.requirePackModrinth) item(key = "modrinth") {
                SwitchSettingsCard(checked = info.packModrinth, onCheckedChange = { value -> edit { it.copy(packModrinth = value) } },
                    title = stringResource(R.string.versions_export_pack_pack_remote, Platform.MODRINTH.displayName), position = CardPosition.Single)
            }
            if (options.requirePackCurseForge) item(key = "curseforge") {
                val available = info.packType == PackType.CurseForge || (info.packType == PackType.Modrinth && info.packModrinth)
                SwitchSettingsCard(checked = info.packCurseForge, enabled = available,
                    onCheckedChange = { value -> edit { it.copy(packCurseForge = value) } },
                    title = if (info.packType == PackType.Modrinth) stringResource(R.string.versions_export_pack_also_pack_curseforge)
                        else stringResource(R.string.versions_export_pack_pack_remote, Platform.CURSEFORGE.displayName), position = CardPosition.Single)
            }
            val remote = when (info.packType) {
                PackType.Modrinth -> info.packModrinth
                PackType.CurseForge -> info.packCurseForge
                PackType.MCBBS -> false
            }
            if (remote) item(key = "remote-notes") {
                val platforms = buildList {
                    if (info.packType == PackType.Modrinth && info.packModrinth) add(Platform.MODRINTH.displayName)
                    if (info.packCurseForge) add(Platform.CURSEFORGE.displayName)
                }.joinToString(" / ")
                Text(stringResource(R.string.versions_export_tip_remote_1, platforms), style = MaterialTheme.typography.bodySmall)
                if (info.packType == PackType.Modrinth) Text(stringResource(R.string.versions_export_tip_remote_2), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

internal fun cleanPackName(value: String): String = value.filterNot { it.code < 32 || it in "\\\"?:*/<>|" }

@Composable
private fun ExportField(label: Int, value: String, required: Boolean = false, multiline: Boolean = false,
    hint: String? = null, change: (String) -> Unit) {
    val invalid = required && value.isBlank()
    OwnOutlinedTextField(value = value, modifier = Modifier.fillMaxWidth(),
        onValueChange = { input -> change(if (multiline) input else input.replace('\r', ' ').replace('\n', ' ')) },
        label = { Text(stringResource(label)) }, singleLine = !multiline, minLines = if (multiline) 3 else 1,
        isError = invalid, supportingText = if (!invalid && hint == null) null else ({
            if (invalid) Text(stringResource(R.string.generic_cannot_empty)) else if (hint != null) Text(hint)
        }))
}