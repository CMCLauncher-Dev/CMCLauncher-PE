package com.cmc.launcher.ui.screens.content

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.notification.NotificationManager
import com.cmc.launcher.path.PathManager
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.terracotta.Terracotta
import com.cmc.launcher.ui.androidText
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.components.NotificationCheck
import com.cmc.launcher.ui.kit.CmcCard
import com.cmc.launcher.ui.kit.CmcChip
import com.cmc.launcher.ui.kit.CmcDivider
import com.cmc.launcher.ui.kit.CmcListAction
import com.cmc.launcher.ui.kit.CmcPanel
import com.cmc.launcher.ui.kit.CmcSection
import com.cmc.launcher.ui.kit.CmcSwitchRow
import com.cmc.launcher.ui.kit.CmcTextField
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.theme.Alpha
import com.cmc.launcher.ui.theme.CardShape
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.utils.file.shareFile
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import com.cmc.launcher.viewmodel.sendToast

@Composable
fun MultiplayerScreen(
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel
) {
    val context = LocalContext.current

    BaseScreen(
        screenKey = NormalNavKey.Multiplayer,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) {
        val shareLogs = {
            val logFile = PathManager.FILE_TERRACOTTA_LOG
            if (logFile.exists()) {
                shareFile(context, logFile)
            } else {
                eventViewModel.sendToast(androidText(R.string.terracotta_export_log_share_null))
            }
            Unit
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            MultiplayerContent(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                onShareLogs = shareLogs
            )
        }
    }
}

private sealed interface MultiplayerOperation {
    data object None : MultiplayerOperation
    data object Notice : MultiplayerOperation
    data object WarningNotification : MultiplayerOperation
}

@Composable
private fun MultiplayerOperation(
    operation: MultiplayerOperation,
    onChange: (MultiplayerOperation) -> Unit,
    onNoticeRead: () -> Unit,
    onNoticeRefused: () -> Unit
) {
    when (operation) {
        is MultiplayerOperation.None -> {}
        is MultiplayerOperation.Notice -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.terracotta_status_uninitialized_desc),
                dismissByDialog = false,
                onDismiss = onNoticeRefused,
                onConfirm = onNoticeRead
            )
        }
        is MultiplayerOperation.WarningNotification -> {
            NotificationCheck(
                text = stringResource(R.string.notification_data_terracotta_message),
                onGranted = { onChange(MultiplayerOperation.None) },
                onIgnore = { onChange(MultiplayerOperation.None) },
                onDismiss = { onChange(MultiplayerOperation.None) }
            )
        }
    }
}

@Composable
private fun MultiplayerContent(
    modifier: Modifier = Modifier,
    onShareLogs: () -> Unit
) {
    val context = LocalContext.current
    var operation by remember { mutableStateOf<MultiplayerOperation>(MultiplayerOperation.None) }
    val enabled = AllSettings.enableTerracotta.state
    val nodesEnabled = AllSettings.enableTerracottaNodes.state

    MultiplayerOperation(
        operation = operation,
        onChange = { operation = it },
        onNoticeRead = {
            AllSettings.terracottaNoticeVer.save(Terracotta.TERRACOTTA_USER_NOTICE_VERSION)
            operation = if (!NotificationManager.checkNotificationEnabled(context)) {
                MultiplayerOperation.WarningNotification
            } else {
                MultiplayerOperation.None
            }
        },
        onNoticeRefused = {
            AllSettings.enableTerracotta.save(false)
            operation = MultiplayerOperation.None
        }
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screenH)
            .padding(top = Space.sm, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        StatusHeader {
            CmcSwitchRow(
                title = stringResource(R.string.terracotta_enable),
                summary = stringResource(R.string.pe_multiplayer_enable_summary),
                checked = enabled,
                onCheckedChange = { value ->
                    AllSettings.enableTerracotta.save(value)
                    if (value) {
                        when {
                            AllSettings.terracottaNoticeVer.getValue() < Terracotta.TERRACOTTA_USER_NOTICE_VERSION -> {
                                operation = MultiplayerOperation.Notice
                            }
                            !NotificationManager.checkNotificationEnabled(context) -> {
                                operation = MultiplayerOperation.WarningNotification
                            }
                        }
                    }
                }
            )
        }

        CmcSection(title = stringResource(R.string.pe_multiplayer_settings))
        CmcPanel {
            CmcSwitchRow(
                title = stringResource(R.string.terracotta_custom_note_list),
                checked = nodesEnabled,
                enabled = enabled,
                onCheckedChange = { AllSettings.enableTerracottaNodes.save(it) }
            )
            if (enabled && nodesEnabled) {
                CmcTextField(
                    modifier = Modifier.padding(horizontal = Space.xs, vertical = Space.xs),
                    value = AllSettings.terracottaNodes.state,
                    onValueChange = { AllSettings.terracottaNodes.save(it) },
                    placeholder = stringResource(R.string.terracotta_custom_note_list_hint),
                    singleLine = false
                )
            }
            CmcDivider(Modifier.padding(horizontal = Space.xs))
            CmcListAction(
                title = stringResource(R.string.terracotta_export_log_share),
                summary = stringResource(R.string.pe_multiplayer_share_summary),
                iconRes = R.drawable.ic_upload,
                endIconRes = R.drawable.ic_arrow_right_rounded,
                enabled = enabled,
                onClick = onShareLogs
            )
        }

        CmcSection(title = stringResource(R.string.pe_multiplayer_guide))
        GuideCard()
    }
}

