package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.main.control_editor.EditorChoices
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutListItem
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSliderItem
import com.cmc.launcher.ui.screens.main.control_editor.getVisibilityText
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.data.JOYSTICK_MIN_SIZE_DP
import com.movtery.layer_controller.data.JOYSTICK_MIN_SIZE_PERCENTAGE
import com.movtery.layer_controller.data.MIN_SIZE_DP
import com.movtery.layer_controller.data.SIZE_PERCENTAGE_EDITOR
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.layer_controller.observable.ObservableNormalData
import com.movtery.layer_controller.observable.ObservableTextData
import com.movtery.layer_controller.observable.ObservableWidget
import kotlin.math.roundToInt

@Composable
fun EditWidgetInfo(screenKey: TitledNavKey, currentKey: TitledNavKey?, data: ObservableWidget,
    onPreviewRequested: () -> Unit, onDismissRequested: () -> Unit) {
    if (screenKey != currentKey) return
    val pixels = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val width = with(density) { pixels.width.toDp().value }
    val height = with(density) { pixels.height.toDp().value }
    when (data) {
        is ObservableNormalData -> GeometryPanel(data.uuid, data.visibilityType, { data.visibilityType = it },
            data.position, { change -> data.position = change(data.position) }, onPreviewRequested, onDismissRequested) {
            RectDimensions(data.buttonSize, { change -> data.buttonSize = change(data.buttonSize) }, width, height,
                onPreviewRequested, onDismissRequested)
        }
        is ObservableTextData -> GeometryPanel(data.uuid, data.visibilityType, { data.visibilityType = it },
            data.position, { change -> data.position = change(data.position) }, onPreviewRequested, onDismissRequested) {
            RectDimensions(data.buttonSize, { change -> data.buttonSize = change(data.buttonSize) }, width, height,
                onPreviewRequested, onDismissRequested)
        }
        is ObservableJoystickData -> GeometryPanel(data.uuid, data.visibilityType, { data.visibilityType = it },
            data.position, { change -> data.position = change(data.position) }, onPreviewRequested, onDismissRequested) {
            DimensionMode(listOf(ButtonSize.Type.Dp, ButtonSize.Type.Percentage), data.sizeType) { data.sizeType = it }
            when (data.sizeType) {
                ButtonSize.Type.Dp -> Measure(R.string.control_editor_edit_size_joystick, data.sizeDp,
                    JOYSTICK_MIN_SIZE_DP..maxOf(JOYSTICK_MIN_SIZE_DP, height, data.sizeDp), false,
                    { data.sizeDp = it }, onPreviewRequested, onDismissRequested)
                ButtonSize.Type.Percentage -> Measure(R.string.control_editor_edit_size_joystick, data.sizePercentage / 100f,
                    (JOYSTICK_MIN_SIZE_PERCENTAGE / 100f)..100f, true, { data.sizePercentage = percentUnits(it) },
                    onPreviewRequested, onDismissRequested)
                ButtonSize.Type.WrapContent -> Unit
            }
        }
    }
}

private fun percentUnits(value: Float): Int = (value * 100f).roundToInt().coerceIn(0, 10000)

@Composable
private fun GeometryPanel(id: String, visibility: VisibilityType, setVisibility: (VisibilityType) -> Unit,
    position: ButtonPosition, move: ((ButtonPosition) -> ButtonPosition) -> Unit, preview: () -> Unit,
    finish: () -> Unit, dimensions: @Composable () -> Unit) {
    var tab by rememberSaveable(id) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        EditorChoices(listOf(stringResource(R.string.pe_widget_position), stringResource(R.string.pe_widget_dimensions)),
            tab, { tab = it }, Modifier.padding(Space.sm))
        key(id, tab) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (tab == 0) {
                    Measure(R.string.control_editor_edit_position_x, position.x / 100f, 0f..100f, true,
                        { x -> move { it.copy(x = percentUnits(x)) } }, preview, finish)
                    Measure(R.string.control_editor_edit_position_y, position.y / 100f, 0f..100f, true,
                        { y -> move { it.copy(y = percentUnits(y)) } }, preview, finish)
                    InfoLayoutListItem(title = stringResource(R.string.control_editor_edit_visibility),
                        items = VisibilityType.entries, selectedItem = visibility, onItemSelected = setVisibility,
                        getItemText = { it.getVisibilityText() })
                } else dimensions()
            }
        }
    }
}

