package com.cmc.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.cardColor
import com.cmc.launcher.ui.theme.onCardColor
import com.cmc.launcher.utils.string.toSingleLine

@Composable
fun SimpleTextInputField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    hint: (@Composable () -> Unit)? = null,
    influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground),
    contentColor: Color = onCardColor(),
    shape: Shape = FieldShape,
    blur: Int = AllSettings.backgroundBlur.state,
    contextPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = contentColor),
    cursorBrush: Brush = SolidColor(LocalTextSelectionColors.current.handleColor),
    singleLine: Boolean = false,
    imePanEnabled: Boolean = true
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focus = LocalFocusManager.current
    BasicTextField(
        modifier = if (imePanEnabled) modifier.imePanAnchor() else modifier,
        value = value,
        onValueChange = { onValueChange(if (singleLine) it.toSingleLine() else it) },
        singleLine = singleLine,
        textStyle = textStyle.copy(color = contentColor),
        cursorBrush = cursorBrush,
        interactionSource = source,
        keyboardOptions = if (singleLine) KeyboardOptions(imeAction = ImeAction.Done) else KeyboardOptions.Default,
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        decorationBox = { field ->
            Surface(
                shape = shape,
                color = color,
                contentColor = contentColor,
                border = BorderStroke(if (focused) 2.dp else 1.dp,
                    if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(Modifier.fillMaxWidth().heightIn(min = Size.touch).padding(contextPadding), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) hint?.invoke()
                    field()
                }
            }
        }
    )
}

private class SimpleTextFieldLabelScope(override val labelMinimizedProgress: Float) : TextFieldLabelScope

@Composable
fun SmallOutlinedEditField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Attached(),
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    contentColor: Color? = null,
    shape: Shape = FieldShape,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    cursorBrush: Brush = SolidColor(LocalTextSelectionColors.current.handleColor),
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions? = null,
    singleLine: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    imePanEnabled: Boolean = true
) {
    val scope = remember { SimpleTextFieldLabelScope(1f) }
    StaticEditField(
        value, onValueChange, modifier, true, false, textStyle, contentColor,
        label = if (label != null) ({ label(scope) }) else null,
        placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
        prefix = null, suffix = null, supportingText = null, isError = false,
        visualTransformation = VisualTransformation.None, keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions, singleLine = singleLine,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE, minLines = 1,
        interactionSource = interactionSource, shape = shape, colors = colors,
        cursorBrush = cursorBrush, imePanEnabled = imePanEnabled
    )
}

@Composable
fun OwnOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = FieldShape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    imePanEnabled: Boolean = true
) {
    StaticEditField(
        value, onValueChange, modifier, enabled, readOnly, textStyle, null,
        label, placeholder, leadingIcon, trailingIcon, prefix, suffix, supportingText,
        isError, visualTransformation, keyboardOptions, keyboardActions, singleLine,
        maxLines, minLines, interactionSource, shape, colors,
        SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor), imePanEnabled
    )
}

@Composable
private fun StaticEditField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    readOnly: Boolean,
    textStyle: TextStyle,
    contentColor: Color?,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    prefix: @Composable (() -> Unit)?,
    suffix: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
    isError: Boolean,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions?,
    keyboardActions: KeyboardActions?,
    singleLine: Boolean,
    maxLines: Int,
    minLines: Int,
    interactionSource: MutableInteractionSource?,
    shape: Shape,
    colors: TextFieldColors,
    cursorBrush: Brush,
    imePanEnabled: Boolean
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focus = LocalFocusManager.current
    val stroke = when {
        !enabled -> colors.disabledIndicatorColor
        isError -> colors.errorIndicatorColor
        focused -> colors.focusedIndicatorColor
        else -> colors.unfocusedIndicatorColor
    }
    val foreground = contentColor ?: when {
        !enabled -> colors.disabledTextColor
        isError -> colors.errorTextColor
        focused -> colors.focusedTextColor
        else -> colors.unfocusedTextColor
    }
    val container = when {
        !enabled -> colors.disabledContainerColor
        isError -> colors.errorContainerColor
        focused -> colors.focusedContainerColor
        else -> colors.unfocusedContainerColor
    }
    val invalidText = androidx.compose.ui.res.stringResource(com.cmc.launcher.R.string.generic_error)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        if (label != null) {
            CompositionLocalProvider(LocalContentColor provides if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                LocalTextStyle provides MaterialTheme.typography.labelMedium) { label() }
        }
        BasicTextField(
            modifier = Modifier.fillMaxWidth()
                .then(if (imePanEnabled) Modifier.imePanAnchor() else Modifier)
                .semantics { if (isError) error(invalidText) },
            value = value,
            onValueChange = { onValueChange(if (singleLine) it.toSingleLine() else it) },
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.copy(color = foreground),
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = visualTransformation,
            cursorBrush = cursorBrush,
            interactionSource = source,
            keyboardOptions = keyboardOptions ?: if (singleLine) KeyboardOptions(imeAction = ImeAction.Done) else KeyboardOptions.Default,
            keyboardActions = keyboardActions ?: KeyboardActions(onDone = { focus.clearFocus() }),
            decorationBox = { field ->
                Surface(shape = shape, color = container, contentColor = foreground,
                    border = BorderStroke(if (focused) 2.dp else 1.dp, stroke)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = Size.touch).padding(Space.md),
                        verticalAlignment = Alignment.CenterVertically) {
                        if (leadingIcon != null) { leadingIcon(); Spacer(Modifier.width(Space.sm)) }
                        prefix?.invoke()
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty() && placeholder != null) {
                                CompositionLocalProvider(LocalContentColor provides colors.unfocusedPlaceholderColor) { placeholder() }
                            }
                            field()
                        }
                        suffix?.invoke()
                        if (trailingIcon != null) { Spacer(Modifier.width(Space.sm)); trailingIcon() }
                    }
                }
            }
        )
        if (supportingText != null) {
            CompositionLocalProvider(LocalContentColor provides if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                LocalTextStyle provides MaterialTheme.typography.bodySmall) { supportingText() }
        }
    }
}

@Composable
fun IntInputField(value: Int, permitted: IntRange, label: String, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    fun toText(v: Int) = v.takeIf { it > 0 }?.toString().orEmpty()
    var text by remember { mutableStateOf(toText(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!focused) text = toText(value) }
    OwnOutlinedTextField(
        modifier = modifier.onFocusChanged { state ->
            focused = state.isFocused
            if (!focused) text = toText(value)
        }, value = text,
        onValueChange = { input ->
            text = input.filter(Char::isDigit).take(6)
            text.toIntOrNull()?.takeIf { it > 0 }?.let { onValueChange(it.coerceIn(permitted)) }
        }, textStyle = MaterialTheme.typography.bodyMedium,
        label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
    )
}