@Composable
private fun StatusHeader(toggle: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    CmcCard(shape = CardShape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(FieldShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_group_filled),
                    contentDescription = null,
                    tint = colors.onPrimaryContainer
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                Text(
                    text = stringResource(R.string.terracotta_terracotta),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.pe_multiplayer_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
        CmcDivider(Modifier.padding(vertical = Space.sm))
        toggle()
    }
}

@Composable
private fun GuideCard() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.terracotta_confirm_title,
        R.string.terracotta_tutorial_host_tab,
        R.string.terracotta_tutorial_guest_tab
    )
    CmcPanel {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            tabs.forEachIndexed { index, title ->
                CmcChip(
                    text = stringResource(title),
                    selected = tab == index,
                    onClick = { tab = index }
                )
            }
        }
        CmcDivider(Modifier.padding(vertical = Space.sm))
        when (tab) {
            0 -> {
                StepList(
                    stringResource(R.string.terracotta_confirm_software),
                    stringResource(R.string.terracotta_confirm_p2p),
                    stringResource(R.string.terracotta_confirm_law)
                )
            }
            1 -> {
                StepList(
                    stringResource(R.string.terracotta_tutorial_step_enable_multiplayer),
                    stringResource(R.string.terracotta_tutorial_step_open_multiplayer_menu),
                    stringResource(R.string.terracotta_tutorial_host_step_become_host),
                    stringResource(R.string.terracotta_tutorial_host_step_open_lan),
                    stringResource(R.string.terracotta_tutorial_step_vpn_permission),
                    stringResource(R.string.terracotta_tutorial_host_step_copy_invite),
                    stringResource(R.string.terracotta_tutorial_host_step_send_invite)
                )
                NoteCard(
                    title = stringResource(R.string.terracotta_tutorial_note_title),
                    lines = listOf(
                        stringResource(R.string.terracotta_tutorial_step_offline_account_support),
                        stringResource(R.string.terracotta_tutorial_step_interoperability)
                    )
                )
            }
            2 -> {
                StepList(
                    stringResource(R.string.terracotta_tutorial_step_enable_multiplayer),
                    stringResource(R.string.terracotta_tutorial_step_open_multiplayer_menu),
                    stringResource(R.string.terracotta_tutorial_guest_step_become_guest),
                    stringResource(R.string.terracotta_tutorial_step_vpn_permission),
                    stringResource(R.string.terracotta_tutorial_guest_step_join_room)
                )
                NoteCard(
                    title = stringResource(R.string.terracotta_tutorial_note_title),
                    lines = listOf(
                        stringResource(R.string.terracotta_tutorial_step_offline_account_support),
                        stringResource(R.string.terracotta_tutorial_step_interoperability),
                        stringResource(R.string.terracotta_tutorial_guest_step_alternate_server)
                    )
                )
            }
        }
    }
}

@Composable
private fun StepList(vararg steps: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = Alpha.soft)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    modifier = Modifier.weight(1f).padding(top = 2.dp),
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun NoteCard(title: String, lines: List<String>) {
    Spacer(Modifier.size(Space.sm))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = Alpha.medium))
            .padding(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        lines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = Alpha.medium))
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}