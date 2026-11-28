package com.cmc.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.ui.kit.CmcSegmented
import com.cmc.launcher.ui.screens.content.elements.DisabledAlpha
import com.cmc.launcher.ui.theme.CardShape
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.itemColor
import com.cmc.launcher.ui.theme.onItemColor
import java.text.DecimalFormat

enum class MenuState {
    NONE, SHOW, HIDE;
    fun next(): MenuState = if (this == SHOW) HIDE else SHOW
}

@Composable
fun MenuSubscreen(
    state: MenuState,
    closeScreen: () -> Unit,
    shape: Shape = CardShape,
    backgroundColor: Color = Color.Black.copy(alpha = 0.25f),
    content: @Composable ColumnScope.() -> Unit
) {
    if (state != MenuState.SHOW) return
    BackHandler(onBack = closeScreen)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        MenuScrim(backgroundColor, closeScreen)
        val width = (maxWidth - 24.dp).coerceAtMost(360.dp).coerceAtLeast(0.dp)
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp).width(width).fillMaxHeight(),
            shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) { Column(content = content) }
    }
}

@Composable
fun DualMenuSubscreen(
    state: MenuState,
    closeScreen: () -> Unit,
    shape: Shape = CardShape,
    backgroundColor: Color = Color.Black.copy(alpha = 0.25f),
    titleHeight: Dp = 48.dp,
    leftMenuTitle: (@Composable BoxScope.() -> Unit)? = null,
    leftMenuContent: @Composable ColumnScope.() -> Unit = {},
    rightMenuTitle: (@Composable BoxScope.() -> Unit)? = null,
    rightMenuContent: @Composable ColumnScope.() -> Unit = {}
) {
    if (state != MenuState.SHOW) return
    BackHandler(onBack = closeScreen)
    var right by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        MenuScrim(backgroundColor, closeScreen)
        if (maxWidth >= 720.dp) {
            val width = ((maxWidth - 36.dp) / 3).coerceAtMost(360.dp)
            MenuPanel(Modifier.align(Alignment.CenterStart).padding(12.dp).width(width).fillMaxHeight(),
                shape, titleHeight, leftMenuTitle, leftMenuContent)
            MenuPanel(Modifier.align(Alignment.CenterEnd).padding(12.dp).width(width).fillMaxHeight(),
                shape, titleHeight, rightMenuTitle, rightMenuContent)
        } else {
            Surface(Modifier.fillMaxSize().padding(12.dp), shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column {
                    CmcSegmented(
                        modifier = Modifier.padding(Space.sm),
                        options = listOf(stringResource(R.string.control_editor_menu_title), stringResource(R.string.control_editor_layers_title)),
                        selectedIndex = if (right) 1 else 0,
                        onSelect = { right = it == 1 }
                    )
                    val title = if (right) rightMenuTitle else leftMenuTitle
                    if (title != null) MenuTitleLayout(title, titleHeight)
                    if (right) rightMenuContent() else leftMenuContent()
                }
            }
        }
    }
}

@Composable
private fun MenuScrim(color: Color, close: () -> Unit) {
    Spacer(Modifier.fillMaxSize().background(color)
        .clickable(interactionSource = null, indication = null, onClick = close))
}

@Composable
private fun MenuPanel(
    modifier: Modifier,
    shape: Shape,
    titleHeight: Dp,
    title: (@Composable BoxScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier, shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            if (title != null) MenuTitleLayout(title, titleHeight)
            content()
        }
    }
}

@Composable
private fun MenuTitleLayout(titleLayout: @Composable BoxScope.() -> Unit, height: Dp = 48.dp) {
    Box(Modifier.fillMaxWidth().heightIn(min = height).padding(horizontal = Space.sm), content = titleLayout)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun MenuTextButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    influencedByBackground: Boolean = false,
    shape: Shape = FieldShape,
    color: Color = itemColor(influencedByBackground),
    contentColor: Color = onItemColor(),
    appendLayout: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    MenuButtonLayout(modifier, enabled, influencedByBackground, shape, color, contentColor,
        onClick = onClick, verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f).padding(horizontal = Space.md, vertical = Space.md),
            style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        appendLayout?.invoke()
    }
}

@Composable
fun MenuSwitchButton(
    modifier: Modifier = Modifier,
    text: String,
    switch: Boolean,
    onSwitch: (Boolean) -> Unit,
    enabled: Boolean = true,
    influencedByBackground: Boolean = false,
    shape: Shape = FieldShape,
    color: Color = itemColor(influencedByBackground),
    contentColor: Color = onItemColor()
) {
    MenuButtonLayout(modifier, enabled, influencedByBackground, shape, color, contentColor,
        onClick = { onSwitch(!switch) }, verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f).padding(Space.md), style = MaterialTheme.typography.bodyMedium,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        DefaultSwitch(switch, onSwitch, Modifier.padding(end = Space.sm), enabled)
    }
}

