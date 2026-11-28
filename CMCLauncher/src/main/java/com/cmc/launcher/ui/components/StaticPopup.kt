package com.cmc.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Composable
fun StaticDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        val view = LocalView.current
        DisposableEffect(view) {
            (view.parent as? DialogWindowProvider)?.window?.setWindowAnimations(0)
            onDispose {}
        }
        content()
    }
}

@Composable
fun StaticDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return
    val density = LocalDensity.current
    val height = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val margin = with(density) { 8.dp.roundToPx() }
    val dx = with(density) { offset.x.roundToPx() }
    val dy = with(density) { offset.y.roundToPx() }
    val position = remember(margin, dx, dy) { StaticMenuPosition(margin, dx, dy) }
    Popup(popupPositionProvider = position, onDismissRequest = onDismissRequest, properties = properties) {
        Surface(modifier = modifier, shape = shape, color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.widthIn(min = 168.dp, max = 320.dp).heightIn(max = (height - 32.dp).coerceAtLeast(48.dp))
                .verticalScroll(scrollState).padding(vertical = 4.dp), content = content)
        }
    }
}

private class StaticMenuPosition(private val margin: Int, private val dx: Int, private val dy: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val requestedX = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left + dx
            else anchorBounds.right - popupContentSize.width - dx
        val below = anchorBounds.bottom + dy
        val requestedY = if (below + popupContentSize.height <= windowSize.height - margin) below
            else anchorBounds.top - popupContentSize.height - dy
        return IntOffset(
            requestedX.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)),
            requestedY.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
        )
    }
}