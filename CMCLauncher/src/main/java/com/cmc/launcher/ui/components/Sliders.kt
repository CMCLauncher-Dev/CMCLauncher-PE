package com.cmc.launcher.ui.components

import androidx.annotation.IntRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.utils.math.addBigDecimal
import com.cmc.launcher.utils.math.subtractBigDecimal
import java.text.DecimalFormat

@Composable
private fun defaultSliderColors(): SliderColors = SliderDefaults.colors()

@Composable
fun SimpleTextSlider(
    modifier: Modifier = Modifier,
    shorter: Boolean = false,
    value: Float,
    decimalFormat: String = "#0.00",
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    toInt: Boolean = false,
    suffix: String? = null,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    onTextClick: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    fineTuningControl: Boolean = false,
    fineTuningStep: Float = 0.5f,
    valueText: ((Float) -> String)? = null,
    appendContent: @Composable () -> Unit = {}
) {
    val formatter = remember(decimalFormat) { DecimalFormat(decimalFormat) }
    val displayValue = value.coerceIn(valueRange)
    val formatted = valueText?.invoke(displayValue)
        ?: if (toInt) displayValue.toInt().toString() else formatter.format(displayValue)
    val label = formatted + suffix.orEmpty()
    fun nudge(increase: Boolean) {
        val next = if (increase) displayValue.addBigDecimal(fineTuningStep)
            else displayValue.subtractBigDecimal(fineTuningStep)
        val result = next.coerceIn(valueRange)
        if (result != displayValue) {
            onValueChange(result)
            onValueChangeFinished?.invoke()
        }
    }

    val editLabel = stringResource(R.string.generic_edit)
    val decreaseLabel = stringResource(R.string.pe_value_less)
    val increaseLabel = stringResource(R.string.pe_value_more)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (shorter) 0.dp else Space.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            appendContent()
            Spacer(Modifier.weight(1f))
            if (fineTuningControl) {
                IconButton(
                    enabled = enabled && displayValue > valueRange.start,
                    onClick = { nudge(false) },
                    modifier = Modifier.semantics { contentDescription = decreaseLabel }
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .then(if (onTextClick != null) Modifier.clickable(
                        enabled = enabled, role = Role.Button, onClickLabel = editLabel,
                        interactionSource = null, indication = null, onClick = onTextClick
                    ) else Modifier)
                    .padding(horizontal = Space.sm, vertical = Space.md),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (fineTuningControl) {
                IconButton(
                    enabled = enabled && displayValue < valueRange.endInclusive,
                    onClick = { nudge(true) },
                    modifier = Modifier.semantics { contentDescription = increaseLabel }
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = displayValue,
            onValueChange = onValueChange,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = defaultSliderColors()
        )
    }
}

@Composable
fun IndicatorSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @IntRange(from = 0) steps: Int = 0,
    colors: SliderColors = defaultSliderColors()
) {
    Slider(
        modifier = modifier.fillMaxWidth(),
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        colors = colors
    )
}
