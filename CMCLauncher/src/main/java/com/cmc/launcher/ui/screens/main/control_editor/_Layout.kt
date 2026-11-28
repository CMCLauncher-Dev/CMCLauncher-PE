package com.cmc.launcher.ui.screens.main.control_editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.cmc.launcher.ui.components.ColorPickerDialog
import com.cmc.launcher.ui.components.SimpleTextSlider
import com.cmc.launcher.ui.components.SliderValueEditDialog
import com.cmc.launcher.ui.theme.Alpha
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.itemColor
import com.cmc.launcher.ui.theme.onItemColor
import com.movtery.colorpicker.components.TransparentChecker
import com.movtery.colorpicker.rememberColorPickerController

@Composable
fun InfoLayoutSliderItem(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    decimalFormat: String = "#0.00",
    suffix: String? = null,
    fineTuningControl: Boolean = true,
    fineTuningStep: Float = 0.5f,
    enabled: Boolean = true,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor()
) {
    var editing by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth(), shape = FieldShape, color = color, contentColor = contentColor) {
        Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            SimpleTextSlider(
                modifier = Modifier.fillMaxWidth(), shorter = true, value = value,
                decimalFormat = decimalFormat, enabled = enabled, onValueChange = onValueChange,
                valueRange = valueRange, onValueChangeFinished = onValueChangeFinished,
                onTextClick = { editing = true }, suffix = suffix,
                fineTuningControl = fineTuningControl, fineTuningStep = fineTuningStep
            )
        }
    }
    if (editing) SliderValueEditDialog(
        onDismissRequest = { editing = false }, title = title, valueRange = valueRange,
        value = value, onValueChange = onValueChange,
        onValueChangeFinished = { onValueChangeFinished?.invoke() }
    )
}

@Composable
fun <E> InfoLayoutListItem(
    modifier: Modifier = Modifier,
    title: String,
    items: List<E>,
    selectedItem: E,
    onItemSelected: (E) -> Unit,
    getItemText: @Composable (E) -> String,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
    maxListHeight: Dp = 200.dp
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth(), shape = FieldShape, color = color, contentColor = contentColor) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(
                    enabled = items.isNotEmpty(), interactionSource = null, indication = null,
                    role = Role.Button, onClick = { expanded = !expanded }
                ).heightIn(min = Size.touch).padding(Space.md),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                    Text(getItemText(selectedItem), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (items.isNotEmpty()) Icon(
                    painterResource(if (expanded) R.drawable.ic_arrow_drop_up_rounded else R.drawable.ic_arrow_drop_down_rounded),
                    stringResource(if (expanded) R.string.generic_collapse else R.string.generic_expand), Modifier.size(Size.iconSm)
                )
            }
            if (expanded && items.isNotEmpty()) {
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = maxListHeight.coerceAtLeast(Size.touch)).selectableGroup(),
                    contentPadding = PaddingValues(Space.xs)
                ) {
                    items(items) { item ->
                        val selected = selectedItem == item
                        Row(
                            Modifier.fillMaxWidth().clip(FieldShape)
                                .selectable(selected, role = Role.RadioButton, interactionSource = null, indication = null,
                                    onClick = { if (!selected) onItemSelected(item); expanded = false })
                                .heightIn(min = Size.touch).padding(horizontal = Space.md, vertical = Space.sm),
                            horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(getItemText(item), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (selected) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(Size.iconSm),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoLayoutSwitchItem(
    modifier: Modifier = Modifier,
    title: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor()
) {
    Surface(
        modifier.fillMaxWidth().clip(FieldShape).toggleable(value, enabled = enabled, role = Role.Switch,
            interactionSource = null, indication = null, onValueChange = onValueChange),
        shape = FieldShape, color = color,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = Alpha.medium)
    ) {
        Row(Modifier.heightIn(min = Size.touch).padding(horizontal = Space.md, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(value, onCheckedChange = null, enabled = enabled)
        }
    }
}

@Composable
fun <E> InfoLayoutSelectItem(
    modifier: Modifier = Modifier,
    title: String,
    options: List<E>,
    current: E,
    onClick: (E) -> Unit,
    label: @Composable SingleChoiceSegmentedButtonRowScope.(E) -> Unit,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor()
) {
    Surface(modifier.fillMaxWidth(), shape = FieldShape, color = color, contentColor = contentColor) {
        Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (options.isNotEmpty()) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = current == option,
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            onClick = { onClick(option) }, label = { label(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoLayoutTextItem(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    selected: Boolean = false,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
    enabled: Boolean = true
) {
    InfoLayoutTextItem(modifier, title, icon = {
        if (showArrow) Icon(painterResource(R.drawable.ic_arrow_right_rounded), null, Modifier.size(Size.iconSm))
    }, onClick, selected, color, contentColor, enabled)
}

@Composable
fun InfoLayoutTextItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    selected: Boolean = false,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
    enabled: Boolean = true
) {
    InfoLayoutItem(modifier, onClick, selected, enabled, color = color, contentColor = contentColor) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        icon()
    }
}

@Composable
fun InfoLayoutColorItem(modifier: Modifier = Modifier, title: String, color: Color, onColorChanged: (Color) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val update by rememberUpdatedState(onColorChanged)
    InfoLayoutTextItem(modifier, title, icon = {
        Box(Modifier.size(30.dp).clip(FieldShape)) {
            TransparentChecker(Modifier.fillMaxSize(), gridSize = 12f)
            Box(Modifier.fillMaxSize().background(color))
        }
    }, onClick = { editing = true })
    if (editing) {
        val controller = rememberColorPickerController(initialColor = color)
        val current by controller.color
        LaunchedEffect(current) { update(current) }
        ColorPickerDialog(
            colorController = controller,
            onCancel = { update(controller.getOriginalColor()); editing = false },
            onConfirm = { update(it); editing = false }
        )
    }
}

@Composable
fun InfoLayoutItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = FieldShape,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    color: Color = itemColor(false),
    contentColor: Color = onItemColor(),
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier.fillMaxWidth().clip(shape).clickable(enabled = enabled, role = Role.Button,
            interactionSource = null, indication = null, onClick = onClick),
        color = color, contentColor = if (enabled) contentColor else contentColor.copy(alpha = Alpha.medium),
        shape = shape, border = if (selected) BorderStroke(Size.stroke, borderColor) else null
    ) {
        Row(Modifier.heightIn(min = Size.touch).padding(Space.md),
            horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
