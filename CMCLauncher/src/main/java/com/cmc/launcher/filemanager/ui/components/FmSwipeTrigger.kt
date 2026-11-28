package com.cmc.launcher.filemanager.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@Stable
class FmSwipeTriggerState(val triggerPx: Float) {
    var offsetX by mutableFloatStateOf(0f)
        private set
    var isDragging by mutableStateOf(false)
        private set
    val dragFraction get() = (abs(offsetX) / triggerPx).coerceIn(0f, 1f)
    fun start() { offsetX = 0f; isDragging = true }
    fun move(delta: Float) { offsetX = (offsetX + delta).coerceIn(-triggerPx * 1.5f, triggerPx * 1.5f) }
    fun reset() { offsetX = 0f; isDragging = false }
}

@Composable
fun rememberFmSwipeTriggerState(triggerDistanceDp: Float = 80f): FmSwipeTriggerState {
    val px = with(LocalDensity.current) { triggerDistanceDp.dp.toPx() }.coerceAtLeast(1f)
    return remember(px) { FmSwipeTriggerState(px) }
}

fun Modifier.fmSwipeTrigger(state: FmSwipeTriggerState, triggerable: Boolean, onTriggered: () -> Unit): Modifier = composed {
    val trigger by rememberUpdatedState(onTriggered)
    this.offset { IntOffset(state.offsetX.roundToInt(), 0) }.pointerInput(state, triggerable) {
        try {
            detectHorizontalDragGestures(onDragStart = { state.start() }, onHorizontalDrag = { _, dx -> state.move(dx) },
                onDragCancel = state::reset, onDragEnd = {
                    val reached = abs(state.offsetX) >= state.triggerPx
                    state.reset()
                    if (triggerable && reached) trigger()
                })
        } finally { state.reset() }
    }
}
