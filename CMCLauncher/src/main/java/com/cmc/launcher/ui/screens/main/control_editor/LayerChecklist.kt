package com.cmc.launcher.ui.screens.main.control_editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.ui.components.SimpleListItem
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.observable.ObservableControlLayer

@Composable
internal fun LayerChecklist(
    layers: List<ObservableControlLayer>,
    selected: Set<String>,
    onToggle: (ObservableControlLayer, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    initialLayerId: String? = null
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = layers.indexOfFirst { it.uuid == initialLayerId }.coerceAtLeast(0))
    LazyColumn(
        modifier = modifier.fillMaxWidth(), state = state,
        contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        if (layers.isEmpty()) {
            item { Text(stringResource(R.string.control_editor_menu_no_layers_title), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(layers, key = { it.uuid }) { layer ->
                SimpleListItem(
                    itemName = layer.name.orUnspecified(), checked = layer.uuid in selected,
                    onCheckedChange = { onToggle(layer, it) }, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
