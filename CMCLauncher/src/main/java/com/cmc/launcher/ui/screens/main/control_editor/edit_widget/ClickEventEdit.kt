package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.main.control_editor.EditorChoices
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSwitchItem
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutTextItem
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.observable.ObservableClickEventsProvider
import com.movtery.layer_controller.observable.ObservableNormalData
import com.movtery.layer_controller.observable.clickEventsProvider

@Composable
fun EditWidgetClickEvent(
    screenKey: TitledNavKey, currentKey: TitledNavKey?, data: ObservableNormalData,
    switchControlLayers: (ObservableClickEventsProvider, ClickEvent.Type) -> Unit,
    sendText: (ObservableClickEventsProvider) -> Unit
) {
    if (screenKey != currentKey) return
    val provider = remember(data) { clickEventsProvider(data) }
    EventCategories(data.uuid, provider, sendText) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_edit_event_swipple),
                value = data.isSwipple, onValueChange = { data.isSwipple = it })
            InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_edit_event_penetrable),
                value = data.isPenetrable, onValueChange = { data.isPenetrable = it })
            InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_edit_event_toggleable),
                value = data.isToggleable, onValueChange = { data.isToggleable = it })
            HorizontalDivider()
            LayerEventActions { switchControlLayers(provider, it) }
        }
    }
}

@Composable
internal fun EventCategories(
    owner: String, provider: ObservableClickEventsProvider, sendText: (ObservableClickEventsProvider) -> Unit,
    basic: @Composable () -> Unit
) {
    var tab by rememberSaveable(owner) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        EditorChoices(listOf(stringResource(R.string.control_editor_edit_event_basic),
            stringResource(R.string.control_editor_edit_event_launcher), stringResource(R.string.control_editor_edit_event_key)),
            tab, { tab = it }, Modifier.padding(Space.sm))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (tab) {
                0 -> basic()
                1 -> LauncherEventsEdit(provider, { sendText(provider) }, Modifier.fillMaxSize().padding(horizontal = Space.md))
                else -> KeyEventEdit(provider, Modifier.fillMaxSize().padding(horizontal = Space.md))
            }
        }
    }
}

@Composable
internal fun LayerEventActions(onEdit: (ClickEvent.Type) -> Unit) {
    InfoLayoutTextItem(title = stringResource(R.string.control_editor_edit_switch_layers), onClick = { onEdit(ClickEvent.Type.SwitchLayer) })
    InfoLayoutTextItem(title = stringResource(R.string.control_editor_edit_show_layers), onClick = { onEdit(ClickEvent.Type.ShowLayer) })
    InfoLayoutTextItem(title = stringResource(R.string.control_editor_edit_hide_layers), onClick = { onEdit(ClickEvent.Type.HideLayer) })
}
