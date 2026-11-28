package com.cmc.launcher.ui.screens.content

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cmc.launcher.game.download.engine.DownloadEngine
import com.cmc.launcher.game.download.engine.DownloadRequest
import com.cmc.launcher.game.update.UpdateChecker
import com.cmc.launcher.game.update.UpdateRelease
import com.cmc.launcher.setting.enums.UpdateChannel
import com.cmc.launcher.ui.components.SimpleAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.runtime.rememberCoroutineScope
import com.cmc.launcher.R
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.setting.enums.AppLanguage
import com.cmc.launcher.setting.enums.applyLanguage
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.kit.CmcDivider
import com.cmc.launcher.ui.kit.CmcListAction
import com.cmc.launcher.ui.kit.CmcPanel
import com.cmc.launcher.ui.kit.CmcRadioRow
import com.cmc.launcher.ui.kit.CmcSection
import com.cmc.launcher.ui.screens.NestedNavDisplay
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.content.settings.JavaManageScreen
import com.cmc.launcher.ui.screens.content.settings.MirrorSettingScreen
import com.cmc.launcher.ui.screens.content.transfer.ReceiveInstanceDialog
import com.cmc.launcher.ui.screens.onBack
import com.cmc.launcher.ui.screens.rememberTransitionSpec
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.utils.CrashReporter
import com.cmc.launcher.utils.file.shareFile
import com.cmc.launcher.viewmodel.ErrorViewModel
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel

@Composable
fun SettingsScreen(
    key: NestedNavKey.Settings,
    backStackViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val parent = backStackViewModel.mainScreen
    val page = key.backStack.lastOrNull() ?: return

    BaseScreen(screenKey = key, currentKey = parent.currentKey) {
        NestedNavDisplay(
            ownerKey = key,
            backStack = key.backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = { onBack(parent.backStack) },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.Settings.Home> {
                    SettingsHome(onOpen = { target -> key.navigateTo(target) })
                }
                entry<NormalNavKey.Settings.Mirror> {
                    MirrorSettingScreen(key, page, parent.currentKey)
                }
                entry<NormalNavKey.Settings.JavaManager> {
                    JavaManageScreen(key, page, parent.currentKey, eventViewModel, submitError)
                }
            }
        )
    }
}

