package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.setting.enums.isLauncherInDarkTheme
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.main.control_editor.orUnspecified
import com.movtery.layer_controller.layout.RendererStyleBox
import com.movtery.layer_controller.observable.ObservableButtonStyle
import com.movtery.layer_controller.observable.ObservableNormalData
import com.movtery.layer_controller.observable.ObservableTextData
import com.movtery.layer_controller.observable.ObservableWidget

@Composable
fun EditWidgetStyle(screenKey: TitledNavKey, currentKey: TitledNavKey?, data: ObservableWidget,
    styles: List<ObservableButtonStyle>, openStyleList: () -> Unit) {
    if (screenKey != currentKey) return
    val selected = when (data) {
        is ObservableNormalData -> data.buttonStyle
        is ObservableTextData -> data.buttonStyle
        else -> return
    }
    val dark = isLauncherInDarkTheme()
    WidgetStylePicker(entries = styles, selectedId = selected, change = { next ->
        when (data) {
            is ObservableNormalData -> data.buttonStyle = next
            is ObservableTextData -> data.buttonStyle = next
            else -> Unit
        }
    }, id = { it.uuid }, name = { it.name.orUnspecified() },
        sample = { RendererStyleBox(modifier = Modifier.fillMaxSize(), style = it, text = "Aa", isDark = dark, isPressed = false) },
        manageLabel = stringResource(R.string.control_editor_edit_style_config),
        emptyLabel = stringResource(R.string.control_editor_edit_style_config_empty), manage = openStyleList)
}
