package com.cmc.launcher.ui.screens.content.versions.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.cmc.launcher.BuildKeys
import com.cmc.launcher.R
import com.cmc.launcher.game.version.export.PackType
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.theme.CardShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

@Composable
fun ExportTypeSelectScreen(
    mainScreenKey: TitledNavKey?,
    exportScreenKey: TitledNavKey?,
    onTypeSelect: (PackType) -> Unit
) {
    if (mainScreenKey !is NestedNavKey.VersionExport || exportScreenKey != NormalNavKey.VersionExports.SelectType) return
    var showNotes by rememberSaveable { mutableStateOf(false) }
    ExportStepLayout(ExportStep.Format) {
        LazyColumn(
            Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            items(PackType.entries, key = { it.name }) { type ->
                val label = stringResource(type.titleResource())
                Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = { onTypeSelect(type) })
                            .heightIn(min = Size.row).padding(Space.lg),
                        horizontalArrangement = Arrangement.spacedBy(Space.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painterResource(when (type) {
                            PackType.MCBBS -> R.drawable.img_chest
                            PackType.Modrinth -> R.drawable.img_platform_modrinth
                            PackType.CurseForge -> R.drawable.img_platform_curseforge
                        }), null, Modifier.size(Size.iconXl))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (type == PackType.MCBBS) stringResource(R.string.versions_export_type_mcbbs_summary, BuildKeys.LAUNCHER_SHORT_NAME)
                                else stringResource(R.string.versions_export_type_summary_common),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(painterResource(R.drawable.ic_arrow_right_rounded), null, Modifier.size(Size.iconSm))
                    }
                }
            }
            item {
                TextButton(onClick = { showNotes = !showNotes }) {
                    Text(stringResource(R.string.generic_tip))
                    Icon(painterResource(if (showNotes) R.drawable.ic_arrow_drop_up_rounded else R.drawable.ic_arrow_drop_down_rounded),
                        stringResource(if (showNotes) R.string.generic_collapse else R.string.generic_expand))
                }
                if (showNotes) {
                    Column(Modifier.padding(horizontal = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        for (note in listOf(R.string.versions_export_tip_1, R.string.versions_export_tip_2,
                            R.string.versions_export_tip_3, R.string.versions_export_tip_4, R.string.versions_export_tip_5)) {
                            Text(stringResource(note), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

internal fun PackType.titleResource(): Int = when (this) {
    PackType.MCBBS -> R.string.versions_export_type_mcbbs
    PackType.Modrinth -> R.string.versions_export_type_modrinth
    PackType.CurseForge -> R.string.versions_export_type_curseforge
}
