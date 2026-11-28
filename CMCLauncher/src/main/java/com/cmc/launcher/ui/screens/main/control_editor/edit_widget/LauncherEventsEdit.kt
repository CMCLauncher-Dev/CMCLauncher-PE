package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN
import com.cmc.launcher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
import com.cmc.launcher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP
import com.cmc.launcher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP_SINGLE
import com.cmc.launcher.ui.control.event.LAUNCHER_EVENT_SWITCH_IME
import com.cmc.launcher.ui.control.event.LAUNCHER_EVENT_SWITCH_MENU
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSwitchItem
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutTextItem
import com.cmc.launcher.ui.theme.Space
import com.movtery.inputmap.keycodes.ControlEventKeycode
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.observable.ObservableClickEventsProvider

private val launcherSwitches = listOf(
    LAUNCHER_EVENT_SWITCH_IME to R.string.game_menu_option_input_method,
    LAUNCHER_EVENT_SWITCH_MENU to R.string.control_editor_edit_event_launcher_switch_menu,
    ControlEventKeycode.GLFW_MOUSE_BUTTON_LEFT to R.string.control_editor_edit_event_launcher_mouse_left,
    ControlEventKeycode.GLFW_MOUSE_BUTTON_MIDDLE to R.string.control_editor_edit_event_launcher_mouse_middle,
    ControlEventKeycode.GLFW_MOUSE_BUTTON_RIGHT to R.string.control_editor_edit_event_launcher_mouse_right,
    LAUNCHER_EVENT_SCROLL_UP to R.string.control_editor_edit_event_launcher_mouse_scroll_up,
    LAUNCHER_EVENT_SCROLL_UP_SINGLE to R.string.control_editor_edit_event_launcher_mouse_scroll_up_single,
    LAUNCHER_EVENT_SCROLL_DOWN to R.string.control_editor_edit_event_launcher_mouse_scroll_down,
    LAUNCHER_EVENT_SCROLL_DOWN_SINGLE to R.string.control_editor_edit_event_launcher_mouse_scroll_down_single
)

@Composable
fun LauncherEventsEdit(provider: ObservableClickEventsProvider, onSendText: () -> Unit, modifier: Modifier = Modifier) {
    val active = provider.clickEvents.filter { it.type == ClickEvent.Type.LauncherEvent }.mapTo(mutableSetOf()) { it.key }
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        item(key = "send-text") {
            InfoLayoutTextItem(title = stringResource(R.string.control_editor_edit_event_launcher_send_text), onClick = onSendText)
        }
        item(key = "divider") { HorizontalDivider() }
        items(launcherSwitches, key = { it.first }) { (code, label) ->
            InfoLayoutSwitchItem(title = stringResource(label), value = code in active, onValueChange = { enabled ->
                val event = ClickEvent(ClickEvent.Type.LauncherEvent, code)
                if (enabled && event !in provider.clickEvents) provider.onAddEvent(event)
                if (!enabled && event in provider.clickEvents) provider.onRemoveEvent(event)
            })
        }
    }
}
