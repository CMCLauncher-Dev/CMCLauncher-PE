package com.cmc.launcher.ui.screens.main.control_editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.components.SingleLineTextCheck
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

@Composable
internal fun EditorChoices(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Row(Modifier.weight(1f).clip(FieldShape)
                .background(if (active) colors.primaryContainer else colors.surfaceContainerHighest)
                .selectable(selected = active, role = Role.RadioButton, interactionSource = null, indication = null,
                    onClick = { onSelect(index) })
                .heightIn(min = Size.touch).padding(horizontal = Space.xs, vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                    color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun StyleEditorForm(
    id: String, title: String, nameLabel: String, name: String, onName: (String) -> Unit,
    common: Boolean, onCommon: (Boolean) -> Unit, close: () -> Unit,
    preview: @Composable (dark: Boolean) -> Unit,
    extra: (@Composable () -> Unit)? = null,
    fields: LazyListScope.(dark: Boolean) -> Unit
) {
    key(id) {
        var darkChoice by rememberSaveable { mutableStateOf(false) }
        val dark = !common && darkChoice
        DialogFrame(title, close, actions = {
            TextButton(onClick = close) { Text(stringResource(R.string.generic_close)) }
        }) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                item(key = "identity") {
                    SingleLineTextCheck(text = name, onSingleLined = onName)
                    OwnOutlinedTextField(value = name, onValueChange = onName, modifier = Modifier.fillMaxWidth(),
                        label = { Text(nameLabel) }, singleLine = true)
                }
                item(key = "theme") {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_edit_style_config_common_style),
                            value = common, onValueChange = { onCommon(it); if (it) darkChoice = false })
                        if (!common) EditorChoices(listOf(stringResource(R.string.control_editor_edit_style_config_light),
                            stringResource(R.string.control_editor_edit_style_config_dark)), if (dark) 1 else 0, { darkChoice = it == 1 })
                    }
                }
                if (extra != null) item(key = "options") { extra() }
                item(key = "sample") { preview(dark) }
                fields(dark)
            }
        }
    }
}
