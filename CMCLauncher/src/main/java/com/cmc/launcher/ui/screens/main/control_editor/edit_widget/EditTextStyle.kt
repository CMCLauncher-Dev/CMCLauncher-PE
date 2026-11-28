package com.cmc.launcher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.main.control_editor.EditorChoices
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutSwitchItem
import com.cmc.launcher.ui.screens.main.control_editor.InfoLayoutTextItem
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.data.TextAlignment
import com.movtery.layer_controller.observable.ObservableNormalData
import com.movtery.layer_controller.observable.ObservableTextData
import com.movtery.layer_controller.observable.ObservableTranslatableString
import com.movtery.layer_controller.observable.ObservableWidget

@Composable
fun EditTextStyle(screenKey: TitledNavKey, currentKey: TitledNavKey?, data: ObservableWidget,
    onEditWidgetText: (ObservableTranslatableString) -> Unit) {
    if (screenKey != currentKey) return
    when (data) {
        is ObservableNormalData -> TypographyPanel(data.text, data.textAlignment, data.textBold, data.textItalic, data.textUnderline,
            { data.textAlignment = it }, { data.textBold = it }, { data.textItalic = it }, { data.textUnderline = it }, onEditWidgetText)
        is ObservableTextData -> TypographyPanel(data.text, data.textAlignment, data.textBold, data.textItalic, data.textUnderline,
            { data.textAlignment = it }, { data.textBold = it }, { data.textItalic = it }, { data.textUnderline = it }, onEditWidgetText)
    }
}

@Composable
private fun TypographyPanel(text: ObservableTranslatableString, alignment: TextAlignment, bold: Boolean,
    italic: Boolean, underline: Boolean, align: (TextAlignment) -> Unit, setBold: (Boolean) -> Unit,
    setItalic: (Boolean) -> Unit, setUnderline: (Boolean) -> Unit, edit: (ObservableTranslatableString) -> Unit) {
    val sample = text.translate(LocalConfiguration.current.locales[0])
    val modes = listOf(TextAlignment.Left, TextAlignment.Center, TextAlignment.Right)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Surface(Modifier.fillMaxWidth(), shape = FieldShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Text(if (sample.isEmpty()) stringResource(R.string.generic_unspecified) else sample,
                Modifier.fillMaxWidth().padding(Space.lg), style = MaterialTheme.typography.titleMedium,
                textAlign = alignment.textAlign, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (underline) TextDecoration.Underline else TextDecoration.None,
                maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
        InfoLayoutTextItem(title = stringResource(R.string.control_editor_edit_text), onClick = { edit(text) })
        Text(stringResource(R.string.control_editor_edit_text_alignment), style = MaterialTheme.typography.titleSmall)
        EditorChoices(listOf(stringResource(R.string.pe_align_left), stringResource(R.string.pe_align_center),
            stringResource(R.string.pe_align_right)), modes.indexOf(alignment), { align(modes[it]) })
        InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_edit_text_bold), value = bold, onValueChange = setBold)
        InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_edit_text_italic), value = italic, onValueChange = setItalic)
        InfoLayoutSwitchItem(title = stringResource(R.string.control_editor_edit_text_underline), value = underline, onValueChange = setUnderline)
    }
}
