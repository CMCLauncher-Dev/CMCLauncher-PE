package com.cmc.launcher.ui.screens.main.control_editor.edit_joystick

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.setting.unit.toFloatRange
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutColorItem
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSliderItem
import com.cmc.launcher.ui.screens.main.control_editor.StyleEditorForm
import com.movtery.layer_controller.data.BORDER_RADIO_RANGE
import com.movtery.layer_controller.data.SHAPE_PERCENT_RANGE
import com.movtery.layer_controller.data.SIZE_PERCENT_RANGE
import com.movtery.layer_controller.layout.JoystickStyleWidget
import com.movtery.layer_controller.observable.ObservableJoystickStyle
import com.movtery.layer_controller.observable.ObservableJoystickStyleConfig

@Composable
fun EditJoystickStyleDialog(visible: Boolean, style: ObservableJoystickStyle?, onClose: () -> Unit) {
    if (!visible || style == null) return
    StyleEditorForm(
        id = style.uuid, title = stringResource(R.string.control_editor_edit_category_joystick_style),
        nameLabel = stringResource(R.string.control_editor_edit_joystick_style_list_name),
        name = style.name, onName = { style.name = it }, common = style.commonStyle,
        onCommon = { style.commonStyle = it }, close = onClose,
        preview = { dark ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                JoystickStyleWidget(modifier = Modifier.size(96.dp), widgetSize = 96.dp, isDarkTheme = dark, style = style)
            }
        }
    ) { dark ->
        joystickFields(if (dark) style.darkStyle else style.lightStyle, if (dark) "dark" else "light")
    }
}

private fun LazyListScope.joystickFields(config: ObservableJoystickStyleConfig, theme: String) {
    fun color(id: String, label: Int, read: () -> Color, write: (Color) -> Unit) {
        item(key = "$theme-$id") {
            InfoLayoutColorItem(title = stringResource(label), color = read(), onColorChanged = write)
        }
    }
    fun percent(id: String, label: Int, read: () -> Float, range: ClosedFloatingPointRange<Float>,
                step: Float, write: (Float) -> Unit) {
        item(key = "$theme-$id") {
            InfoLayoutSliderItem(title = stringResource(label), value = read(), valueRange = range,
                onValueChange = write, decimalFormat = if (step < 1f) "0.#" else "0",
                suffix = "%", fineTuningStep = step)
        }
    }
    percent("opacity", R.string.control_editor_edit_style_config_alpha, { config.alpha * 100f },
        0f..100f, 0.1f) { config.alpha = it / 100f }
    color("background", R.string.control_editor_edit_style_config_background_color, { config.backgroundColor }) { config.backgroundColor = it }
    color("stick", R.string.control_editor_special_joystick_style_joystick_color, { config.joystickColor }) { config.joystickColor = it }
    color("lock-ready", R.string.control_editor_special_joystick_style_joystick_can_lock_color, { config.joystickCanLockColor }) { config.joystickCanLockColor = it }
    color("locked", R.string.control_editor_special_joystick_style_joystick_locked_color, { config.joystickLockedColor }) { config.joystickLockedColor = it }
    color("lock-mark", R.string.control_editor_special_joystick_style_lock_mark_color, { config.lockMarkColor }) { config.lockMarkColor = it }
    color("outline", R.string.control_editor_edit_style_config_border_color, { config.borderColor }) { config.borderColor = it }
    percent("base-radius", R.string.control_editor_special_joystick_style_background_rounded_corner,
        { config.backgroundShape.toFloat() }, SHAPE_PERCENT_RANGE.toFloatRange(), 1f) { config.backgroundShape = it.toInt() }
    percent("outline-width", R.string.control_editor_edit_style_config_border_width,
        { config.borderWidthRatio.toFloat() }, BORDER_RADIO_RANGE.toFloatRange(), 1f) { config.borderWidthRatio = it.toInt() }
    percent("stick-radius", R.string.control_editor_special_joystick_style_joystick_rounded_corner,
        { config.joystickShape.toFloat() }, SHAPE_PERCENT_RANGE.toFloatRange(), 1f) { config.joystickShape = it.toInt() }
    percent("stick-size", R.string.control_editor_special_joystick_style_joystick_size, { config.joystickSize * 100f },
        (SIZE_PERCENT_RANGE.start * 100f)..(SIZE_PERCENT_RANGE.endInclusive * 100f), 1f) { config.joystickSize = it / 100f }
}
