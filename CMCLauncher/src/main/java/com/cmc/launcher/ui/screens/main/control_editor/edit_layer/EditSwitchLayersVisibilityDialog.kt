package com.cmc.launcher.ui.screens.main.control_editor.edit_layer

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.screens.main.control_editor.LayerChecklist
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.observable.ObservableClickEventsProvider
import com.movtery.layer_controller.observable.ObservableControlLayer

@Composable
fun EditSwitchLayersVisibilityDialog(
    data: ObservableClickEventsProvider,
    layers: List<ObservableControlLayer>,
    type: ClickEvent.Type,
    onDismissRequest: () -> Unit
) {
    val title = when (type) {
        ClickEvent.Type.ShowLayer -> R.string.control_editor_edit_show_layers
        ClickEvent.Type.HideLayer -> R.string.control_editor_edit_hide_layers
        ClickEvent.Type.SwitchLayer -> R.string.control_editor_edit_switch_layers
        else -> { LaunchedEffect(type) { onDismissRequest() }; return }
    }
    val events = data.clickEvents
    val validIds = layers.mapTo(mutableSetOf()) { it.uuid }
    val orphaned = events.filter { it.isAboutLayers() && it.key !in validIds }
    LaunchedEffect(data, orphaned) { if (orphaned.isNotEmpty()) data.onRemoveAllEvents(orphaned) }
    val selected = events.filter { it.type == type }.mapTo(mutableSetOf()) { it.key }
    DialogFrame(stringResource(title), onDismissRequest, actions = {
        TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.generic_close)) }
    }) {
        LayerChecklist(layers, selected, onToggle = { layer, checked ->
            val event = ClickEvent(type, layer.uuid)
            if (checked) data.onAddEvent(event) else data.onRemoveEvent(event)
        }, modifier = Modifier.weight(1f, fill = false))
    }
}