@Composable
private fun SettingsHome(onOpen: (NormalNavKey.Settings) -> Unit) {
    var languageExpanded by rememberSaveable { mutableStateOf(false) }
    var showReceive by rememberSaveable { mutableStateOf(false) }
    val language = AllSettings.launcherLanguage.state
    val channel = AllSettings.updateChannel.state
    var updateRelease by remember { mutableStateOf<UpdateRelease?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    updateRelease?.let { release ->
        SimpleAlertDialog(
            title = release.name,
            text = stringResource(R.string.pe_update_found, release.tag) +
                    if (release.prerelease) "\n" + stringResource(R.string.pe_update_channel_beta) else "",
            confirmText = stringResource(R.string.pe_update_open),
            onConfirm = {
                val url = release.apkUrl
                if (url == null) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                } else {
                    Toast.makeText(context, R.string.pe_update_downloading, Toast.LENGTH_SHORT).show()
                    scope.launch {
                        val file = runCatching {
                            withContext(Dispatchers.IO) {
                                val target = File(context.cacheDir, "update-${release.tag}.apk")
                                DownloadEngine.download(DownloadRequest(listOf(url), target))
                                target
                            }
                        }.getOrNull()
                        if (file != null && file.length() > 0) {
                            runCatching {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW)
                                        .setDataAndType(uri, "application/vnd.android.package-archive")
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure {
                                Toast.makeText(context, R.string.pe_update_failed, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, R.string.pe_update_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                updateRelease = null
            },
            onDismiss = { updateRelease = null }
        )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Space.screenH, end = Space.screenH, bottom = Space.xxl
            )
        ) {
            item {
                CmcSection(title = stringResource(R.string.pe_settings_launcher))
                CmcPanel {
                    CmcListAction(
                        title = stringResource(R.string.pe_settings_appearance),
                        summary = stringResource(R.string.pe_settings_appearance_detail),
                        iconRes = R.drawable.ic_format_paint_outlined,
                        endIconRes = R.drawable.ic_arrow_right_rounded,
                        onClick = { onOpen(NormalNavKey.Settings.Mirror) }
                    )
                    CmcDivider()
                    CmcListAction(
                        title = stringResource(R.string.settings_launcher_language),
                        summary = stringResource(language.textRes),
                        iconRes = R.drawable.ic_public,
                        endIconRes = if (languageExpanded) R.drawable.ic_arrow_drop_up_rounded else R.drawable.ic_arrow_drop_down_rounded,
                        onClick = { languageExpanded = !languageExpanded }
                    )
                    if (languageExpanded) {
                        AppLanguage.available.forEach { option ->
                            CmcRadioRow(
                                text = stringResource(option.textRes),
                                selected = language == option,
                                onClick = {
                                    languageExpanded = false
                                    AllSettings.launcherLanguage.save(option)
                                    applyLanguage(option)
                                }
                            )
                        }
                    }
                }
            }
            item {
                CmcSection(title = stringResource(R.string.pe_settings_migration))
                CmcPanel {
                    CmcListAction(
                        title = stringResource(R.string.pe_transfer_receive_title),
                        summary = stringResource(R.string.pe_settings_migration_detail),
                        iconRes = R.drawable.ic_download,
                        endIconRes = R.drawable.ic_arrow_right_rounded,
                        onClick = { showReceive = true }
                    )
                }
            }
            item {
                CmcSection(title = stringResource(R.string.pe_settings_runtime))
                CmcPanel {
                    CmcListAction(
                        title = stringResource(R.string.settings_tab_java_manage),
                        summary = stringResource(R.string.pe_settings_java_detail),
                        iconRes = R.drawable.ic_java,
                        endIconRes = R.drawable.ic_arrow_right_rounded,
                        onClick = { onOpen(NormalNavKey.Settings.JavaManager) }
                    )
                }
            }
            item {
                CmcSection(title = stringResource(R.string.pe_settings_diagnostics))
                CmcPanel {
                    CmcListAction(
                        title = stringResource(R.string.pe_settings_diagnostics_title),
                        summary = stringResource(R.string.pe_settings_diagnostics_detail),
                        iconRes = R.drawable.ic_article_outlined,
                        endIconRes = R.drawable.ic_arrow_right_rounded,
                        onClick = {
                            scope.launch {
                                val file = withContext(Dispatchers.IO) { CrashReporter.createBundle(context) }
                                if (file != null) {
                                    shareFile(context, file) {
                                        Toast.makeText(context, R.string.pe_diagnostics_export_failed, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, R.string.pe_diagnostics_export_failed, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
            item {
                CmcSection(title = stringResource(R.string.pe_settings_update))
                CmcPanel {
                    UpdateChannel.entries.forEach { option ->
                        CmcRadioRow(
                            text = stringResource(option.textRes),
                            selected = channel == option,
                            onClick = { AllSettings.updateChannel.save(option) }
                        )
                    }
                    CmcDivider()
                    CmcListAction(
                        title = stringResource(R.string.pe_update_check),
                        summary = stringResource(R.string.pe_update_check_summary),
                        iconRes = R.drawable.ic_autorenew,
                        endIconRes = R.drawable.ic_arrow_right_rounded,
                        onClick = {
                            scope.launch {
                                val release = withContext(Dispatchers.IO) { UpdateChecker.latest(channel) }
                                if (release != null) {
                                    val installedAt = runCatching {
                                        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
                                    }.getOrDefault(0L)
                                    if (release.publishedAt > installedAt) {
                                        updateRelease = release
                                    } else {
                                        Toast.makeText(context, R.string.pe_update_latest, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, R.string.pe_update_check_failed, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    if (showReceive) {
        val versions by VersionsManager.versions.collectAsStateWithLifecycle()
        ReceiveInstanceDialog(
            versions = versions,
            initial = null,
            onDismiss = { showReceive = false }
        )
    }
}
