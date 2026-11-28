package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutTextItem
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.guide.GuideKeys
import com.movtery.guide.guideNode
import com.movtery.layer_controller.data.JoystickDirection
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.observable.ObservableClickEventsProvider
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.layer_controller.observable.joystickDirectionEventsProvider
import com.movtery.layer_controller.observable.joystickLockEventsProvider

private enum class BindingArea(val symbol: String, val direction: JoystickDirection?) {
    NW("↖", JoystickDirection.NorthWest), N("↑", JoystickDirection.North), NE("↗", JoystickDirection.NorthEast),
    W("←", JoystickDirection.West), LOCK("◎", null), E("→", JoystickDirection.East),
    SW("↙", JoystickDirection.SouthWest), S("↓", JoystickDirection.South), SE("↘", JoystickDirection.SouthEast)
}

@Composable
fun EditJoystickEvents(
    data: ObservableJoystickData,
    switchControlLayers: (ObservableClickEventsProvider, ClickEvent.Type) -> Unit,
    sendText: (ObservableClickEventsProvider) -> Unit
) {
    val labels = stringArrayResource(R.array.pe_joystick_areas)
    var selected by rememberSaveable(data.uuid) { mutableIntStateOf(-1) }
    var editing by rememberSaveable(data.uuid) { mutableStateOf(false) }
    val area = BindingArea.entries.getOrNull(selected)
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.md), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.control_editor_edit_joystick_select_area), Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.widthIn(max = 320.dp).fillMaxWidth().selectableGroup()
                .guideNode(key = GuideKeys.JoystickEvents.Step.Direction, holeRadius = 16.dp),
                verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                for (row in 0..2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        for (column in 0..2) {
                            val index = row * 3 + column
                            val target = BindingArea.entries[index]
                            val active = index == selected
                            val caption = labels[index]
                            Box(Modifier.weight(1f).then(if (target == BindingArea.LOCK)
                                Modifier.guideNode(key = GuideKeys.JoystickEvents.Step.Lock, holeRadius = 12.dp) else Modifier)
                                .clip(FieldShape).background(if (active) colors.primaryContainer else colors.surfaceContainerHighest)
                                .selectable(selected = active, role = Role.RadioButton, interactionSource = null, indication = null,
                                    onClick = { selected = if (active) -1 else index })
                                .semantics { contentDescription = caption }.heightIn(min = Size.touch), contentAlignment = Alignment.Center) {
                                Text(target.symbol, Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.titleLarge,
                                    color = if (active) colors.onPrimaryContainer else colors.onSurface)
                            }
                        }
                    }
                }
            }
        }
        if (area != null) {
            InfoLayoutTextItem(modifier = Modifier.guideNode(key = GuideKeys.JoystickEvents.Step.Events, holeRadius = 12.dp),
                title = stringResource(R.string.generic_edit) + " · " + labels[area.ordinal], onClick = { editing = true })
        }
    }
    if (!editing || area == null) return
    val provider = remember(data, area) {
        val direction = area.direction
        if (direction == null) joystickLockEventsProvider(data) else joystickDirectionEventsProvider(data, direction)
    }
    DialogFrame(labels[area.ordinal], { editing = false }, actions = {
        TextButton(onClick = { editing = false }) { Text(stringResource(R.string.generic_close)) }
    }) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            EventCategories(data.uuid + ":" + area.name, provider, { editing = false; sendText(it) }) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.md),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    LayerEventActions { editing = false; switchControlLayers(provider, it) }
                }
            }
        }
    }
}
