package com.cmc.launcher.ui.screens.content.home.server

import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmc.launcher.R
import com.cmc.launcher.context.COPY_LABEL_SERVER_IP
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.MenuSwitchButton
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.components.SimpleListDialog
import com.cmc.launcher.ui.kit.CmcListAction
import com.cmc.launcher.ui.kit.CmcRadioRow
import com.cmc.launcher.ui.screens.content.home.version.VersionCardDir
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.utils.copyText
import com.cmc.launcher.utils.string.stripColorCodes

@Composable
internal fun ServerCardSettingsDialog(record: ServerCardRecord, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val cardId = record.cardId
    var name by rememberSaveable(cardId) { mutableStateOf(record.name) }
    var quickLaunch by rememberSaveable(cardId) { mutableStateOf(record.quickLaunchEnabled) }
    var mode by rememberSaveable(cardId) { mutableStateOf(record.versionMode) }
    var bindingName by rememberSaveable(cardId) { mutableStateOf(record.versionName) }
    var bindingPath by rememberSaveable(cardId) { mutableStateOf(record.dir?.resolveGameHome()) }
    var bindingIsDefault by rememberSaveable(cardId) { mutableStateOf(record.dir == VersionCardDir.Default) }
    var choosingVersion by rememberSaveable(cardId) { mutableStateOf(false) }
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val inFlight by ServerCardManager.inFlightCards.collectAsStateWithLifecycle()
    val validName = name.stripColorCodes().trim().isNotEmpty()
    val validBinding = mode != ServerCardVersionMode.SPECIFIC ||
        (!bindingName.isNullOrBlank() && !bindingPath.isNullOrBlank())
    val canSave = validName && (!quickLaunch || validBinding)

    DialogFrame(
        title = stringResource(R.string.home_server_card_settings),
        onDismiss = onDismissRequest,
        actions = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.generic_cancel)) }
            TextButton(enabled = canSave, onClick = {
                val dir = if (mode == ServerCardVersionMode.SPECIFIC) {
                    if (bindingIsDefault) VersionCardDir.Default else bindingPath?.let { VersionCardDir.Custom(it) }
                } else null
                ServerCardManager.saveBookmarkSettings(cardId, name, quickLaunch, mode, dir, bindingName)
                onDismissRequest()
            }) { Text(stringResource(R.string.generic_confirm)) }
        }
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Space.lg, end = Space.lg, bottom = Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            OwnOutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                isError = !validName, label = { Text(stringResource(R.string.servers_list_add_server_name)) },
                supportingText = { if (!validName) Text(stringResource(R.string.generic_cannot_empty)) }
            )
            CmcListAction(
                title = record.cardId, iconRes = R.drawable.ic_copy_all_outlined,
                summary = stringResource(R.string.servers_list_copy_server_address),
                onClick = { copyText(COPY_LABEL_SERVER_IP, record.cardId, context) }
            )
            CmcListAction(
                title = stringResource(if (cardId in inFlight) R.string.generic_loading else R.string.generic_refresh),
                iconRes = R.drawable.ic_refresh, enabled = cardId !in inFlight,
                onClick = {
                    ServerCardManager.refresh(cardId, manual = true) { success ->
                        if (!success) Toast.makeText(context, context.getString(R.string.home_server_card_refresh_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            )
            MenuSwitchButton(
                text = stringResource(R.string.home_server_card_quick_launch),
                switch = quickLaunch, onSwitch = { quickLaunch = it }
            )
            if (quickLaunch) {
                CmcRadioRow(
                    text = stringResource(R.string.home_server_card_version_current),
                    selected = mode == ServerCardVersionMode.CURRENT,
                    onClick = { mode = ServerCardVersionMode.CURRENT }
                )
                CmcRadioRow(
                    text = stringResource(R.string.home_server_card_version_specific),
                    selected = mode == ServerCardVersionMode.SPECIFIC,
                    onClick = { mode = ServerCardVersionMode.SPECIFIC }
                )
                if (mode == ServerCardVersionMode.SPECIFIC) {
                    CmcListAction(
                        title = bindingName ?: stringResource(R.string.generic_unspecified),
                        summary = stringResource(R.string.download_addon_filter_game_version),
                        iconRes = R.drawable.ic_sports_esports_outlined, endIconRes = R.drawable.ic_arrow_right_rounded,
                        onClick = { choosingVersion = true }
                    )
                    if (!validBinding) Text(stringResource(R.string.pe_server_choose_version),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (choosingVersion) {
        val usable = versions.filter { it.isValid() }
        if (usable.isEmpty()) {
            SimpleAlertDialog(
                title = stringResource(R.string.download_addon_filter_game_version),
                text = stringResource(R.string.versions_manage_no_versions),
                onDismiss = { choosingVersion = false }
            )
        } else {
            SimpleListDialog(
                title = stringResource(R.string.download_addon_filter_game_version),
                items = usable,
                current = usable.firstOrNull { it.getVersionName() == bindingName && it.getGameHome() == bindingPath },
                onItemSelected = { version ->
                    val dir = VersionCardDir.fromGameHome(version.getGameHome())
                    bindingName = version.getVersionName()
                    bindingPath = dir.resolveGameHome()
                    bindingIsDefault = dir == VersionCardDir.Default
                },
                onDismissRequest = { choosingVersion = false },
                itemLayout = { version: Version, selected, onClick ->
                    CmcRadioRow(text = version.getVersionName(), selected = selected, onClick = onClick)
                }
            )
        }
    }
}
