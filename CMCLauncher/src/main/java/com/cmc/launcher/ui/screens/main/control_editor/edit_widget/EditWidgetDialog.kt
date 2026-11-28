package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import com.cmc.launcher.ui.components.StaticDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.ui.screens.content.elements.CategoryItem
import com.cmc.launcher.ui.screens.main.control_editor.orUnspecified
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.PanelShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.observable.ObservableButtonStyle
import com.movtery.layer_controller.observable.ObservableClickEventsProvider
import com.movtery.layer_controller.observable.ObservableControlLayer
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.layer_controller.observable.ObservableJoystickStyle
import com.movtery.layer_controller.observable.ObservableNormalData
import com.movtery.layer_controller.observable.ObservableTextData
import com.movtery.layer_controller.observable.ObservableTranslatableString
import com.movtery.layer_controller.observable.ObservableWidget

@Composable
fun EditWidgetDialog(
    visible: Boolean,
    data: SelectedWidgetData?,
    styles: List<ObservableButtonStyle>,
    joystickStyles: List<ObservableJoystickStyle>,
    onDismissRequest: () -> Unit,
    onDelete: (ObservableWidget, ObservableControlLayer) -> Unit,
    onClone: (ObservableWidget, ObservableControlLayer) -> Unit,
    onEditWidgetText: (ObservableTranslatableString) -> Unit,
    switchControlLayers: (ObservableClickEventsProvider, ClickEvent.Type) -> Unit,
    sendText: (ObservableClickEventsProvider) -> Unit,
    openStyleList: () -> Unit,
    openJoystickStyleList: () -> Unit,
    onEditJoystickEvents: () -> Unit,
) {
    if (!visible || data == null) return
    val widget = data.data
    val widgetId = when (widget) {
        is ObservableJoystickData -> widget.uuid
        is ObservableNormalData -> widget.uuid
        is ObservableTextData -> widget.uuid
        else -> data.layer.uuid
    }
    val tabs = remember(widget) {
        when (widget) {
            is ObservableJoystickData -> editJoystickCategories
            is ObservableNormalData -> editWidgetCategories
            else -> editWidgetCategories.filterNot { it.key == EditWidgetCategory.ClickEvent }
        }
    }
    key(data.layer.uuid, widgetId) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var preview by rememberSaveable { mutableStateOf(false) }
        var dragging by remember { mutableStateOf(false) }
        val current = tabs.getOrNull(tab) ?: tabs.first()
        val select: (Int) -> Unit = { tab = it; dragging = false }
        val transparent = preview || dragging
        BackHandler(onBack = onDismissRequest)
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val rail = maxWidth >= 700.dp
            Spacer(Modifier.matchParentSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = if (transparent) 0f else 0.24f))
                .clickable(interactionSource = null, indication = null, onClick = onDismissRequest))
            Surface(
                modifier = Modifier.widthIn(max = 1040.dp).fillMaxSize()
                    .padding(if (rail) Space.lg else Space.xs).alpha(if (transparent) 0.35f else 1f),
                shape = PanelShape, color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Space.sm), verticalAlignment = Alignment.CenterVertically) {
                        if (rail) {
                            Text(data.layer.name.orUnspecified(), Modifier.weight(1f).padding(start = Space.sm),
                                style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Box(Modifier.weight(1f)) {
                                var open by remember { mutableStateOf(false) }
                                Row(Modifier.fillMaxWidth().clip(FieldShape).clickable(role = Role.Button) { open = true }
                                    .heightIn(min = Size.touch).padding(horizontal = Space.sm),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(current.textRes), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Icon(painterResource(R.drawable.ic_arrow_drop_down_rounded), null, Modifier.size(Size.iconSm))
                                }
                                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                    tabs.forEachIndexed { index, item ->
                                        DropdownMenuItem(text = { Text(stringResource(item.textRes)) }, leadingIcon = item.icon,
                                            onClick = { open = false; select(index) })
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { preview = !transparent; dragging = false }) {
                            Icon(painterResource(if (transparent) R.drawable.ic_visibility_off_outlined else R.drawable.ic_visibility_outlined),
                                stringResource(if (transparent) R.string.control_editor_edit_dialog_close_preview else R.string.control_editor_edit_dialog_open_preview))
                        }
                        Box {
                            var open by remember { mutableStateOf(false) }
                            IconButton(onClick = { open = true }) {
                                Icon(painterResource(R.drawable.ic_more_horiz), stringResource(R.string.generic_more))
                            }
                            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.control_editor_edit_dialog_clone_widget)) },
                                    onClick = { open = false; onClone(widget, data.layer) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.generic_delete), color = MaterialTheme.colorScheme.error) },
                                    onClick = { open = false; onDelete(widget, data.layer) })
                            }
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.generic_close))
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        if (rail) EditorCategories(tabs, current, select, Modifier.width(176.dp).fillMaxHeight())
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            when (val page = current.key) {
                                EditWidgetCategory.Info -> EditWidgetInfo(page, page, widget, { dragging = true }, { dragging = false })
                                EditWidgetCategory.TextStyle -> EditTextStyle(page, page, widget, onEditWidgetText)
                                EditWidgetCategory.Style -> EditWidgetStyle(page, page, widget, styles, openStyleList)
                                EditWidgetCategory.ClickEvent -> if (widget is ObservableNormalData) {
                                    EditWidgetClickEvent(page, page, widget, switchControlLayers, sendText)
                                }
                                EditWidgetCategory.JoystickConfig -> if (widget is ObservableJoystickData) EditJoystickConfig(page, page, widget)
                                EditWidgetCategory.JoystickStyle -> if (widget is ObservableJoystickData) {
                                    EditJoystickStyle(page, page, widget, joystickStyles, openJoystickStyleList)
                                }
                                EditWidgetCategory.DirectionEvents -> if (widget is ObservableJoystickData) {
                                    LaunchedEffect(widgetId) { onEditJoystickEvents() }
                                    EditJoystickEvents(widget, switchControlLayers, sendText)
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorCategories(tabs: List<CategoryItem>, current: CategoryItem, select: (Int) -> Unit, modifier: Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainer).verticalScroll(rememberScrollState())
        .selectableGroup().padding(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        tabs.forEachIndexed { index, item ->
            val selected = current.key == item.key
            Row(Modifier.fillMaxWidth().clip(FieldShape)
                .then(if (selected) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest) else Modifier)
                .selectable(selected = selected, role = Role.Tab, onClick = { select(index) })
                .heightIn(min = Size.touch).padding(horizontal = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                item.icon()
                Text(stringResource(item.textRes), style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
