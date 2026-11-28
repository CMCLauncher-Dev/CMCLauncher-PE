package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

@Composable
internal fun <T> WidgetStylePicker(
    entries: List<T>, selectedId: String?, change: (String?) -> Unit,
    id: (T) -> String, name: @Composable (T) -> String, sample: @Composable (T) -> Unit,
    manageLabel: String, emptyLabel: String, manage: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        item { TextButton(onClick = manage) { Text(manageLabel) } }
        if (entries.isEmpty()) item { Text(emptyLabel, style = MaterialTheme.typography.bodyMedium) }
        items(entries, key = { id(it) }) { entry ->
            val entryId = id(entry)
            val active = entryId == selectedId
            Surface(shape = FieldShape, color = if (active) colors.primaryContainer else colors.surfaceContainerHighest) {
                Row(Modifier.fillMaxWidth().clip(FieldShape).selectable(selected = active, role = Role.RadioButton,
                    interactionSource = null, indication = null, onClick = { change(if (active) null else entryId) })
                    .heightIn(min = Size.row).padding(Space.sm), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    Box(Modifier.size(Size.touch), contentAlignment = Alignment.Center) { sample(entry) }
                    Text(name(entry), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (active) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(Size.iconSm))
                }
            }
        }
    }
}
