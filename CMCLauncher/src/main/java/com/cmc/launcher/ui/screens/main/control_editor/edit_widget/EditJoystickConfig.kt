package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.main.control_editor.EditorChoices
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSliderItem
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSwitchItem
import com.cmc.launcher.ui.screens.main.control_editor.getTriggerModeText
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.data.JOYSTICK_DEAD_ZONE_RANGE
import com.movtery.layer_controller.data.JOYSTICK_LOCK_THRESHOLD_RANGE
import com.movtery.layer_controller.data.JoystickTriggerMode
import com.movtery.layer_controller.observable.ObservableJoystickData

@Composable
fun EditJoystickConfig(screenKey: TitledNavKey, currentKey: TitledNavKey?, data: ObservableJoystickData) {
    if (screenKey != currentKey) return
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        item(key = "trigger") {
            val modes = JoystickTriggerMode.entries
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(R.string.control_editor_edit_joystick_trigger_mode), style = MaterialTheme.typography.titleSmall)
                EditorChoices(modes.map { it.getTriggerModeText() }, modes.indexOf(data.triggerMode), { data.triggerMode = modes[it] })
            }
        }
        item(key = "dead-zone") {
            RatioField(stringResource(R.string.control_editor_edit_joystick_dead_zone), data.deadZoneRatio,
                JOYSTICK_DEAD_ZONE_RANGE, { data.deadZoneRatio = it })
        }
        item { HorizontalDivider() }
        item(key = "lock") {
            InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_edit_joystick_can_lock),
                value = data.canLock, onValueChange = { data.canLock = it })
        }
        if (data.canLock) item(key = "threshold") {
            RatioField(stringResource(R.string.control_editor_edit_joystick_lock_threshold), data.lockThreshold,
                JOYSTICK_LOCK_THRESHOLD_RANGE, { data.lockThreshold = it })
        }
    }
}

@Composable
private fun RatioField(label: String, ratio: Float, range: ClosedFloatingPointRange<Float>, change: (Float) -> Unit) {
    InfoLayoutSliderItem(title = label, value = ratio * 100f,
        valueRange = (range.start * 100f)..(range.endInclusive * 100f), decimalFormat = "0.##",
        suffix = "%", fineTuningStep = 1f, onValueChange = { change((it / 100f).coerceIn(range)) })
}
