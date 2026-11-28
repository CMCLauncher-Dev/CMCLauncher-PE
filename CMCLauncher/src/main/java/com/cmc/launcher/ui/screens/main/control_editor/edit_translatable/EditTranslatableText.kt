package com.cmc.launcher.ui.screens.main.control_editor.edit_translatable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.components.SingleLineTextCheck
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Space
import com.movtery.layer_controller.observable.ObservableTranslatableString
import com.movtery.layer_controller.utils.toSimpleLangTag

@Composable
fun EditTranslatableTextDialog(
    text: ObservableTranslatableString, onClose: () -> Unit, singleLine: Boolean = true,
    allowEmpty: Boolean = true, onDismissRequest: (() -> Unit)? = null,
    title: String = stringResource(R.string.control_editor_edit_text),
    closeText: String = stringResource(R.string.generic_close), take: Int? = null
) {
    val locale = LocalConfiguration.current.locales[0].toSimpleLangTag()
    key(text) {
        DialogFrame(title, onDismissRequest ?: onClose, actions = {
            TextButton(onClick = { text.addLocalizedString() }) { Text(stringResource(R.string.control_editor_edit_translatable_other_add)) }
            TextButton(onClick = onClose) { Text(closeText) }
        }) {
            Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = Space.lg, end = Space.lg, bottom = Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.md)) {
                TranslationField(stringResource(R.string.control_editor_edit_translatable_default), text.default,
                    { text.default = it }, singleLine, allowEmpty, take)
                Text(stringResource(R.string.control_editor_edit_translatable_other_tip, locale),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                text.matchQueue.forEach { entry ->
                    key(entry) {
                        Surface(Modifier.fillMaxWidth(), shape = FieldShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                            Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    val tag = entry.languageTag.ifBlank { stringResource(R.string.generic_unspecified) }
                                    Text(tag, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val remove = stringResource(R.string.generic_delete)
                                    IconButton(onClick = { text.deleteLocalizedString(entry) }) {
                                        Icon(painterResource(R.drawable.ic_delete_outlined), "$remove · $tag")
                                    }
                                }
                                TranslationField(stringResource(R.string.control_editor_edit_translatable_other_tag),
                                    entry.languageTag, { entry.languageTag = it }, true, true, 8)
                                TranslationField(stringResource(R.string.control_editor_edit_translatable_other_value),
                                    entry.value, { entry.value = it }, singleLine, allowEmpty, take)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationField(label: String, value: String, change: (String) -> Unit, single: Boolean,
    allowEmpty: Boolean, limit: Int?) {
    val invalid = !allowEmpty && value.isBlank()
    val maximum = limit?.coerceAtLeast(0)
    if (single) SingleLineTextCheck(value, change)
    OwnOutlinedTextField(modifier = Modifier.fillMaxWidth(), value = value, onValueChange = { input ->
        val normalized = if (single) input.replace("\r\n", " " ).replace('\r', ' ').replace('\n', ' ') else input
        change(if (maximum == null) normalized else normalized.take(maximum))
    }, label = { Text(label) }, singleLine = single, isError = invalid,
        supportingText = if (!invalid && maximum == null) null else ({
            if (invalid) Text(stringResource(R.string.control_manage_create_new_field_blank))
            else if (maximum != null) Text(stringResource(R.string.generic_input_length, value.length, maximum))
        }))
}
