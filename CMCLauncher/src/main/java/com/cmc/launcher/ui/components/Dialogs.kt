package com.cmc.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import com.cmc.launcher.ui.components.StaticProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Space
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@Composable
fun rememberDialogMaxHeight(): Dp = with(LocalDensity.current) {
    (LocalWindowInfo.current.containerSize.height.toDp() - 48.dp).coerceAtLeast(48.dp)
}

@Composable
fun SimpleAlertDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissText: String = stringResource(R.string.generic_cancel),
    dismissByDialog: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertContent(title, { if (dismissByDialog) onDismiss() }, confirmText, onConfirm, dismissText, onDismiss) {
        Text(text)
    }
}

@Composable
fun SimpleAlertDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissByDialog: Boolean = true,
    onDismiss: () -> Unit
) {
    AlertContent(title, { if (dismissByDialog) onDismiss() }, confirmText, onDismiss) { Text(text) }
}

@Composable
fun SimpleAlertDialog(
    title: String,
    text: @Composable () -> Unit = {},
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissText: String = stringResource(R.string.generic_cancel),
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    AlertContent(title, onDismissRequest, confirmText, onConfirm, dismissText, onCancel, text)
}

@Composable
fun SimpleAlertDialog(
    title: String,
    text: @Composable () -> Unit = {},
    confirmText: String = stringResource(R.string.generic_confirm),
    onDismiss: () -> Unit
) {
    AlertContent(title, onDismiss, confirmText, onDismiss, text = text)
}

@Composable
private fun AlertContent(
    title: String,
    dismiss: () -> Unit,
    confirmLabel: String,
    confirm: () -> Unit,
    cancelLabel: String? = null,
    cancel: (() -> Unit)? = null,
    text: @Composable () -> Unit
) {
    DialogFrame(title, dismiss, actions = {
        if (cancel != null && cancelLabel != null) TextButton(onClick = cancel) { Text(cancelLabel) }
        TextButton(onClick = confirm) { Text(confirmLabel) }
    }) {
        Column(
            Modifier.weight(1f, fill = false).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Space.xl, end = Space.xl, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) { text() }
    }
}

@Composable
fun SimpleEditDialog(
    title: String,
    value: String,
    onValueChange: (newValue: String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    extraBody: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    onDismissRequest: () -> Unit = {},
    onCancel: () -> Unit = onDismissRequest,
    onConfirm: () -> Unit = {}
) {
    EditContent(title, value, onValueChange, label, isError, supportingText, singleLine, maxLines,
        keyboardOptions, extraBody, extraContent, onDismissRequest, onCancel, onConfirm)
}

@Composable
fun SimpleEditDialog(
    title: String,
    value: String,
    onValueChange: (newValue: String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    extraBody: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    onConfirm: () -> Unit = {}
) {
    EditContent(title, value, onValueChange, label, isError, supportingText, singleLine, maxLines,
        keyboardOptions, extraBody, extraContent, {}, null, onConfirm)
}

@Composable
private fun EditContent(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)?,
    isError: Boolean,
    supportingText: @Composable (() -> Unit)?,
    singleLine: Boolean,
    maxLines: Int,
    keyboardOptions: KeyboardOptions,
    extraBody: @Composable (() -> Unit)?,
    extraContent: @Composable (() -> Unit)?,
    dismiss: () -> Unit,
    cancel: (() -> Unit)?,
    confirm: () -> Unit
) {
    val focus = LocalFocusManager.current
    DialogFrame(title, dismiss, actions = {
        if (cancel != null) TextButton(onClick = cancel) { Text(stringResource(R.string.generic_cancel)) }
        TextButton(onClick = confirm, enabled = !isError) { Text(stringResource(R.string.generic_confirm)) }
    }) {
        Column(
            Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = Space.xl, end = Space.xl, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            extraBody?.invoke()
            if (singleLine) SingleLineTextCheck(text = value, onSingleLined = onValueChange)
            OwnOutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = label,
                isError = isError,
                supportingText = supportingText,
                singleLine = singleLine,
                maxLines = maxLines,
                shape = FieldShape,
                keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!isError) {
                        focus.clearFocus()
                        confirm()
                    }
                })
            )
            extraContent?.invoke()
        }
    }
}

@Composable
fun SimpleCheckEditDialog(
    title: String,
    text: String,
    value: String,
    checked: Boolean,
    checkBoxText: String? = null,
    onValueChange: (newValue: String) -> Unit,
    onCheckedChange: (newValue: Boolean) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    EditContent(title, value, onValueChange, label, isError, supportingText, singleLine, maxLines,
        keyboardOptions, extraBody = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        extraContent = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (checkBoxText != null) Text(checkBoxText, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Checkbox(checked, onCheckedChange)
            }
        }, dismiss = onDismissRequest, cancel = onDismissRequest, confirm = onConfirm)
}

@Composable
fun <T> SimpleListDialog(
    title: String,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    onDismissRequest: (selected: Boolean) -> Unit,
    current: T? = null,
    itemLayout: @Composable (item: T, isCurrent: Boolean, onClick: () -> Unit) -> Unit,
    showConfirm: Boolean = false,
    confirmText: @Composable RowScope.() -> Unit = { Text(stringResource(R.string.generic_confirm)) }
) {
    var choice by remember(current) { mutableStateOf(current) }
    DialogFrame(title, { onDismissRequest(false) }, actions = {
        TextButton(onClick = { onDismissRequest(false) }) { Text(stringResource(R.string.generic_cancel)) }
        if (showConfirm) {
            TextButton(enabled = choice?.let { items.contains(it) } == true, onClick = {
                choice?.let {
                    onItemSelected(it)
                    onDismissRequest(true)
                }
            }, content = confirmText)
        }
    }) {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            items(items) { item ->
                itemLayout(item, choice == item) {
                    choice = item
                    if (!showConfirm) {
                        if (current != item) onItemSelected(item)
                        onDismissRequest(true)
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleTaskDialog(
    title: String,
    text: String? = null,
    task: suspend () -> Unit,
    context: CoroutineContext = Dispatchers.Default,
    onDismiss: () -> Unit,
    onError: (Throwable) -> Unit = {}
) {
    val latestTask by rememberUpdatedState(task)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val latestError by rememberUpdatedState(onError)
    ProgressDialog(title, text)
    LaunchedEffect(Unit) {
        try {
            withContext(context) { latestTask() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            latestError(error)
        }
        latestDismiss()
    }
}

@Composable
fun ProgressDialog(title: String = stringResource(R.string.generic_in_progress), text: String? = null) {
    DialogFrame(title, {}, actions = {}) {
        Column(
            Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = Space.xl, end = Space.xl, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.lg)
        ) {
            if (text != null) Text(text, style = MaterialTheme.typography.bodyMedium)
            StaticProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
