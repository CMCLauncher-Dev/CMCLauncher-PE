package com.cmc.launcher.ui.screens.content.versions.layouts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.cmc.launcher.ui.components.StaticDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.contract.extensionToMimeType
import com.cmc.launcher.coroutine.TaskSystem
import com.cmc.launcher.ui.screens.content.elements.SortByDropdownMenu
import com.cmc.launcher.ui.screens.content.elements.SortByEnum
import com.cmc.launcher.ui.screens.content.elements.rememberMultipleUriImportTaskBuilder
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.viewmodel.ErrorViewModel
import java.io.File

internal class LibrarySelection(
    val count: Int,
    val selectAll: () -> Unit,
    val clear: () -> Unit,
    val requestDelete: () -> Unit,
    val checkUpdates: (() -> Unit)? = null
)

@Composable
internal fun PackToolbar(
    query: String,
    onQuery: (String) -> Unit,
    sortOptions: List<SortByEnum>,
    sortBy: SortByEnum,
    onSortBy: (SortByEnum) -> Unit,
    ascending: Boolean,
    onReverse: () -> Unit,
    onRefresh: () -> Unit,
    selection: LibrarySelection? = null,
    onImport: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    filterMenuItem: @Composable (dismiss: () -> Unit) -> Unit = {}
) {
    var sortOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query, onValueChange = onQuery, modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.generic_search)) }, singleLine = true,
                shape = FieldShape, textStyle = MaterialTheme.typography.bodyMedium
            )
            Box {
                PackToolIcon(R.drawable.ic_sort, R.string.sort_by) { sortOpen = true }
                SortByDropdownMenu(
                    expanded = sortOpen, onClose = { sortOpen = false }, enums = sortOptions,
                    currentEnum = sortBy, onEnumChanged = onSortBy, isAscending = ascending, onToggleSortOrder = onReverse
                )
            }
            Box {
                PackToolIcon(R.drawable.ic_more_vert, R.string.generic_more) { moreOpen = true }
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    filterMenuItem { moreOpen = false }
                    if (onImport != null) {
                        PackMenuAction(R.drawable.ic_file_copy_filled, R.string.generic_import) { moreOpen = false; onImport() }
                    }
                    if (onDownload != null) {
                        PackMenuAction(R.drawable.ic_download_2_outlined, R.string.generic_download) { moreOpen = false; onDownload() }
                    }
                    PackMenuAction(R.drawable.ic_refresh, R.string.generic_refresh) { moreOpen = false; onRefresh() }
                    if (selection != null) {
                        PackMenuAction(R.drawable.ic_select_all, R.string.fm_select_all) { moreOpen = false; selection.selectAll() }
                        selection.checkUpdates?.takeIf { selection.count > 0 }?.let { update ->
                            PackMenuAction(R.drawable.ic_update, R.string.mods_update_task_check_update) { moreOpen = false; update() }
                        }
                    }
                }
            }
        }
        if (selection != null && selection.count > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = Size.touch).padding(horizontal = Space.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                PackToolIcon(R.drawable.ic_close, R.string.fm_clear_selection, selection.clear)
                Text(
                    text = stringResource(R.string.pe_selected_count, selection.count),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                PackToolIcon(R.drawable.ic_select_all, R.string.fm_select_all, selection.selectAll)
                PackToolIcon(R.drawable.ic_delete_outlined, R.string.generic_delete, selection.requestDelete)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun PackToolIcon(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), stringResource(label), Modifier.size(Size.iconSm))
    }
}

@Composable
private fun PackMenuAction(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) },
        leadingIcon = { Icon(painterResource(icon), null) }, onClick = onClick)
}

@Composable
internal fun rememberPackImport(
    taskId: String,
    directory: File,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    extension: String = "zip",
    errorMessage: String = stringResource(R.string.error_import_file),
    onFileCopied: suspend (com.cmc.launcher.coroutine.Task, File) -> Unit = { _, _ -> },
    refresh: () -> Unit
): () -> Unit {
    val task = key(taskId, directory, extension) {
        rememberMultipleUriImportTaskBuilder(
            id = taskId,
            targetDir = directory,
            checkExtension = listOf(extension),
            errorMessage = errorMessage,
            submitError = submitError,
            onFileCopied = onFileCopied,
            onImported = refresh
        )
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) TaskSystem.submitTask(task(uris))
    }
    return { picker.launch(extension.extensionToMimeType()) }
}