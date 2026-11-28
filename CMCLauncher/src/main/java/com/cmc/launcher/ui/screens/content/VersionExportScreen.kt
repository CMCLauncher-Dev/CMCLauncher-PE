package com.cmc.launcher.ui.screens.content

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import com.cmc.launcher.R
import com.cmc.launcher.game.version.export.ExportInfo
import com.cmc.launcher.game.version.export.PackExporter
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.ui.base.VersionViewModelStoreScope
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.screens.NestedNavDisplay
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.clearKeys
import com.cmc.launcher.ui.screens.content.elements.TitleTaskFlowDialog
import com.cmc.launcher.ui.screens.content.versions.export.ExportInfoScreen
import com.cmc.launcher.ui.screens.content.versions.export.ExportSelectFilesScreen
import com.cmc.launcher.ui.screens.content.versions.export.ExportTypeSelectScreen
import com.cmc.launcher.ui.screens.navigateTo
import com.cmc.launcher.ui.screens.onBack
import com.cmc.launcher.ui.screens.rememberTransitionSpec
import com.cmc.launcher.utils.logging.Logger
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import com.cmc.launcher.viewmodel.sendKeepScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun rememberExportModpackViewModel(version: Version): ExportModpackViewModel = viewModel(
    key = version.toString() + "_ExportModpack"
) {
    val info = checkNotNull(version.getVersionInfo())
    ExportModpackViewModel(
        mcVersion = info.minecraftVersion, versionName = version.getVersionName(), gamePath = version.getGameDir(),
        loader = info.primaryLoader?.let { ExportInfo.LoaderVersion(it.loader, it.version) },
        gameArgs = version.getGameArgs(), javaArgs = version.getJvmArgs()
    )
}

@Composable
fun VersionExportScreen(
    key: NestedNavKey.VersionExport,
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    backToMainScreen: () -> Unit
) {
    val leave by rememberUpdatedState(backToMainScreen)
    if (!key.version.isValid() || key.version.getVersionInfo() == null) {
        LaunchedEffect(key) { leave() }
        return
    }
    DisposableEffect(key.version) {
        val listener: suspend () -> Unit = {
            if (!key.version.isValid()) withContext(Dispatchers.Main) { leave() }
        }
        VersionsManager.registerListener(listener)
        onDispose { VersionsManager.unregisterListener(listener) }
    }
    VersionViewModelStoreScope(key.version) {
        val model = rememberExportModpackViewModel(key.version)
        val operation by model.packExportOperation.collectAsStateWithLifecycle()
        val exporter by model.packExporter.collectAsStateWithLifecycle()
        val stack = key.backStack
        val top = stack.lastOrNull()
        val main = backScreenViewModel.mainScreen.backStack.lastOrNull()
        if (main == key) {
            NestedNavDisplay(
                ownerKey = key, backStack = stack, modifier = Modifier.fillMaxSize(),
                onBack = { if (operation != PackExportOperation.Exporting) onBack(stack) },
                transitionSpec = rememberTransitionSpec(), popTransitionSpec = rememberTransitionSpec(),
                entryProvider = entryProvider {
                    entry<NormalNavKey.VersionExports.SelectType> {
                        ExportTypeSelectScreen(main, top) { format ->
                            model.selectType(format)
                            stack.navigateTo(NormalNavKey.VersionExports.EditInfo)
                        }
                    }
                    entry<NormalNavKey.VersionExports.EditInfo> {
                        val info by model.exportInfo.collectAsStateWithLifecycle()
                        ExportInfoScreen(info, model::editInfo, onFinishClick = {
                            model.prepareFiles()
                            stack.navigateTo(NormalNavKey.VersionExports.SelectFiles)
                        }, mainScreenKey = main, exportScreenKey = top)
                    }
                    entry<NormalNavKey.VersionExports.SelectFiles> {
                        ExportFilesPage(key, model, backScreenViewModel, eventViewModel, leave)
                    }
                }
            )
            ExportOperationDialog(operation, exporter, model::cancelExport) { model.updateOperation(PackExportOperation.None) }
        }
    }
}

