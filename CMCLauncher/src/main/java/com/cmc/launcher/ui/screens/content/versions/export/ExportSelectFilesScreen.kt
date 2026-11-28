package com.cmc.launcher.ui.screens.content.versions.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmc.launcher.R
import com.cmc.launcher.game.version.export.data.FileSelectionData
import com.cmc.launcher.game.version.export.data.Selected
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

@Composable
fun ExportSelectFilesScreen(
    allFiles: List<FileSelectionData>,
    selectedFiles: Boolean,
    onRefreshRootSelect: () -> Unit,
    isSelectingFolder: Boolean,
    isRefreshingFiles: Boolean,
    mainScreenKey: TitledNavKey?,
    exportScreenKey: TitledNavKey?,
    version: Version,
    backToMainScreen: () -> Unit,
    onFinish: () -> Unit,
    isChangingSelection: Boolean = false,
    onFileSelection: (FileSelectionData, Selected) -> Unit = { data, state ->
        data.updateSelectState(state)
        onRefreshRootSelect()
    }
) {
    if (!version.isValid()) {
        LaunchedEffect(version) { backToMainScreen() }
        return
    }
    if (mainScreenKey !is NestedNavKey.VersionExport || exportScreenKey != NormalNavKey.VersionExports.SelectFiles) return
    val busy = isSelectingFolder || isRefreshingFiles || isChangingSelection
    var expansion by remember(allFiles) { mutableIntStateOf(0) }
    val nodes = rememberVisibleNodes(allFiles, expansion)
    ExportStepLayout(
        step = ExportStep.Files,
        action = stringResource(R.string.versions_export_pack_select_output),
        enabled = !busy && selectedFiles,
        proceed = { if (!busy && selectedFiles) onFinish() }
    ) {
        if (isRefreshingFiles || allFiles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(Space.lg), contentAlignment = Alignment.Center) {
                Text(stringResource(if (isRefreshingFiles) R.string.generic_loading else R.string.versions_export_pack_dir_empty))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                if (isChangingSelection) item(key = "selection-busy") {
                    Text(stringResource(R.string.generic_in_progress), Modifier.padding(Space.sm),
                        style = MaterialTheme.typography.labelMedium)
                }
                items(nodes, key = { it.key }) { node ->
                    val indent = (node.indentation.coerceAtMost(4) * 12).dp
                    when (node) {
                        is VisibleNode.EmptyNode -> Text(stringResource(R.string.versions_export_pack_dir_empty),
                            Modifier.padding(start = indent + Size.touch, top = Space.xs, bottom = Space.xs),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        is VisibleNode.FileNode -> ExportFileRow(node, busy, Modifier.padding(start = indent),
                            onExpand = { expansion++ }, onSelect = onFileSelection)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFileRow(node: VisibleNode.FileNode, busy: Boolean, modifier: Modifier,
    onExpand: () -> Unit, onSelect: (FileSelectionData, Selected) -> Unit) {
    val data = node.data
    val expanded by data.expand.collectAsStateWithLifecycle()
    val selected by data.selected.collectAsStateWithLifecycle()
    val state = when (selected) {
        Selected.Selected -> ToggleableState.On
        Selected.Unselected -> ToggleableState.Off
        Selected.Indeterminate -> ToggleableState.Indeterminate
    }
    val enabled = !busy && (data.child == null || data.child.isNotEmpty())
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (data.child != null) {
            IconButton(enabled = !busy, onClick = { data.expandDirs(!expanded); onExpand() }) {
                Icon(painterResource(if (expanded) R.drawable.ic_arrow_drop_down_rounded else R.drawable.ic_arrow_right_rounded),
                    stringResource(if (expanded) R.string.generic_collapse else R.string.generic_expand) + " · " + data.file.name)
            }
        } else Spacer(Modifier.width(Size.touch))
        Row(Modifier.weight(1f).clip(FieldShape)
            .triStateToggleable(state = state, enabled = enabled, role = Role.Checkbox, interactionSource = null, indication = null,
                onClick = { onSelect(data, if (selected == Selected.Selected) Selected.Unselected else Selected.Selected) })
            .heightIn(min = Size.touch).padding(horizontal = Space.xs, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            TriStateCheckbox(state = state, onClick = null, enabled = enabled)
            Icon(painterResource(if (data.child == null) R.drawable.ic_description_outlined else R.drawable.ic_folder_outlined),
                null, Modifier.size(Size.iconSm), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(data.file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val detail = data.alias?.let { stringResource(it) } ?: data.file.parent.orEmpty().takeIf { node.indentation > 4 }
                if (detail != null) Text(detail, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}