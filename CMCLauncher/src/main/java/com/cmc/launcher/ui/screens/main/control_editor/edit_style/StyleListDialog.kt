package com.cmc.launcher.ui.screens.main.control_editor.edit_style

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.setting.enums.isLauncherInDarkTheme
import com.cmc.launcher.ui.screens.main.control_editor.StyleLibraryDialog
import com.cmc.launcher.ui.screens.main.control_editor.orUnspecified
import com.movtery.layer_controller.layout.RendererStyleBox
import com.movtery.layer_controller.observable.ObservableButtonStyle

@Composable
fun StyleListDialog(
    styles: List<ObservableButtonStyle>,
    onEditStyle: (ObservableButtonStyle) -> Unit,
    onCreate: () -> Unit,
    onClone: (ObservableButtonStyle) -> Unit,
    onDelete: (ObservableButtonStyle) -> Unit,
    onFavorite: (ObservableButtonStyle) -> Unit,
    onOpenCabinet: () -> Unit,
    onClose: () -> Unit
) {
    val dark = isLauncherInDarkTheme()
    StyleLibraryDialog(
        title = stringResource(R.string.control_editor_edit_style_config),
        empty = stringResource(R.string.control_editor_edit_style_config_empty),
        entries = styles, id = { it.uuid }, name = { it.name.orUnspecified() },
        preview = { RendererStyleBox(modifier = Modifier.fillMaxSize(), style = it, text = "Aa", isPressed = false, isDark = dark) },
        edit = onEditStyle, clone = onClone, favorite = onFavorite, delete = onDelete,
        create = onCreate, cabinet = onOpenCabinet, close = onClose
    )
}
