package com.cmc.launcher.ui.screens.content.versions.layouts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cmc.launcher.ui.components.StaticDialog as Dialog
import com.cmc.launcher.R
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.theme.CardShape
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

internal data class VersionTool(
    val page: NormalNavKey.Versions,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @StringRes val group: Int? = null
)

internal val versionTools = listOf(
    VersionTool(NormalNavKey.Versions.OverView, R.string.versions_settings_overview, R.drawable.ic_dashboard_outlined, R.string.pe_version_group_game),
    VersionTool(NormalNavKey.Versions.Config, R.string.versions_settings_config, R.drawable.ic_build_outlined),
    VersionTool(NormalNavKey.Versions.ModifyVersion, R.string.versions_modify_version, R.drawable.ic_edit_outlined),
    VersionTool(NormalNavKey.Versions.ModsManager, R.string.mods_manage, R.drawable.ic_extension_outlined, R.string.pe_version_group_content),
    VersionTool(NormalNavKey.Versions.SavesManager, R.string.saves_manage, R.drawable.ic_public),
    VersionTool(NormalNavKey.Versions.ResourcePackManager, R.string.resource_pack_manage, R.drawable.ic_format_paint_outlined),
    VersionTool(NormalNavKey.Versions.ShadersManager, R.string.shader_pack_manage, R.drawable.ic_lightbulb),
    VersionTool(NormalNavKey.Versions.ScreenshotsManager, R.string.screenshots_manage, R.drawable.ic_photo_library_outlined),
    VersionTool(NormalNavKey.Versions.ServerList, R.string.servers_list, R.drawable.ic_dns_outlined, R.string.pe_version_group_network)
)

@Composable
internal fun VersionWorkspace(
    name: String,
    page: TitledNavKey?,
    onSelect: (NormalNavKey.Versions) -> Unit,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(204.dp).fillMaxHeight().padding(horizontal = Space.sm)) {
                    Text(name, Modifier.padding(horizontal = Space.md, vertical = Space.lg),
                        style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    VersionToolList(page, onSelect, Modifier.weight(1f))
                }
                Spacer(Modifier.fillMaxHeight().width(Size.stroke).background(colors.outlineVariant))
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            var chooseTool by rememberSaveable { mutableStateOf(false) }
            val tool = versionTools.find { it.page == page } ?: versionTools.first()
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = null, indication = null, role = Role.Button,
                        onClickLabel = stringResource(R.string.pe_version_sections),
                        onClick = { chooseTool = true }
                    ).heightIn(min = Size.touch).padding(horizontal = Space.screenH, vertical = Space.xs),
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(tool.icon), null, Modifier.size(Size.iconSm), tint = colors.primary)
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(tool.label), style = MaterialTheme.typography.titleSmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(painterResource(R.drawable.ic_arrow_drop_down_rounded), null, tint = colors.onSurfaceVariant)
                }
                HorizontalDivider(color = colors.outlineVariant)
                Box(Modifier.fillMaxWidth().weight(1f)) { content() }
            }
            if (chooseTool) {
                Dialog(onDismissRequest = { chooseTool = false }) {
                    Surface(shape = CardShape, color = colors.surfaceContainerHigh) {
                        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(Space.sm)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.pe_version_sections),
                                    Modifier.weight(1f).padding(start = Space.md).semantics { heading() },
                                    style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { chooseTool = false }) {
                                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.generic_close))
                                }
                            }
                            VersionToolList(page, { dest -> chooseTool = false; onSelect(dest) }, Modifier.weight(1f, fill = false))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionToolList(page: TitledNavKey?, onSelect: (NormalNavKey.Versions) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.verticalScroll(rememberScrollState()).selectableGroup().padding(bottom = Space.md)) {
        versionTools.forEach { tool ->
            tool.group?.let { group ->
                Text(stringResource(group), Modifier.padding(start = Space.md, top = Space.md, bottom = Space.xs).semantics { heading() },
                    style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            val selected = page == tool.page
            Row(
                modifier = Modifier.fillMaxWidth().clip(FieldShape)
                    .background(if (selected) colors.primary.copy(alpha = 0.10f) else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, interactionSource = null, indication = null,
                        onClick = { onSelect(tool.page) })
                    .heightIn(min = Size.touch).padding(horizontal = Space.md, vertical = Space.xs),
                horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(tool.icon), null, Modifier.size(Size.iconSm),
                    tint = if (selected) colors.primary else colors.onSurfaceVariant)
                Text(stringResource(tool.label), Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = colors.onSurface, style = MaterialTheme.typography.bodyMedium)
                if (selected) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(Size.iconXs), tint = colors.primary)
            }
        }
    }
}
