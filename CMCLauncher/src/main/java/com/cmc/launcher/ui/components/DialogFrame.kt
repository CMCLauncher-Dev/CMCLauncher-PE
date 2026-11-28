package com.cmc.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cmc.launcher.ui.components.StaticDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import com.cmc.launcher.ui.theme.CardShape
import com.cmc.launcher.ui.theme.Space

@Composable
internal fun DialogFrame(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = rememberDialogMaxHeight()),
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column {
                Text(
                    text = title,
                    modifier = Modifier.padding(Space.xl).semantics { heading() },
                    style = MaterialTheme.typography.titleLarge
                )
                content()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm, androidx.compose.ui.Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = actions
                )
            }
        }
    }
}
