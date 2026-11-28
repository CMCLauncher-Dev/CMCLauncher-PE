package com.cmc.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R

@Composable
fun StaticBusyIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp
) {
    val label = stringResource(R.string.generic_loading)
    Canvas(modifier.size(24.dp).semantics {
        contentDescription = label
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    }) {
        val centerX = size.width / 2f
        val top = size.height * 0.18f
        val bottom = size.height * 0.82f
        val left = size.width * 0.27f
        val right = size.width * 0.73f
        val width = strokeWidth.toPx()
        drawLine(color, Offset(left, top), Offset(right, top), width, StrokeCap.Round)
        drawLine(color, Offset(left, bottom), Offset(right, bottom), width, StrokeCap.Round)
        drawLine(color, Offset(left, top), Offset(right, bottom), width, StrokeCap.Round)
        drawLine(color, Offset(right, top), Offset(left, bottom), width, StrokeCap.Round)
        drawCircle(color, width / 2f, Offset(centerX, size.height / 2f))
    }
}

@Composable
fun StaticProgressIndicator(
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    wavelength: Dp = 0.dp
) {
    val value = progress?.invoke()?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
    val label = stringResource(R.string.generic_loading)
    Canvas(modifier.fillMaxWidth().height(4.dp).semantics {
        progressBarRangeInfo = value?.let { ProgressBarRangeInfo(it, 0f..1f) } ?: ProgressBarRangeInfo.Indeterminate
        if (value == null) contentDescription = label
    }) {
        drawRoundRect(trackColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
        if (value != null) {
            if (value > 0f) drawRoundRect(color, size = size.copy(width = size.width * value),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
        } else {
            val radius = size.height / 2f
            for (fraction in listOf(0.25f, 0.5f, 0.75f)) drawCircle(color, radius, Offset(size.width * fraction, radius))
        }
    }
}