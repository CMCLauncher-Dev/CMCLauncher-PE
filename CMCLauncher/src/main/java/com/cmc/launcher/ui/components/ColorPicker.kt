package com.cmc.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Space
import com.movtery.colorpicker.ColorPickerController
import com.movtery.colorpicker.components.AlphaBarPicker
import com.movtery.colorpicker.components.ColorSquarePicker
import com.movtery.colorpicker.components.HueBarPicker
import com.movtery.colorpicker.components.TransparentChecker

@Composable
fun ColorPickerDialog(
    colorController: ColorPickerController,
    onChangeFinished: () -> Unit = {},
    onCancel: () -> Unit,
    onConfirm: (Color) -> Unit,
    showAlpha: Boolean = true,
    showHue: Boolean = true
) {
    val selected by colorController.color
    val original = remember(colorController) { colorController.getOriginalColor() }
    var editHex by rememberSaveable { mutableStateOf(false) }
    val cancel = {
        colorController.setColor(original)
        onCancel()
        onChangeFinished()
    }
    val finish = {
        val result = if (showAlpha) selected else selected.copy(alpha = 1f)
        colorController.setColor(result)
        onConfirm(result)
        onChangeFinished()
    }

    DialogFrame(
        title = stringResource(R.string.theme_color_picker_title),
        onDismiss = cancel,
        actions = {
            TextButton(onClick = cancel) { Text(stringResource(R.string.generic_cancel)) }
            TextButton(onClick = finish) { Text(stringResource(R.string.generic_confirm)) }
        }
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Space.lg, end = Space.lg, bottom = Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            ColorSquarePicker(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.4f).clip(FieldShape),
                controller = colorController,
                onChangeFinished = onChangeFinished
            )
            if (showHue) {
                HueBarPicker(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    controller = colorController,
                    onChangeFinished = onChangeFinished
                )
            }
            if (showAlpha) {
                AlphaBarPicker(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    controller = colorController,
                    onChangeFinished = onChangeFinished
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                ColorPreview(original, stringResource(R.string.pe_color_original), Modifier.weight(1f))
                ColorPreview(selected, stringResource(R.string.pe_color_current), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { colorController.setColor(original); onChangeFinished() }) {
                    Text(stringResource(R.string.generic_reset))
                }
                TextButton(onClick = { editHex = true }, modifier = Modifier.weight(1f)) {
                    Icon(painterResource(R.drawable.ic_edit_outlined), null, Modifier.size(18.dp))
                    Text(stringResource(R.string.theme_color_picker_edit_hex),
                        Modifier.padding(start = Space.sm), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (editHex) {
        var input by rememberSaveable { mutableStateOf(selected.toHex()) }
        val parsed = input.toColorOrNull()
        SimpleEditDialog(
            title = stringResource(R.string.theme_color_picker_edit_hex),
            value = input,
            onValueChange = { input = it },
            isError = parsed == null,
            singleLine = true,
            supportingText = {
                if (parsed == null) Text(stringResource(R.string.theme_color_picker_edit_hex_invalid))
            },
            onDismissRequest = { editHex = false },
            onConfirm = {
                if (parsed != null) {
                    colorController.setColor(if (showAlpha) parsed else parsed.copy(alpha = 1f))
                    onChangeFinished()
                    editHex = false
                }
            }
        )
    }
}

@Composable
private fun ColorPreview(color: Color, title: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(40.dp).clip(FieldShape)) {
            TransparentChecker(Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(color))
        }
        Text("#${color.toHex()}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

fun Color.toHex(): String = listOf(alpha, red, green, blue).joinToString("") {
    (it.coerceIn(0f, 1f) * 255).toInt().toString(16).padStart(2, '0')
}.uppercase()

fun String.toColorOrNull(): Color? {
    val hex = trim().removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    if (hex.any { it.digitToIntOrNull(16) == null }) return null
    val value = hex.toLongOrNull(16) ?: return null
    val alpha = if (hex.length == 8) ((value shr 24) and 0xFF) / 255f else 1f
    return Color(((value shr 16) and 0xFF) / 255f, ((value shr 8) and 0xFF) / 255f, (value and 0xFF) / 255f, alpha)
}
