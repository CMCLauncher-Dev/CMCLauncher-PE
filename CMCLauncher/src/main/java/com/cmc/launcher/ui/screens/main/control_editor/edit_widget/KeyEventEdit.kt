package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.ui.control.Keyboard
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.itemColor
import com.cmc.launcher.ui.theme.onItemColor
import com.movtery.inputmap.keycodes.ControlEventKeyName
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.observable.ObservableClickEventsProvider

@Composable
fun KeyEventEdit(
    provider: ObservableClickEventsProvider, modifier: Modifier = Modifier,
    containerColor: Color = itemColor(false), contentColor: Color = onItemColor()
) {
    var choosing by remember(provider) { mutableStateOf(false) }
    val keys = provider.clickEvents.filter { it.type == ClickEvent.Type.Key }
    LazyColumn(modifier, contentPadding = PaddingValues(vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        item {
            TextButton(onClick = { choosing = true }) {
                Text(stringResource(R.string.control_editor_edit_event_key_new))
            }
        }
        items(keys) { event ->
            val name = remember(event.key) { ControlEventKeyName.getNameByKey(event.key) ?: event.key }
            Surface(shape = FieldShape, color = containerColor, contentColor = contentColor) {
                Row(Modifier.fillMaxWidth().heightIn(min = Size.row).padding(start = Space.md, end = Space.xs),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.control_editor_edit_event_key_value, name), Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { provider.onRemoveEvent(event) }) {
                        val remove = stringResource(R.string.generic_delete)
                        Icon(painterResource(R.drawable.ic_delete_outlined), "$remove · $name")
                    }
                }
            }
        }
    }
    if (choosing) Keyboard(onDismissRequest = { choosing = false }, isTapMode = true, onTap = { code ->
        val next = ClickEvent(ClickEvent.Type.Key, code)
        if (next !in provider.clickEvents) provider.onAddEvent(next)
        choosing = false
    })
}
