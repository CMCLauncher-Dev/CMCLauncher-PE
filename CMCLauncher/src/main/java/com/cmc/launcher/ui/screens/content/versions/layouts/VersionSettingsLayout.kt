package com.cmc.launcher.ui.screens.content.versions.layouts

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import com.cmc.launcher.ui.components.StaticDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.game.version.installed.SettingState
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.ui.components.TitleAndSummary
import com.cmc.launcher.ui.screens.content.elements.VersionIconImage
import com.cmc.launcher.ui.screens.content.settings.layouts.CardPosition
import com.cmc.launcher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.cmc.launcher.ui.screens.content.settings.layouts.SettingsCard
import kotlin.math.min

@Composable
fun StatefulDropdownMenuFollowGlobal(
    title: String,
    position: CardPosition,
    modifier: Modifier = Modifier,
    currentValue: SettingState,
    onValueChange: (SettingState) -> Unit,
    enabled: Boolean = true,
    iconSize: Dp = 20.dp,
    summary: String? = null
) {
    val value = currentValue
    var expanded by remember { mutableStateOf(false) }

    SettingsCard(
        modifier = modifier,
        position = position,
        onClick = {
            expanded = !expanded
        },
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TitleAndSummary(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                title = title,
                summary = summary
            )

            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = stringResource(value.textRes),
                style = MaterialTheme.typography.labelMedium
            )

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.size(34.dp),
                    enabled = enabled,
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        painter = painterResource(R.drawable.ic_settings_filled),
                        contentDescription = stringResource(R.string.generic_setting)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 3.dp,
                ) {
                    val allEntries = SettingState.entries
                    repeat(allEntries.size) { index ->
                        val state = allEntries[index]
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(state.textRes))
                            },
                            enabled = enabled,
                            onClick = {
                                onValueChange(state)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleableIntSliderSettingsCard(
    currentValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Int,
    position: CardPosition,
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    suffix: String? = null,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit = {},
    onValueChangeFinished: () -> Unit = {},
    previewContent: @Composable (ColumnScope.() -> Unit) = {}
) {
    val checked = currentValue >= valueRange.start
    val value = (if (checked) currentValue else defaultValue)
        .coerceIn(valueRange.start.toInt(), valueRange.endInclusive.toInt())
    IntSliderSettingsCard(
        modifier = modifier,
        position = position,
        value = value,
        title = title,
        summary = summary,
        valueRange = valueRange,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        suffix = suffix,
        enabled = enabled && checked,
        fineTuningControl = true,
        appendContent = {
            Checkbox(
                modifier = Modifier.padding(start = 12.dp),
                checked = checked,
                enabled = enabled,
                onCheckedChange = { useCustom ->
                    val next = defaultValue.coerceIn(valueRange.start.toInt(), valueRange.endInclusive.toInt())
                    onValueChange(if (useCustom) next else -1)
                    onValueChangeFinished()
                }
            )
        },
        previewContent = previewContent
    )
}

@Composable
fun VersionOverviewItem(
    modifier: Modifier = Modifier,
    version: Version,
    versionName: String = version.getVersionName(),
    versionSummary: String,
    refreshKey: Any? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VersionIconImage(
            version = version,
            modifier = Modifier.size(34.dp),
            refreshKey = refreshKey
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = versionName,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            if (versionSummary.isNotBlank()) {
                Text(
                    text = versionSummary,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
