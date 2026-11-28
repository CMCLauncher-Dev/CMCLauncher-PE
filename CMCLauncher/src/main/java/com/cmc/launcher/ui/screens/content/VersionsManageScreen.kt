package com.cmc.launcher.ui.screens.content

import android.os.Environment
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.launcher.R
import com.cmc.launcher.game.path.GamePath
import com.cmc.launcher.game.path.GamePathManager
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionComparator
import com.cmc.launcher.game.version.installed.VersionType
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.game.version.installed.cleanup.GameAssetCleaner
import com.cmc.launcher.ui.activities.MainActivity
import com.cmc.launcher.ui.androidText
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.SimpleListItem
import com.cmc.launcher.ui.kit.CmcButton
import com.cmc.launcher.ui.kit.CmcChip
import com.cmc.launcher.ui.kit.CmcListAction
import com.cmc.launcher.ui.kit.CmcPanel
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.content.elements.CleanupOperation
import com.cmc.launcher.ui.screens.content.elements.GamePathOperation
import com.cmc.launcher.ui.screens.content.elements.VersionCategory
import com.cmc.launcher.ui.screens.content.elements.VersionItemLayout
import com.cmc.launcher.ui.screens.content.elements.VersionsOperation
import com.cmc.launcher.ui.screens.content.transfer.ReceiveInstanceDialog
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.utils.canHandlePermission
import com.cmc.launcher.utils.checkStoragePermissions
import com.cmc.launcher.utils.string.getMessageOrToString
import com.cmc.launcher.viewmodel.ErrorViewModel
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import com.cmc.launcher.viewmodel.sendKeepScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class VersionCatalog(val versions: List<Version> = emptyList(), val vanilla: Int = 0, val modded: Int = 0)

private class VersionsScreenViewModel : ViewModel() {
    private val selectedCategory = MutableStateFlow(VersionCategory.ALL)
    val category = selectedCategory.asStateFlow()
    private val sortRevision = MutableStateFlow(0)
    val catalog = combine(VersionsManager.versions, sortRevision) { versions, _ ->
        VersionCatalog(versions.sortedWith(VersionComparator),
            versions.count { it.versionType == VersionType.VANILLA },
            versions.count { it.versionType == VersionType.MODLOADERS })
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VersionCatalog())
    val visibleVersions = combine(catalog, category) { catalog, category ->
        when (category) {
            VersionCategory.ALL -> catalog.versions
            VersionCategory.VANILLA -> catalog.versions.filter { it.versionType == VersionType.VANILLA }
            VersionCategory.MODLOADER -> catalog.versions.filter { it.versionType == VersionType.MODLOADERS }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var gamePathOperation by mutableStateOf<GamePathOperation>(GamePathOperation.None)
    var versionsOperation by mutableStateOf<VersionsOperation>(VersionsOperation.None)
    var cleanupOperation by mutableStateOf<CleanupOperation>(CleanupOperation.None)
    var cleaner by mutableStateOf<GameAssetCleaner?>(null)
    var selecting by mutableStateOf(false)
        private set
    private var stopKeepingScreen: (() -> Unit)? = null

    fun changeCategory(value: VersionCategory) { selectedCategory.value = value }
    var pinning by mutableStateOf<Set<Version>>(emptySet())
        private set

    fun togglePin(version: Version, report: (ErrorViewModel.ThrowableMessage) -> Unit) {
        if (version in pinning) return
        pinning = pinning + version
        val value = !version.pinnedState
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { version.setPinnedAndSave(value) }
                sortRevision.update { it + 1 }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                report(ErrorViewModel.ThrowableMessage(androidText(R.string.versions_config_failed_to_save), androidText(error.getMessageOrToString())))
            } finally {
                pinning = pinning - version
            }
        }
    }

    fun refresh() {
        if (!VersionsManager.isRefreshing.value) VersionsManager.refresh("VersionsScreen.refresh")
    }

