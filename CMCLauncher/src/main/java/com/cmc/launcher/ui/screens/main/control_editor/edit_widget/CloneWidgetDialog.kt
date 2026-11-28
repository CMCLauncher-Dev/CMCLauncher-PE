package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.screens.main.control_editor.LayerChecklist
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.observable.ObservableControlLayer

@Composable
fun SelectLayers(
    layers: List<ObservableControlLayer>,
    initLayer: ObservableControlLayer,
    onDismissRequest: () -> Unit,
    title: String,
    onConfirm: (selected: List<ObservableControlLayer>) -> Unit,
    confirmText: String = stringResource(R.string.generic_confirm)
) {
    var ids by rememberSaveable(initLayer.uuid) { mutableStateOf(arrayListOf(initLayer.uuid)) }
    val selected = ids.toSet()
    val targets = layers.filter { it.uuid in selected }
    DialogFrame(title, onDismissRequest, actions = {
        TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.generic_cancel)) }
        TextButton(enabled = targets.isNotEmpty(), onClick = { onConfirm(targets) }) { Text(confirmText) }
    }) {
        Text(stringResource(R.string.pe_selected_count, targets.size), Modifier.padding(horizontal = Space.lg),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LayerChecklist(layers, selected, onToggle = { layer, checked ->
            ids = ArrayList(if (checked) selected + layer.uuid else selected - layer.uuid)
        }, modifier = Modifier.weight(1f, fill = false), initialLayerId = initLayer.uuid)
    }
}
