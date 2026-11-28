package com.cmc.launcher.ui.screens.main.control_editor.edit_style

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.ui.screens.main.control_editor.EditorChoices
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutColorItem
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSliderItem
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSwitchItem
import com.cmc.launcher.ui.screens.main.control_editor.StyleEditorForm
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.data.BORDER_WIDTH
import com.movtery.layer_controller.data.ButtonShape
import com.movtery.layer_controller.data.DEFAULT_FONT_SIZE
import com.movtery.layer_controller.data.FONT_SIZE_RANGE
import com.movtery.layer_controller.data.SHAPE_RANGE
import com.movtery.layer_controller.layout.RendererStyleBox
import com.movtery.layer_controller.observable.ObservableButtonStyle

@Composable
fun EditButtonStyleDialog(visible: Boolean, style: ObservableButtonStyle?, onClose: () -> Unit) {
    if (!visible || style == null) return
    var pressed by rememberSaveable(style.uuid) { mutableStateOf(false) }
    val normalLabel = stringResource(R.string.control_editor_edit_style_config_normal)
    val pressedLabel = stringResource(R.string.control_editor_edit_style_config_pressed)
    StyleEditorForm(
        id = style.uuid, title = stringResource(R.string.control_editor_edit_category_style),
        nameLabel = stringResource(R.string.control_editor_edit_style_config_name),
        name = style.name, onName = { style.name = it }, common = style.commonStyle,
        onCommon = { style.commonStyle = it }, close = onClose,
        preview = { dark ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (down in listOf(false, true)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        RendererStyleBox(style = style, isDark = dark, isPressed = down, text = "Aa", modifier = Modifier.size(56.dp))
                        Text(if (down) pressedLabel else normalLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    ) { dark ->
        val config = if (dark) style.darkStyle else style.lightStyle
        item(key = "button-state") {
            EditorChoices(listOf(normalLabel, pressedLabel), if (pressed) 1 else 0, { pressed = it == 1 })
        }
        if (pressed) {
            commonStyleConfig(
                "pressed-$dark", Modifier.fillMaxWidth(), config.pressedAlpha, { config.pressedAlpha = it },
                config.pressedBackgroundColor, { config.pressedBackgroundColor = it },
                config.pressedContentColor, { config.pressedContentColor = it },
                config.pressedFontSize, { config.pressedFontSize = it }, config.pressedBorderWidth, { config.pressedBorderWidth = it },
                config.pressedBorderColor, { config.pressedBorderColor = it }, config.pressedBorderRadius, { config.pressedBorderRadius = it }
            )
        } else {
            commonStyleConfig(
                "normal-$dark", Modifier.fillMaxWidth(), config.alpha, { config.alpha = it },
                config.backgroundColor, { config.backgroundColor = it }, config.contentColor, { config.contentColor = it },
                config.fontSize, { config.fontSize = it }, config.borderWidth, { config.borderWidth = it },
                config.borderColor, { config.borderColor = it }, config.borderRadius, { config.borderRadius = it }
            )
        }
    }
}

private fun LazyListScope.commonStyleConfig(
    tag: String,
    itemModifier: Modifier,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    backgroundColor: Color,
    onBackgroundColorChange: (Color) -> Unit,
    contentColor: Color,
    onContentColorChange: (Color) -> Unit,
    textSize: Int?,
    onTextSizeChanged: (Int?) -> Unit,
    borderWidth: Int,
    onBorderWidthChange: (Int) -> Unit,
    borderColor: Color,
    onBorderColorChange: (Color) -> Unit,
    borderRadius: ButtonShape,
    onBorderRadiusChange: (ButtonShape) -> Unit
) {

    item(key = "opacity_$tag") {
        InfoLayoutSliderItem(
            modifier = itemModifier,
            title = stringResource(R.string.control_editor_edit_style_config_alpha),
            value = alpha * 100f,
            onValueChange = { onAlphaChange(it / 100f) },
            valueRange = 0f..100f,
            decimalFormat = "#0",
            suffix = "%",
            fineTuningStep = 0.1f
        )
    }

    item(key = "background_color_$tag") {
        InfoLayoutColorItem(
            modifier = itemModifier,
            title = stringResource(R.string.control_editor_edit_style_config_background_color),
            color = backgroundColor,
            onColorChanged = onBackgroundColorChange
        )
    }

    item(key = "content_color_$tag") {
        InfoLayoutColorItem(
            modifier = itemModifier,
            title = stringResource(R.string.control_editor_edit_style_config_content_color),
            color = contentColor,
            onColorChanged = onContentColorChange
        )
    }

    item(key = "custom_text_size_$tag") {
        InfoLayoutSwitchItem(
            modifier = itemModifier,
            title = stringResource(R.string.control_editor_edit_text_size_custom),
            value = textSize != null,
            onValueChange = { value ->
                if (value) {
                    onTextSizeChanged(DEFAULT_FONT_SIZE)
                } else {
                    onTextSizeChanged(null)
                }
            }
        )
    }

    if (textSize != null) {

        item(key = "text_size_$tag") {
            InfoLayoutSliderItem(
                modifier = itemModifier,
                title = stringResource(R.string.control_editor_edit_text_size),
                value = textSize.toFloat(),
                onValueChange = { value ->
                    onTextSizeChanged(value.toInt())
                },
                valueRange = FONT_SIZE_RANGE,
                decimalFormat = "#0",
                suffix = "sp",
                fineTuningStep = 1f,
            )
        }
    }

    item(key = "border_width_$tag") {
        InfoLayoutSliderItem(
            modifier = itemModifier,
            title = stringResource(R.string.control_editor_edit_style_config_border_width),
            value = borderWidth.toFloat(),
            onValueChange = { onBorderWidthChange(it.toInt()) },
            valueRange = BORDER_WIDTH,
            decimalFormat = "#0",
            suffix = "dp",
            fineTuningStep = 1f,
        )
    }

    item(key = "border_color_$tag") {
        InfoLayoutColorItem(
            modifier = itemModifier,
            title = stringResource(R.string.control_editor_edit_style_config_border_color),
            color = borderColor,
            onColorChanged = onBorderColorChange
        )
    }

    item(key = "corner_radius_text_$tag") {
        Text(
            modifier = Modifier,
            text = stringResource(R.string.control_editor_edit_style_config_widget_radius)
        )
    }

    item(key = "corner_radius_$tag") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                ,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            InfoLayoutSliderItem(
                modifier = itemModifier,
                title = stringResource(R.string.control_editor_edit_style_config_widget_radius_left_top),
                value = borderRadius.topStart,
                onValueChange = { onBorderRadiusChange(borderRadius.copy(topStart = it)) },
                valueRange = SHAPE_RANGE,
                suffix = "dp"
            )

            InfoLayoutSliderItem(
                modifier = itemModifier,
                title = stringResource(R.string.control_editor_edit_style_config_widget_radius_right_top),
                value = borderRadius.topEnd,
                onValueChange = { onBorderRadiusChange(borderRadius.copy(topEnd = it)) },
                valueRange = SHAPE_RANGE,
                suffix = "dp"
            )

            InfoLayoutSliderItem(
                modifier = itemModifier,
                title = stringResource(R.string.control_editor_edit_style_config_widget_radius_left_bottom),
                value = borderRadius.bottomStart,
                onValueChange = { onBorderRadiusChange(borderRadius.copy(bottomStart = it)) },
                valueRange = SHAPE_RANGE,
                suffix = "dp"
            )

            InfoLayoutSliderItem(
                modifier = itemModifier,
                title = stringResource(R.string.control_editor_edit_style_config_widget_radius_right_bottom),
                value = borderRadius.bottomEnd,
                onValueChange = { onBorderRadiusChange(borderRadius.copy(bottomEnd = it)) },
                valueRange = SHAPE_RANGE,
                suffix = "dp"
            )
        }
    }
}