@Composable
private fun ExportFilesPage(
    key: NestedNavKey.VersionExport, model: ExportModpackViewModel, navigation: ScreenBackStackViewModel,
    events: EventViewModel, leave: () -> Unit
) {
    val files by model.allFiles.collectAsStateWithLifecycle()
    val selected by model.selectedFiles.collectAsStateWithLifecycle()
    val scanning by model.isRefreshingFiles.collectAsStateWithLifecycle()
    val selecting by model.selectingFolder.collectAsStateWithLifecycle()
    val changing by model.changingSelection.collectAsStateWithLifecycle()
    val operation by model.packExportOperation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var explanation by remember(key) { mutableStateOf(false) }
    val chooseFolder = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        model.updateSelecting(false)
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val uri = intent?.data
            if (uri != null) {
                try {
                    val flags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    check(flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0) { context.getString(R.string.pe_export_folder_denied) }
                    if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
                        context.contentResolver.takePersistableUriPermission(uri, flags)
                    }
                    model.startExport(key.version, context, uri,
                        onStart = { events.sendKeepScreen(true) }, onStop = { events.sendKeepScreen(false) },
                        onFinished = { key.backStack.clearKeys(NormalNavKey.VersionExports.EditInfo, NormalNavKey.VersionExports.SelectFiles) })
                } catch (error: Exception) {
                    model.updateOperation(PackExportOperation.Error(error))
                }
            }
        }
    }
    ExportSelectFilesScreen(
        allFiles = files, selectedFiles = selected, onRefreshRootSelect = model::refreshRootSelect,
        isSelectingFolder = selecting || operation == PackExportOperation.Exporting, isRefreshingFiles = scanning,
        mainScreenKey = navigation.mainScreen.backStack.lastOrNull(), exportScreenKey = key.backStack.lastOrNull(),
        version = key.version, backToMainScreen = leave, onFinish = { explanation = true },
        isChangingSelection = changing, onFileSelection = model::setFileSelection
    )
    if (explanation) SimpleAlertDialog(
        title = stringResource(R.string.versions_export_pack_select_folder_title),
        text = stringResource(R.string.versions_export_pack_select_folder_message),
        onDismiss = { explanation = false },
        onConfirm = {
            explanation = false
            model.updateSelecting(true)
            try {
                chooseFolder.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                ))
            } catch (error: Exception) {
                model.updateSelecting(false)
                model.updateOperation(PackExportOperation.Error(error))
            }
        }
    )
}

@Composable
private fun ExportOperationDialog(operation: PackExportOperation, exporter: PackExporter?, cancel: () -> Unit, dismiss: () -> Unit) {
    when (operation) {
        PackExportOperation.None -> Unit
        PackExportOperation.Finished -> SimpleAlertDialog(
            title = stringResource(R.string.versions_export), text = stringResource(R.string.versions_export_task_finished), onDismiss = dismiss
        )
        is PackExportOperation.Error -> {
            LaunchedEffect(operation) { Logger.error("VersionExport", "Failed to export modpack", operation.throwable) }
            SimpleAlertDialog(title = stringResource(R.string.versions_export_task_error_title), text = {
                Text(stringResource(R.string.versions_export_task_error_message))
                Text(operation.throwable.localizedMessage ?: stringResource(R.string.generic_error))
            }, onDismiss = dismiss)
        }
        PackExportOperation.Exporting -> {
            if (exporter == null) {
                PreparingExportDialog(cancel)
            } else {
                val tasks by exporter.taskFlow.collectAsStateWithLifecycle()
                if (tasks.isEmpty()) PreparingExportDialog(cancel)
                else TitleTaskFlowDialog(title = stringResource(R.string.versions_export), tasks = tasks, onCancel = cancel)
            }
        }
    }
}

@Composable
private fun PreparingExportDialog(cancel: () -> Unit) {
    SimpleAlertDialog(title = stringResource(R.string.versions_export), text = stringResource(R.string.generic_in_progress),
        confirmText = stringResource(R.string.generic_cancel), dismissByDialog = false, onDismiss = cancel)
}