@Composable
fun <E> MenuListLayout(
    modifier: Modifier = Modifier,
    title: String,
    items: List<E>,
    currentItem: E,
    onItemChange: (E) -> Unit,
    getItemText: @Composable (E) -> String,
    selectedItemLayout: @Composable ColumnScope.(E) -> Unit = { Text(getItemText(it), style = MaterialTheme.typography.bodySmall) },
    enabled: Boolean = true,
    maxListHeight: Dp = 200.dp,
    influencedByBackground: Boolean = false,
    shape: Shape = FieldShape,
    color: Color = itemColor(influencedByBackground),
    contentColor: Color = onItemColor()
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(modifier, shape = shape, color = color, contentColor = contentColor) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(enabled = enabled && items.isNotEmpty(),
                    interactionSource = null, indication = null, role = Role.Button, onClick = { expanded = !expanded })
                    .heightIn(min = Size.touch).padding(Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                    selectedItemLayout(currentItem)
                }
                if (items.isNotEmpty()) Icon(
                    painterResource(if (expanded) R.drawable.ic_arrow_drop_up_rounded else R.drawable.ic_arrow_drop_down_rounded),
                    stringResource(if (expanded) R.string.generic_collapse else R.string.generic_expand), Modifier.size(Size.iconSm)
                )
            }
            if (enabled && expanded) {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxListHeight).selectableGroup(),
                    contentPadding = PaddingValues(Space.xs)) {
                    items(items) { item ->
                        val selected = currentItem == item
                        Row(
                            Modifier.fillMaxWidth().clip(FieldShape)
                                .selectable(selected, role = Role.RadioButton, interactionSource = null, indication = null,
                                    onClick = { if (!selected) onItemChange(item); expanded = false })
                                .heightIn(min = Size.touch).padding(horizontal = Space.md, vertical = Space.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(getItemText(item), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (selected) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(Size.iconSm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuSliderLayout(
    modifier: Modifier = Modifier,
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    onValueChangeFinished: (Int) -> Unit = {},
    suffix: String? = null,
    influencedByBackground: Boolean = false,
    colors: SliderColors = SliderDefaults.colors(),
    shape: Shape = FieldShape,
    color: Color = itemColor(influencedByBackground),
    contentColor: Color = onItemColor()
) {
    MenuSliderContent(modifier, title, value.toFloat(), { onValueChange(it.toInt()) }, valueRange, enabled,
        { onValueChangeFinished(it.toInt()) }, suffix, "#0", colors, shape, color, contentColor, true)
}

@Composable
fun MenuSliderLayout(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    onValueChangeFinished: (Float) -> Unit = {},
    suffix: String? = null,
    decimalFormat: String = "#0.00",
    influencedByBackground: Boolean = false,
    colors: SliderColors = SliderDefaults.colors(),
    shape: Shape = FieldShape,
    color: Color = itemColor(influencedByBackground),
    contentColor: Color = onItemColor()
) {
    MenuSliderContent(modifier, title, value, onValueChange, valueRange, enabled,
        onValueChangeFinished, suffix, decimalFormat, colors, shape, color, contentColor, false)
}

@Composable
private fun MenuSliderContent(
    modifier: Modifier, title: String, value: Float, onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>, enabled: Boolean, finished: (Float) -> Unit,
    suffix: String?, format: String, colors: SliderColors, shape: Shape, color: Color, contentColor: Color, integer: Boolean
) {
    var editing by remember { mutableStateOf(false) }
    val formatter = remember(format) { DecimalFormat(format) }
    Surface(modifier, shape = shape, color = color, contentColor = contentColor) {
        Column(Modifier.padding(Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(formatter.format(value) + suffix.orEmpty(),
                    Modifier.clickable(enabled = enabled, interactionSource = null, indication = null,
                        role = Role.Button, onClick = { editing = true }).padding(Space.sm),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            IndicatorSlider(value = value, onValueChange = onValueChange, valueRange = range,
                enabled = enabled, onValueChangeFinished = { finished(value) }, colors = colors)
        }
    }
    if (editing) {
        SliderValueEditDialog(
            title = title, value = value, valueRange = range, intCheck = integer,
            onDismissRequest = { editing = false },
            onValueChange = { next -> onValueChange(next); finished(next) }
        )
    }
}

@Composable
fun MenuButtonLayout(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    influencedByBackground: Boolean = false,
    shape: Shape = FieldShape,
    color: Color = itemColor(influencedByBackground),
    contentColor: Color = onItemColor(),
    blur: Int = AllSettings.backgroundBlur.state,
    onClick: () -> Unit = {},
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.clip(shape).clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onClick),
        shape = shape, color = color,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = DisabledAlpha)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = Size.touch),
            horizontalArrangement = horizontalArrangement, verticalAlignment = verticalAlignment, content = content)
    }
}
