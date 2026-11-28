package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.setting.enums.isLauncherInDarkTheme
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.main.control_editor.edit_joystick.JoystickStylePreview
import com.cmc.launcher.ui.screens.main.control_editor.edit_joystick.resolveThemeConfig
import com.cmc.launcher.ui.screens.main.control_editor.orUnspecified
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.layer_controller.observable.ObservableJoystickStyle

@Composable
fun EditJoystickStyle(screenKey: TitledNavKey, currentKey: TitledNavKey?, data: ObservableJoystickData,
    joystickStyles: List<ObservableJoystickStyle>, openJoystickStyleList: () -> Unit) {
    if (screenKey != currentKey) return
    val dark = isLauncherInDarkTheme()
    WidgetStylePicker(entries = joystickStyles, selectedId = data.joystickStyleId, change = { data.joystickStyleId = it },
        id = { it.uuid }, name = { it.name.orUnspecified() },
        sample = { JoystickStylePreview(resolveThemeConfig(it, dark), Modifier.fillMaxSize()) },
        manageLabel = stringResource(R.string.control_editor_edit_joystick_style_list),
        emptyLabel = stringResource(R.string.control_editor_edit_joystick_style_list_empty), manage = openJoystickStyleList)
}
