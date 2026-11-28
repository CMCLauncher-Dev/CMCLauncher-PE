package com.cmc.launcher.ui.screens.main.control_editor.edit_joystick

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.setting.enums.isLauncherInDarkTheme
import com.cmc.launcher.ui.screens.main.control_editor.StyleLibraryDialog
import com.cmc.launcher.ui.screens.main.control_editor.orUnspecified
import com.movtery.layer_controller.observable.ObservableJoystickStyle

@Composable
fun JoystickStyleListDialog(
    styles: List<ObservableJoystickStyle>,
    onEditStyle: (ObservableJoystickStyle) -> Unit,
    onCreate: () -> Unit,
    onClone: (ObservableJoystickStyle) -> Unit,
    onDelete: (ObservableJoystickStyle) -> Unit,
    onFavorite: (ObservableJoystickStyle) -> Unit,
    onOpenCabinet: () -> Unit,
    onClose: () -> Unit
) {
    val dark = isLauncherInDarkTheme()
    StyleLibraryDialog(
        title = stringResource(R.string.control_editor_edit_joystick_style_list),
        empty = stringResource(R.string.control_editor_edit_joystick_style_list_empty),
        entries = styles, id = { it.uuid }, name = { it.name.orUnspecified() },
        preview = { JoystickStylePreview(modifier = Modifier.fillMaxSize(), config = resolveThemeConfig(it, dark)) },
        edit = onEditStyle, clone = onClone, favorite = onFavorite, delete = onDelete,
        create = onCreate, cabinet = onOpenCabinet, close = onClose
    )
}
