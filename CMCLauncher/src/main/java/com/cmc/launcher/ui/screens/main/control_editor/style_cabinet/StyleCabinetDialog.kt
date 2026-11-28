package com.cmc.launcher.ui.screens.main.control_editor.style_cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.game.control.CabinetStyleItem
import com.cmc.launcher.setting.enums.isLauncherInDarkTheme
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.screens.main.control_editor.EditorChoices
import com.cmc.launcher.ui.screens.main.control_editor.StyleLibraryRow
import com.cmc.launcher.ui.screens.main.control_editor.edit_joystick.JoystickStylePreview
import com.cmc.launcher.ui.screens.main.control_editor.edit_joystick.resolveThemeConfig
import com.cmc.launcher.ui.screens.main.control_editor.orUnspecified
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.layout.RendererStyleBox

private enum class CabinetFilter { ALL, BUTTON, JOYSTICK }

@Composable
fun StyleCabinetDialog(
    visible: Boolean, items: List<CabinetStyleItem>, onClose: () -> Unit,
    onEditItem: (CabinetStyleItem) -> Unit, onAddToLayout: (CabinetStyleItem) -> Unit,
    onDeleteItem: (CabinetStyleItem) -> Unit
) {
    if (!visible) return
    var filter by rememberSaveable { mutableStateOf(CabinetFilter.ALL) }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var deletePath by rememberSaveable { mutableStateOf<String?>(null) }
    val dark = isLauncherInDarkTheme()
    val buttonLabel = stringResource(R.string.control_editor_edit_category_style)
    val joystickLabel = stringResource(R.string.control_editor_edit_category_joystick_style)
    val shown = when (filter) {
        CabinetFilter.ALL -> items
        CabinetFilter.BUTTON -> items.filterIsInstance<CabinetStyleItem.Button>()
        CabinetFilter.JOYSTICK -> items.filterIsInstance<CabinetStyleItem.Joystick>()
    }
    DialogFrame(stringResource(R.string.control_editor_style_cabinet), onClose, actions = {
        TextButton(onClick = onClose) { Text(stringResource(R.string.generic_close)) }
    }) {
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            item {
                EditorChoices(listOf(stringResource(R.string.generic_all), buttonLabel, joystickLabel), filter.ordinal, {
                    filter = CabinetFilter.entries[it]
                    expanded = null
                })
            }
            if (shown.isEmpty()) item {
                Text(stringResource(R.string.control_editor_style_cabinet_empty), style = MaterialTheme.typography.bodyMedium)
            }
            items(shown, key = { it.file.absolutePath }) { entry ->
                val path = entry.file.absolutePath
                StyleLibraryRow(
                    name = entry.name.orUnspecified(), expanded = expanded == path,
                    onExpand = { expanded = if (expanded == path) null else path },
                    onOpen = { onEditItem(entry) }, subtitle = if (entry is CabinetStyleItem.Button) buttonLabel else joystickLabel,
                    preview = {
                        when (entry) {
                            is CabinetStyleItem.Button -> RendererStyleBox(modifier = Modifier.fillMaxSize(), style = entry.style,
                                text = "Aa", isPressed = false, isDark = dark)
                            is CabinetStyleItem.Joystick -> JoystickStylePreview(resolveThemeConfig(entry.style, dark), Modifier.fillMaxSize())
                        }
                    }
                ) {
                    TextButton(onClick = { expanded = null; onAddToLayout(entry) }) {
                        Text(stringResource(R.string.control_editor_style_cabinet_add_to_layout))
                    }
                    TextButton(onClick = { expanded = null; deletePath = path }) {
                        Text(stringResource(R.string.generic_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    items.firstOrNull { it.file.absolutePath == deletePath }?.let { target ->
        SimpleAlertDialog(
            title = stringResource(R.string.generic_delete),
            text = stringResource(R.string.control_editor_style_cabinet_delete, target.name.orUnspecified()),
            confirmText = stringResource(R.string.generic_delete),
            onConfirm = { deletePath = null; onDeleteItem(target) }, onDismiss = { deletePath = null }
        )
    }
}
