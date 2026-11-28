package com.cmc.launcher.ui.screens.main.control_editor.edit_layer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.components.SingleLineTextCheck
import com.cmc.launcher.ui.kit.CmcListAction
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutListItem
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSwitchItem
import com.cmc.launcher.ui.screens.main.control_editor.getVisibilityText
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.observable.ObservableControlLayer

@Composable
fun EditControlLayerDialog(
    layer: ObservableControlLayer,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    onMergeDownward: () -> Unit,
    onCopy: () -> Unit,
    onHideChange: (Boolean) -> Unit
) {
    key(layer.uuid) {
        DialogFrame(stringResource(R.string.control_editor_layers_attribute), onDismissRequest, actions = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.generic_close)) }
        }) {
            Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = Space.lg, end = Space.lg, bottom = Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                SingleLineTextCheck(text = layer.name, onSingleLined = { layer.name = it })
                OwnOutlinedTextField(value = layer.name, onValueChange = { layer.name = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(R.string.control_editor_layers_attribute_name)) })
                InfoLayoutListItem(title = stringResource(R.string.control_editor_edit_visibility),
                    items = VisibilityType.entries, selectedItem = layer.visibilityType,
                    onItemSelected = { layer.visibilityType = it }, getItemText = { it.getVisibilityText() })
                InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_layers_attribute_hide),
                    value = layer.editorHide, onValueChange = { layer.editorHide = it; onHideChange(it) })
                InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_layers_attribute_hide_when_mouse),
                    value = layer.hideWhenMouse, onValueChange = { layer.hideWhenMouse = it })
                InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_layers_attribute_hide_when_gamepad),
                    value = layer.hideWhenGamepad, onValueChange = { layer.hideWhenGamepad = it })
                HorizontalDivider()
                CmcListAction(stringResource(R.string.generic_copy), R.drawable.ic_copy_all_outlined, onCopy)
                CmcListAction(stringResource(R.string.control_editor_layers_merge_downward), R.drawable.ic_merge,
                    onClick = { onDismissRequest(); onMergeDownward() })
                CmcListAction(stringResource(R.string.generic_delete), R.drawable.ic_delete_outlined,
                    onClick = onDelete, destructive = true)
            }
        }
    }
}
