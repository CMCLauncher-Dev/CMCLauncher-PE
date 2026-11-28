package com.cmc.launcher.ui.screens.content.versions.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

internal enum class ExportStep(val label: Int) {
    Format(R.string.pe_export_format), Info(R.string.pe_export_info), Files(R.string.pe_export_files)
}

@Composable
internal fun ExportStepLayout(step: ExportStep, action: String? = null, enabled: Boolean = true,
    proceed: () -> Unit = {}, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(step.label), Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${step.ordinal + 1} / ${ExportStep.entries.size}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        if (action != null) {
            HorizontalDivider()
            Button(onClick = proceed, enabled = enabled, modifier = Modifier.fillMaxWidth()
                .padding(horizontal = Space.lg, vertical = Space.xs).heightIn(min = Size.touch)) { Text(action) }
        }
    }
}
