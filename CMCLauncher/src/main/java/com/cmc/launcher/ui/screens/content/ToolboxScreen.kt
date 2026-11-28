package com.cmc.launcher.ui.screens.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmc.launcher.R
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.kit.CmcListAction
import com.cmc.launcher.ui.kit.CmcPanel
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.content.transfer.ReceiveInstanceDialog
import com.cmc.launcher.ui.screens.content.transfer.SendInstanceDialog
import com.cmc.launcher.ui.screens.navigateTo
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel

private enum class TransferPage { Role, Send, Receive }

@Composable
fun ToolboxScreen(backStackViewModel: ScreenBackStackViewModel) {
    val main = backStackViewModel.mainScreen
    var transfer by rememberSaveable { mutableStateOf<TransferPage?>(null) }
    BaseScreen(screenKey = NormalNavKey.Toolbox, currentKey = main.currentKey) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            CmcPanel(Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(Space.screenH)) {
                CmcListAction(
                    title = stringResource(R.string.terracotta_terracotta),
                    iconRes = R.drawable.ic_group_filled,
                    summary = stringResource(R.string.pe_multiplayer_subtitle),
                    endIconRes = R.drawable.ic_arrow_right_rounded,
                    onClick = { main.navigateTo(NormalNavKey.Multiplayer) }
                )
                CmcListAction(
                    title = stringResource(R.string.pe_transfer_tool),
                    iconRes = R.drawable.ic_package_2_outlined,
                    summary = stringResource(R.string.pe_transfer_tool_detail),
                    endIconRes = R.drawable.ic_arrow_right_rounded,
                    onClick = { transfer = TransferPage.Role }
                )
            }
        }
        when (transfer) {
            TransferPage.Role -> DialogFrame(
                title = stringResource(R.string.pe_transfer_tool),
                onDismiss = { transfer = null },
                actions = {}
            ) {
                Column(
                    Modifier.padding(horizontal = Space.xl, vertical = Space.md),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    CmcListAction(
                        title = stringResource(R.string.pe_transfer_send_title),
                        iconRes = R.drawable.ic_upload,
                        onClick = { transfer = TransferPage.Send }
                    )
                    CmcListAction(
                        title = stringResource(R.string.pe_transfer_receive_title),
                        iconRes = R.drawable.ic_download,
                        onClick = { transfer = TransferPage.Receive }
                    )
                }
            }
            TransferPage.Send, TransferPage.Receive -> {
                val versions by VersionsManager.versions.collectAsStateWithLifecycle()
                if (transfer == TransferPage.Send) {
                    val current by VersionsManager.currentVersion.collectAsStateWithLifecycle()
                    SendInstanceDialog(versions, current) { transfer = TransferPage.Role }
                } else {
                    ReceiveInstanceDialog(versions, initial = null) { transfer = TransferPage.Role }
                }
            }
            null -> Unit
        }
    }
}