    fun selectVersion(version: Version) {
        if (selecting || version == VersionsManager.currentVersion.value) return
        selecting = true
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) { VersionsManager.saveVersion(version) }
                if (!success) versionsOperation = VersionsOperation.InvalidDelete(version)
            } finally { selecting = false }
        }
    }

    fun changePath(path: GamePath, report: (ErrorViewModel.ThrowableMessage) -> Unit) {
        if (selecting || VersionsManager.isRefreshing.value) return
        selecting = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (path.id == GamePathManager.DEFAULT_ID) GamePathManager.saveDefaultPath()
                    else GamePathManager.saveCurrentPath(path.id)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                report(ErrorViewModel.ThrowableMessage(androidText(R.string.versions_manage_game_path_error_title), androidText(error.getMessageOrToString())))
            } finally { selecting = false }
        }
    }

    fun clean(onStart: () -> Unit, onStop: () -> Unit) {
        if (cleaner != null) return
        stopKeepingScreen = onStop
        onStart()
        cleanupOperation = CleanupOperation.Clean
        cleaner = GameAssetCleaner(viewModelScope).also { item ->
            item.start(onEnd = { count, size ->
                viewModelScope.launch { finishCleaning(CleanupOperation.Success(count, size)) }
            }, onThrowable = { error ->
                viewModelScope.launch { finishCleaning(CleanupOperation.Error(error)) }
            })
        }
    }

    private fun finishCleaning(result: CleanupOperation) {
        cleaner = null
        cleanupOperation = result
        stopKeepingScreen?.invoke()
        stopKeepingScreen = null
    }

    fun cancelCleaner() {
        cleaner?.cancel()
        finishCleaning(CleanupOperation.None)
    }

    override fun onCleared() {
        cancelCleaner()
        super.onCleared()
    }
}

