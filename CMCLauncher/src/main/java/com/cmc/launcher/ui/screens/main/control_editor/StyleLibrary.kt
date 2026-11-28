package com.cmc.launcher.ui.screens.main.control_editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

@Composable
internal fun StyleLibraryRow(
    name: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onOpen: () -> Unit,
    preview: @Composable () -> Unit,
    subtitle: String? = null,
    actions: @Composable () -> Unit
) {
    Surface(shape = FieldShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clickable(role = Role.Button, onClick = onOpen)
                    .heightIn(min = Size.row).padding(Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(Size.touch), contentAlignment = Alignment.Center) { preview() }
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onExpand) {
                    val action = stringResource(if (expanded) R.string.generic_collapse else R.string.generic_more)
                    Icon(painterResource(if (expanded) R.drawable.ic_arrow_drop_up_rounded else R.drawable.ic_more_horiz),
                        "$name · $action")
                }
            }
            if (expanded) {
                HorizontalDivider()
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)) { actions() }
            }
        }
    }
}

@Composable
internal fun <T> StyleLibraryDialog(
    title: String, empty: String, entries: List<T>, id: (T) -> String,
    name: @Composable (T) -> String, preview: @Composable (T) -> Unit,
    edit: (T) -> Unit, clone: (T) -> Unit, favorite: (T) -> Unit, delete: (T) -> Unit,
    create: () -> Unit, cabinet: () -> Unit, close: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    DialogFrame(title, close, actions = {
        TextButton(onClick = create) { Text(stringResource(R.string.control_manage_create_new)) }
        TextButton(onClick = close) { Text(stringResource(R.string.generic_close)) }
    }) {
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            item { TextButton(onClick = cabinet) { Text(stringResource(R.string.control_editor_style_cabinet)) } }
            if (entries.isEmpty()) item { Text(empty, style = MaterialTheme.typography.bodyMedium) }
            items(entries, key = { id(it) }) { entry ->
                val key = id(entry)
                StyleLibraryRow(name(entry), expanded == key, { expanded = if (expanded == key) null else key },
                    onOpen = { edit(entry) }, preview = { preview(entry) }) {
                    TextButton(onClick = { expanded = null; clone(entry) }) { Text(stringResource(R.string.generic_copy)) }
                    TextButton(onClick = { expanded = null; favorite(entry) }) { Text(stringResource(R.string.control_editor_style_cabinet_favorite)) }
                    TextButton(onClick = { expanded = null; delete(entry) }) {
                        Text(stringResource(R.string.generic_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
