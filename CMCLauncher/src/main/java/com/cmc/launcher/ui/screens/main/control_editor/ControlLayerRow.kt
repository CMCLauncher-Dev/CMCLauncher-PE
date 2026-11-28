package com.cmc.launcher.ui.screens.main.control_editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.observable.ObservableControlLayer

@Composable
internal fun ControlLayerItem(
    modifier: Modifier = Modifier,
    layer: ObservableControlLayer,
    dragButtonModifier: Modifier,
    selected: Boolean,
    onSelected: () -> Unit,
    onUnSelected: () -> Unit,
    onAttribute: () -> Unit,
    onHideSwitch: () -> Unit,
    color: Color,
    contentColor: Color,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = FieldShape,
    shadowElevation: Dp = 0.dp,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = color,
        contentColor = contentColor,
        border = if (selected) BorderStroke(Size.stroke, borderColor) else null,
        shadowElevation = shadowElevation
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.xs),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onHideSwitch, enabled = enabled) {
                Icon(
                    painterResource(if (layer.editorHide) R.drawable.ic_visibility_off_outlined else R.drawable.ic_visibility_outlined),
                    stringResource(if (layer.editorHide) R.string.pe_layer_show else R.string.pe_layer_hide),
                    Modifier.size(Size.iconSm)
                )
            }
            Row(
                modifier = Modifier.weight(1f).clip(FieldShape)
                    .selectable(selected = selected, enabled = enabled, role = Role.RadioButton,
                        interactionSource = null, indication = null,
                        onClick = { if (selected) onUnSelected() else onSelected() })
                    .heightIn(min = Size.touch).padding(vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(layer.name.orUnspecified(), style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onAttribute, enabled = enabled) {
                Icon(painterResource(R.drawable.ic_more_horiz),
                    stringResource(R.string.control_editor_layers_attribute), Modifier.size(Size.iconSm))
            }
            Row(
                modifier = (if (enabled) dragButtonModifier else Modifier)
                    .size(Size.touch).clip(FieldShape),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_drag_handle), stringResource(R.string.pe_layer_reorder),
                    Modifier.size(Size.iconSm), tint = contentColor.copy(alpha = if (enabled) 0.8f else 0.38f))
            }
        }
    }
}