@Composable
private fun DimensionMode(options: List<ButtonSize.Type>, current: ButtonSize.Type, change: (ButtonSize.Type) -> Unit) {
    Text(stringResource(R.string.control_editor_edit_size_type), style = MaterialTheme.typography.titleSmall)
    EditorChoices(options.map { stringResource(when (it) {
        ButtonSize.Type.Dp -> R.string.control_editor_edit_size_type_dp
        ButtonSize.Type.Percentage -> R.string.control_editor_edit_size_type_percentage
        ButtonSize.Type.WrapContent -> R.string.control_editor_edit_size_type_wrap_content
    }) }, options.indexOf(current), { change(options[it]) })
}

@Composable
private fun RectDimensions(size: ButtonSize, update: ((ButtonSize) -> ButtonSize) -> Unit, width: Float, height: Float,
    preview: () -> Unit, finish: () -> Unit) {
    DimensionMode(ButtonSize.Type.entries, size.type) { next -> update { it.copy(type = next) } }
    when (size.type) {
        ButtonSize.Type.Dp -> {
            Measure(R.string.control_editor_edit_size_width, size.widthDp, MIN_SIZE_DP..maxOf(MIN_SIZE_DP, width, size.widthDp),
                false, { v -> update { it.copy(widthDp = v) } }, preview, finish)
            Measure(R.string.control_editor_edit_size_height, size.heightDp, MIN_SIZE_DP..maxOf(MIN_SIZE_DP, height, size.heightDp),
                false, { v -> update { it.copy(heightDp = v) } }, preview, finish)
        }
        ButtonSize.Type.Percentage -> {
            Measure(R.string.control_editor_edit_size_width, size.widthPercentage / 100f, SIZE_PERCENTAGE_EDITOR, true,
                { v -> update { it.copy(widthPercentage = percentUnits(v)) } }, preview, finish)
            ReferenceField(R.string.control_editor_edit_size_width_reference, size.widthReference) { r -> update { it.copy(widthReference = r) } }
            Measure(R.string.control_editor_edit_size_height, size.heightPercentage / 100f, SIZE_PERCENTAGE_EDITOR, true,
                { v -> update { it.copy(heightPercentage = percentUnits(v)) } }, preview, finish)
            ReferenceField(R.string.control_editor_edit_size_height_reference, size.heightReference) { r -> update { it.copy(heightReference = r) } }
        }
        ButtonSize.Type.WrapContent -> Unit
    }
}

@Composable
private fun ReferenceField(label: Int, current: ButtonSize.Reference, change: (ButtonSize.Reference) -> Unit) {
    val refs = ButtonSize.Reference.entries
    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
    EditorChoices(refs.map { stringResource(when (it) {
        ButtonSize.Reference.ScreenWidth -> R.string.control_editor_edit_size_reference_screen_width
        ButtonSize.Reference.ScreenHeight -> R.string.control_editor_edit_size_reference_screen_height
    }) }, refs.indexOf(current), { change(refs[it]) })
}

@Composable
private fun Measure(label: Int, value: Float, range: ClosedFloatingPointRange<Float>, percent: Boolean,
    change: (Float) -> Unit, preview: () -> Unit, finish: () -> Unit) {
    InfoLayoutSliderItem(title = stringResource(label), value = value, valueRange = range, decimalFormat = "0.##",
        suffix = if (percent) "%" else " dp", onValueChange = { change(it.coerceIn(range)); preview() },
        onValueChangeFinished = finish)
}
