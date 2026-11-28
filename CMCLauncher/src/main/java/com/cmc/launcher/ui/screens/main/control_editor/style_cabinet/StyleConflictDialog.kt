package com.cmc.launcher.ui.screens.main.control_editor.style_cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cmc.launcher.R
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.kit.CmcListAction
import com.cmc.launcher.ui.theme.Space

@Composable
fun StyleConflictDialog(
    title: String, message: String, primaryText: String, secondaryText: String,
    onPrimary: () -> Unit, onSecondary: () -> Unit, onCancel: () -> Unit
) {
    DialogFrame(title, onCancel, actions = {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.generic_cancel)) }
    }) {
        Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            CmcListAction(title = secondaryText, iconRes = R.drawable.ic_copy_all_outlined, onClick = onSecondary)
            CmcListAction(title = primaryText, iconRes = R.drawable.ic_edit_outlined, onClick = onPrimary, destructive = true)
        }
    }
}