@Composable
fun VersionsManageScreen(
    backScreenViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    navigateToExport: (Version) -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val model: VersionsScreenViewModel = viewModel(key = NormalNavKey.VersionsManager.toString()) { VersionsScreenViewModel() }
    if (backScreenViewModel.mainScreen.currentKey != NormalNavKey.VersionsManager) return
    val catalog by model.catalog.collectAsStateWithLifecycle()
    val versions by model.visibleVersions.collectAsStateWithLifecycle()
    val category by model.category.collectAsStateWithLifecycle()
    val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val refreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()
    val paths by GamePathManager.gamePathData.collectAsStateWithLifecycle()
    val currentPath by GamePathManager.currentPath.collectAsStateWithLifecycle()
    var showPaths by rememberSaveable { mutableStateOf(false) }
    var showReceive by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val activePath = paths.firstOrNull { it.path == currentPath }
    val pathTitle = if (activePath == null || activePath.id == GamePathManager.DEFAULT_ID)
        stringResource(R.string.versions_manage_game_path_default) else activePath.title
    val install: () -> Unit = {
        backScreenViewModel.downloadGameScreen.clearWith(NormalNavKey.DownloadGame.SelectGameVersion)
        backScreenViewModel.navigateToDownload(backScreenViewModel.downloadGameScreen)
    }
    GamePathOperation(model.gamePathOperation, { model.gamePathOperation = it }, submitError)
    VersionsOperation(model.versionsOperation, { model.versionsOperation = it }, submitError)
    CleanupOperation(model.cleanupOperation, { model.cleanupOperation = it }, model.cleaner,
        onClean = { model.clean({ eventViewModel.sendKeepScreen(true) }, { eventViewModel.sendKeepScreen(false) }) },
        onCancel = model::cancelCleaner, submitError = submitError)
    Column(Modifier.fillMaxSize()) {
        CmcPanel(Modifier.padding(horizontal = Space.screenH, vertical = Space.sm)) {
            CmcListAction(pathTitle, R.drawable.ic_folder_outlined, { showPaths = true }, summary = currentPath,
                endIconRes = R.drawable.ic_arrow_drop_down_rounded, enabled = !model.selecting)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.screenH), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            CmcButton(stringResource(R.string.versions_manage_install_new), install, Modifier.weight(1f))
            IconButton(enabled = !refreshing, onClick = model::refresh) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.generic_refresh))
            }
            IconButton(enabled = !refreshing, onClick = { showReceive = true }) {
                Icon(painterResource(R.drawable.ic_download), stringResource(R.string.pe_transfer_receive_title))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Space.screenH, vertical = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            VersionCategory.entries.forEach { item ->
                val count = when (item) {
                    VersionCategory.ALL -> catalog.versions.size
                    VersionCategory.VANILLA -> catalog.vanilla
                    VersionCategory.MODLOADER -> catalog.modded
                }
                CmcChip("${stringResource(item.textRes)} · $count", category == item, { model.changeCategory(item) })
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when {
            refreshing -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.pe_home_scanning)) }
            versions.isEmpty() -> Column(Modifier.weight(1f).fillMaxWidth().padding(Space.xl),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.versions_manage_no_versions), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = install) { Text(stringResource(R.string.versions_manage_install_new)) }
            }
            else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(Space.screenH),
                verticalArrangement = Arrangement.spacedBy(Space.md)) {
                items(versions, key = { it.getVersionPath().absolutePath }, contentType = { "version" }) { version ->
                    VersionItemLayout(version = version, selected = version == currentVersion, submitError = submitError,
                        modifier = Modifier.fillMaxWidth(), onSelected = { model.selectVersion(version) },
                        onSettingsClick = { navigateToVersions(version) },
                        onRenameClick = { model.versionsOperation = VersionsOperation.Rename(version) },
                        onCopyClick = { model.versionsOperation = VersionsOperation.Copy(version) },
                        onExportClick = { navigateToExport(version) },
                        onDeleteClick = { model.versionsOperation = VersionsOperation.Delete(version) },
                        pinEnabled = version !in model.pinning, onPinClick = { model.togglePin(version, submitError) })
                }
            }
        }
    }
    if (showReceive) {
        ReceiveInstanceDialog(versions = catalog.versions, initial = currentVersion, onDismiss = { showReceive = false })
    }
    if (showPaths) {
        DialogFrame(stringResource(R.string.versions_manage_game_path_default), { showPaths = false }, actions = {
            TextButton(enabled = !refreshing && !model.selecting && canHandlePermission, onClick = {
                (context as? MainActivity)?.let { activity ->
                    checkStoragePermissions(activity = activity, message = activity.getString(R.string.versions_manage_game_path_storage_permissions),
                        messageSdk30 = activity.getString(R.string.versions_manage_game_path_storage_permissions_sdk30), hasPermission = {
                            showPaths = false
                            backScreenViewModel.mainScreen.backStack.navigateToFileSelector(
                                startPath = Environment.getExternalStorageDirectory().absolutePath, selectFile = false,
                                saveKey = NormalNavKey.VersionsManager
                            ) { model.gamePathOperation = GamePathOperation.AddNewPath(it) }
                        })
                }
            }) { Text(stringResource(R.string.versions_manage_game_path_add_new)) }
            TextButton(onClick = { showPaths = false; model.cleanupOperation = CleanupOperation.Tip }) {
                Text(stringResource(R.string.versions_manage_cleanup))
            }
            TextButton(onClick = { showPaths = false }) { Text(stringResource(R.string.generic_close)) }
        }) {
            LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth(), contentPadding = PaddingValues(Space.md)) {
                items(paths, key = { it.id }) { path ->
                    var actions by remember(path.id) { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SimpleListItem(Modifier.weight(1f), selected = path.path == currentPath,
                            itemName = if (path.id == GamePathManager.DEFAULT_ID) stringResource(R.string.versions_manage_game_path_default) else path.title,
                            summary = { Text(path.path, style = MaterialTheme.typography.bodySmall) },
                            enabled = !refreshing && !model.selecting && canHandlePermission, onClick = {
                                val select = { model.changePath(path, submitError); showPaths = false }
                                if (path.id == GamePathManager.DEFAULT_ID) select()
                                else (context as? MainActivity)?.let { activity ->
                                    checkStoragePermissions(activity = activity, message = activity.getString(R.string.versions_manage_game_storage_permissions),
                                        messageSdk30 = activity.getString(R.string.versions_manage_game_storage_permissions_sdk30), hasPermission = select)
                                }
                            })
                        if (path.id != GamePathManager.DEFAULT_ID) IconButton(onClick = { actions = !actions }) {
                            Icon(painterResource(R.drawable.ic_more_horiz), stringResource(R.string.generic_more))
                        }
                    }
                    if (actions) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPaths = false; model.gamePathOperation = GamePathOperation.RenamePath(path) }) {
                            Text(stringResource(R.string.generic_rename))
                        }
                        TextButton(onClick = { showPaths = false; model.gamePathOperation = GamePathOperation.DeletePath(path) }) {
                            Text(stringResource(R.string.generic_delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}