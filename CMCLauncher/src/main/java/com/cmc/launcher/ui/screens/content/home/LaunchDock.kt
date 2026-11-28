package com.cmc.launcher.ui.screens.content.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.game.account.Account
import com.cmc.launcher.game.account.getAccountTypeName
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.SimpleListItem
import com.cmc.launcher.ui.screens.content.elements.PlayerFace
import com.cmc.launcher.ui.screens.content.elements.VersionIconImage
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.PanelShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.launcherInsetColor
import com.cmc.launcher.ui.theme.launcherPanelColor

@Composable
internal fun LaunchDock(
    account: Account?, version: Version?, versions: List<Version>, refreshing: Boolean,
    accounts: () -> Unit, manageVersions: () -> Unit, versionSettings: (Version) -> Unit,
    install: () -> Unit, launch: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val selection: LaunchSelectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel { LaunchSelectionViewModel() }
    selection.error?.let { message ->
        com.cmc.launcher.ui.components.SimpleAlertDialog(
            title = stringResource(R.string.versions_config_failed_to_save),
            text = message,
            onDismiss = selection::clearError
        )
    }
    var choosingVersion by rememberSaveable { mutableStateOf(false) }
    val currentSummary = version?.getVersionSummaryOrNull()
    val valid = currentSummary != null
    val hasVersions = versions.isNotEmpty()
    val choose: () -> Unit = {
        if (!refreshing) {
            if (hasVersions) choosingVersion = true else install()
        }
    }
    Surface(Modifier.fillMaxWidth(), shape = PanelShape, color = launcherPanelColor(),
        border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.09f))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Row(Modifier.fillMaxWidth().clip(FieldShape).background(launcherInsetColor())
                .clickable(role = Role.Button, interactionSource = null, indication = com.cmc.launcher.ui.kit.ImmediateIndication, onClick = accounts)
                .heightIn(min = 54.dp).padding(horizontal = Space.md, vertical = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
                if (account != null) PlayerFace(account = account, avatarSize = 30.dp)
                else Icon(painterResource(R.drawable.ic_person_outlined), null, Modifier.size(30.dp), tint = colors.onSurface)
                Column(Modifier.weight(1f)) {
                    Text(account?.username ?: stringResource(R.string.page_title_account_list),
                        style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (account != null) Text(getAccountTypeName(account), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(painterResource(R.drawable.ic_arrow_right_rounded), null, Modifier.size(Size.iconSm))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clip(FieldShape)
                    .clickable(enabled = !refreshing, role = Role.Button, interactionSource = null, indication = com.cmc.launcher.ui.kit.ImmediateIndication, onClick = choose)
                    .heightIn(min = 54.dp).padding(horizontal = Space.sm, vertical = Space.xs),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                    VersionIconImage(version, Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(when {
                            refreshing -> stringResource(R.string.pe_home_scanning)
                            version != null -> version.getVersionName()
                            hasVersions -> stringResource(R.string.pe_home_choose_version)
                            else -> stringResource(R.string.versions_manage_no_versions)
                        }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (version != null && !refreshing) {
                            val summary = currentSummary ?: stringResource(R.string.pe_home_version_invalid)
                            if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.labelSmall,
                                color = if (valid) colors.onSurfaceVariant else colors.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (hasVersions) Icon(painterResource(R.drawable.ic_arrow_drop_down_rounded),
                        stringResource(R.string.pe_home_choose_version), Modifier.size(Size.iconSm))
                }
                if (version != null) IconButton(enabled = !refreshing, onClick = {
                    if (valid) versionSettings(version) else manageVersions()
                }) {
                    Icon(painterResource(R.drawable.ic_settings_filled), stringResource(R.string.versions_manage_settings),
                        Modifier.size(Size.iconSm), tint = colors.onSurfaceVariant)
                }
            }
            val actionLabel = when {
                refreshing -> stringResource(R.string.pe_home_scanning)
                !hasVersions && version == null -> stringResource(R.string.pe_home_install)
                version == null -> stringResource(R.string.pe_home_choose_version)
                account == null -> stringResource(R.string.pe_home_sign_in_launch)
                else -> stringResource(R.string.main_launch_game)
            }
            val enabled = !refreshing && !selection.selecting && (version == null || valid)
            Row(Modifier.fillMaxWidth().clip(FieldShape)
                .background(if (enabled) colors.primary else colors.onSurface.copy(alpha = 0.12f))
                .clickable(enabled = enabled, role = Role.Button, interactionSource = null, indication = com.cmc.launcher.ui.kit.ImmediateIndication, onClick = {
                    when {
                        version == null -> choose()
                        account == null -> accounts()
                        valid -> launch()
                    }
                }).heightIn(min = Size.touch).padding(horizontal = Space.md, vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm, Alignment.CenterHorizontally)) {
                Icon(painterResource(if (version == null && !hasVersions) R.drawable.ic_download_2_outlined else R.drawable.ic_play_arrow_filled),
                    null, Modifier.size(Size.iconMd), tint = if (enabled) colors.onPrimary else colors.onSurfaceVariant)
                Text(actionLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    color = if (enabled) colors.onPrimary else colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    if (choosingVersion) {
        DialogFrame(stringResource(R.string.pe_home_choose_version), { choosingVersion = false }, actions = {
            TextButton(onClick = { choosingVersion = false; manageVersions() }) { Text(stringResource(R.string.page_title_version_manage)) }
            TextButton(onClick = { choosingVersion = false }) { Text(stringResource(R.string.generic_close)) }
        }) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), contentPadding = PaddingValues(Space.md)) {
                items(versions, key = { it.getVersionPath().absolutePath }) { item ->
                    val summary = item.getVersionSummaryOrNull()
                    SimpleListItem(
                        selected = item == version,
                        itemName = item.getVersionName(),
                        enabled = !refreshing && !selection.selecting && summary != null,
                        summary = {
                            Text(
                                summary ?: stringResource(R.string.pe_home_version_invalid),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        onClick = {
                            if (item.isValid()) {
                                selection.select(item)
                                choosingVersion = false
                            }
                        }
                    )
                }
            }
        }
    }
}