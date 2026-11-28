package com.cmc.launcher.ui.screens.main.control_editor

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import com.cmc.launcher.setting.enums.isLauncherInDarkTheme
import com.cmc.launcher.ui.components.rememberBoxSize
import com.cmc.launcher.ui.control.mouse.SwitchableMouseLayout
import com.movtery.layer_controller.ControlBoxLayout
import com.movtery.layer_controller.data.HideLayerWhen
import com.movtery.layer_controller.observable.ObservableControlLayout

private class PreviewPointers {
    val occupied = mutableSetOf<PointerId>()
    val moveOnly = mutableSetOf<PointerId>()

    fun release(id: PointerId) {
        occupied.remove(id)
        moveOnly.remove(id)
    }

    fun clear() {
        occupied.clear()
        moveOnly.clear()
    }
}

@Composable
fun BoxWithConstraintsScope.PreviewControlBox(
    observableLayout: ObservableControlLayout,
    previewScenario: PreviewScenario,
    previewHideLayerWhen: HideLayerWhen,
    modifier: Modifier = Modifier
) {
    val screenSize = rememberBoxSize()
    key(observableLayout, previewScenario, previewHideLayerWhen) {
        val pointers = remember { PreviewPointers() }
        val lifecycle = Modifier.pointerInput(pointers) {
            try {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        for (change in event.changes) {
                            if (change.previousPressed && !change.pressed) pointers.release(change.id)
                        }
                        if (event.changes.none { it.pressed }) pointers.clear()
                    }
                }
            } finally {
                pointers.clear()
            }
        }
        ControlBoxLayout(
            modifier = modifier.fillMaxSize().then(lifecycle),
            observedLayout = observableLayout,
            checkOccupiedPointers = { it in pointers.occupied },
            markPointerAsMoveOnly = { pointers.moveOnly.add(it) },
            onOccupiedPointer = { pointers.occupied.add(it) },
            onReleasePointer = pointers::release,
            isCursorGrabbing = previewScenario.isCursorGrabbing,
            hideLayerWhen = previewHideLayerWhen,
            isDark = isLauncherInDarkTheme()
        ) {
            SwitchableMouseLayout(
                modifier = Modifier.fillMaxSize(), screenSize = screenSize, cursorMode = previewScenario.cursorMode,
                isMoveOnlyPointer = { it in pointers.moveOnly }, onOccupiedPointer = { pointers.occupied.add(it) },
                onReleasePointer = pointers::release
            )
        }
    }
}
