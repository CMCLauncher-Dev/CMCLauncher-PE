package com.cmc.launcher.ui.screens.content.versions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import com.cmc.launcher.ui.components.StaticDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.cmc.launcher.ui.components.StaticBusyIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.launcher.R
import com.cmc.launcher.coroutine.TaskSystem
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionFolders
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.components.CardTitleLayout
import com.cmc.launcher.ui.components.EdgeDirection
import com.cmc.launcher.ui.components.IconTextButton
import com.cmc.launcher.ui.components.ProgressDialog
import com.cmc.launcher.ui.components.ScalingLabel
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.components.SimpleTextInputField
import com.cmc.launcher.ui.components.animated
import com.cmc.launcher.ui.components.fadeEdge
import com.cmc.launcher.ui.components.rememberAnimatedContext
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.elements.ImportMultipleFileButton
import com.cmc.launcher.ui.screens.content.elements.SortByDropdownMenu
import com.cmc.launcher.ui.screens.content.elements.SortByEnum
import com.cmc.launcher.ui.screens.content.elements.rememberMultipleUriImportTaskBuilder
import com.cmc.launcher.ui.screens.content.versions.elements.DeleteAllOperation
import com.cmc.launcher.ui.screens.content.versions.elements.FileNameInputDialog
import com.cmc.launcher.ui.screens.content.versions.elements.LoadingState
import com.cmc.launcher.ui.screens.content.versions.elements.ShaderOperation
import com.cmc.launcher.ui.screens.content.versions.elements.ShaderPackInfo
import com.cmc.launcher.ui.screens.content.versions.elements.filterShaders
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.ui.screens.content.versions.layouts.LibrarySelection
import com.cmc.launcher.ui.screens.content.versions.layouts.PackToolbar
import com.cmc.launcher.ui.screens.content.versions.layouts.rememberPackImport
import com.cmc.launcher.ui.screens.content.versions.layouts.VersionChunkBackground
import com.cmc.launcher.ui.theme.itemColor
import com.cmc.launcher.ui.theme.onItemColor
import com.cmc.launcher.utils.animation.getAnimateTween
import com.cmc.launcher.utils.file.FolderFileCounter
import com.cmc.launcher.utils.file.formatFileSize
import com.cmc.launcher.viewmodel.ErrorViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

private class ShadersManageViewModel(
    val shadersDir: File
) : ViewModel() {
    var nameFilter by mutableStateOf("")

    var allShaders by mutableStateOf<List<ShaderPackInfo>>(emptyList())
        private set
    var filteredShaders by mutableStateOf<List<ShaderPackInfo>?>(null)
        private set
    var sortByEnum by mutableStateOf(SortByEnum.FileName)
        private set
    var isAscending by mutableStateOf(true)
        private set

    var shadersState by mutableStateOf<LoadingState>(LoadingState.None)
        private set

    val selectedPacks = mutableStateListOf<ShaderPackInfo>()

    var deleteAllOperation by mutableStateOf<DeleteAllOperation>(DeleteAllOperation.None)

    private var packCount = FolderFileCounter(shadersDir)

    fun selectAllFiles() {
        filteredShaders?.forEach { pack ->
            if (!selectedPacks.contains(pack)) selectedPacks.add(pack)
        }
    }

    fun clearSelected() {
        filteredShaders?.let {
            selectedPacks.removeAll(it)
        }
    }

    private var job: Job? = null

    fun refresh(
        checkCount: Boolean = true
    ) {
        job = viewModelScope.launch {
            shadersState = LoadingState.Loading
            selectedPacks.clear()
            if (checkCount) packCount.checkDir()

            withContext(Dispatchers.IO) {
                try {
                    val list = shadersDir.listFiles()?.filter {

                        it.isFile && it.extension.equals("zip", true)
                    }?.map { file ->
                        ensureActive()
                        ShaderPackInfo(
                            file = file,
                            fileSize = FileUtils.sizeOf(file)
                        )
                    } ?: emptyList()
                    allShaders = list.sortedBy { it.file.name }
                    filterShaders()
                } catch (_: CancellationException) {
                    return@withContext
                }
            }

            shadersState = LoadingState.None
            job = null
        }
    }

    fun checkCountAndRefresh() {
        val isUnchecked = packCount.isUnchecked()
        if (packCount.checkDir() && !isUnchecked && job == null) {
            refresh(checkCount = false)
        }
    }

    init {
        refresh(checkCount = false)
    }

    fun updateFilter(name: String) {
        this.nameFilter = name
        filterShaders()
    }

    fun updateSortBy(sortByEnum: SortByEnum) {
        this.sortByEnum = sortByEnum
        filterShaders()
    }

    fun updateSortOrder() {
        this.isAscending = !this.isAscending
        filterShaders()
    }

    val supportedSortByEnums = listOf(
        SortByEnum.FileName, SortByEnum.FileModifiedTime
    )

    private var filterJob: Job? = null

    private fun filterShaders() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            val result = allShaders
                .takeIf { it.isNotEmpty() }
                ?.filterShaders(nameFilter)
                ?.sortedWith { o1, o2 ->
                    val file1 = o1.file
                    val file2 = o2.file
                    val value = when (sortByEnum) {
                        SortByEnum.FileName -> file1.name.compareTo(file2.name)
                        SortByEnum.FileModifiedTime -> file2.lastModified().compareTo(file1.lastModified())
                        else -> error("This sorting method is not supported: $sortByEnum")
                    }
                    if (isAscending) {
                        value
                    } else {
                        -value
                    }
                }
            if (!isActive) return@launch
            filteredShaders = result
        }
    }
}

@Composable
private fun rememberShadersManageViewModel(
    shadersDir: File,
    version: Version
) = viewModel(
    key = version.toString() + "_" + VersionFolders.SHADERS.folderName
) {
    ShadersManageViewModel(shadersDir)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShadersManagerScreen(
    mainScreenKey: TitledNavKey?,
    versionsScreenKey: TitledNavKey?,
    version: Version,
    backToMainScreen: () -> Unit,
    swapToDownload: () -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    if (!version.isValid()) {
        backToMainScreen()
        return
    }

    val shadersDir = remember(version) {
        VersionFolders.SHADERS.getDir(version.getGameDir())
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.ShadersManager, versionsScreenKey, false),
    ) { isVisible ->
        val viewModel = rememberShadersManageViewModel(shadersDir, version)

        LaunchedEffect(Unit) {
            viewModel.checkCountAndRefresh()
        }

        DeleteAllOperation(
            operation = viewModel.deleteAllOperation,
            changeOperation = { viewModel.deleteAllOperation = it },
            submitError = submitError,
            onRefresh = { viewModel.refresh() }
        )

        val ctx = rememberAnimatedContext(isVisible = isVisible)

        VersionChunkBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
                .animated(ctx),
            paddingValues = PaddingValues()
        ) {
            val operationScope = rememberCoroutineScope()

            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = false,
                enabled = viewModel.shadersState !is LoadingState.Loading,
                onRefresh = viewModel::refresh,
            ) {
                when (viewModel.shadersState) {
                    LoadingState.None -> {
                        var shaderOperation by remember { mutableStateOf<ShaderOperation>(ShaderOperation.None) }
                        fun runProgress(task: () -> Unit) {
                            operationScope.launch(Dispatchers.IO) {
                                shaderOperation = ShaderOperation.Progress
                                task()
                                shaderOperation = ShaderOperation.None
                                viewModel.refresh()
                            }
                        }
                        ShaderOperation(
                            shaderOperation = shaderOperation,
                            updateOperation = { shaderOperation = it },
                            shadersDir = shadersDir,
                            renameShaderPack = { info, newName ->
                                runProgress {
                                    val file = info.file
                                    file.renameTo(File(shadersDir, "$newName.${file.extension}"))
                                }
                            },
                            deleteShaderPack = { info ->
                                runProgress {
                                    FileUtils.deleteQuietly(info.file)
                                }
                            }
                        )

                        Column {
                            PackToolbar(
                                modifier = Modifier.fillMaxWidth(),
                                query = viewModel.nameFilter,
                                onQuery = viewModel::updateFilter,
                                sortOptions = viewModel.supportedSortByEnums,
                                sortBy = viewModel.sortByEnum,
                                onSortBy = viewModel::updateSortBy,
                                ascending = viewModel.isAscending,
                                onReverse = viewModel::updateSortOrder,
                                selection = LibrarySelection(
                                    count = viewModel.selectedPacks.size,
                                    selectAll = viewModel::selectAllFiles,
                                    clear = viewModel::clearSelected,
                                    requestDelete = {
                                        val files = viewModel.selectedPacks.map { it.file }
                                        if (viewModel.deleteAllOperation == DeleteAllOperation.None && files.isNotEmpty()) {
                                            viewModel.deleteAllOperation = DeleteAllOperation.Warning(files = files)
                                        }
                                    }
                                ),
                                onDownload = swapToDownload,
                                onImport = rememberPackImport("ContentManager.Shaders.Import", shadersDir, submitError) { viewModel.refresh() },
                                onRefresh = { viewModel.refresh() }
                            )

                            ShadersList(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shadersList = viewModel.filteredShaders,
                                selectedPacks = viewModel.selectedPacks,
                                removeFromSelected = { viewModel.selectedPacks.remove(it) },
                                addToSelected = { viewModel.selectedPacks.add(it) },
                                updateOperation = { shaderOperation = it }
                            )
                        }
                    }
                    LoadingState.Loading -> {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            StaticBusyIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShadersList(
    modifier: Modifier = Modifier,
    shadersList: List<ShaderPackInfo>?,
    selectedPacks: List<ShaderPackInfo>,
    removeFromSelected: (ShaderPackInfo) -> Unit,
    addToSelected: (ShaderPackInfo) -> Unit,
    updateOperation: (ShaderOperation) -> Unit
) {
    shadersList?.let { list ->
        if (list.isNotEmpty()) {
            val scrollState = rememberLazyListState()
            LazyColumn(
                modifier = modifier.nonInteractiveScrollbar(
                    state = scrollState.scrollIndicatorState!!,
                    orientation = Orientation.Vertical,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                state = scrollState,
            ) {
                items(list, key = { it.file.absolutePath }) { info ->
                    ShaderPackItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shaderPackInfo = info,
                        selected = selectedPacks.contains(info),
                        onClick = {
                            if (selectedPacks.contains(info)) {
                                removeFromSelected(info)
                            } else {
                                addToSelected(info)
                            }
                        },
                        updateOperation = updateOperation
                    )
                }
            }
        } else {

            Box(modifier = Modifier.fillMaxSize()) {
                ScalingLabel(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.generic_no_matching_items)
                )
            }
        }
    } ?: run {

        Box(modifier = Modifier.fillMaxSize()) {
            ScalingLabel(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.shader_pack_manage_no_packs)
            )
        }
    }
}

@Composable
private fun ShaderPackItem(
    modifier: Modifier = Modifier,
    shaderPackInfo: ShaderPackInfo,
    selected: Boolean,
    onClick: () -> Unit = {},
    updateOperation: (ShaderOperation) -> Unit,
    itemColor: Color = itemColor(),
    itemContentColor: Color = onItemColor(),
    borderColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = MaterialTheme.shapes.large,
) {
    Surface(
        modifier = modifier,
        checked = selected,
        onCheckedChange = { onClick() },
        border = if (selected) BorderStroke(1.dp, borderColor) else null,
        shape = shape,
        color = itemColor,
        contentColor = itemContentColor,
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f),
            ) {

                Text(
                    text = shaderPackInfo.file.name,
                    style = MaterialTheme.typography.titleSmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )

                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = stringResource(
                        R.string.generic_file_size,
                        formatFileSize(shaderPackInfo.fileSize)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShadersOperationMenu(
                    buttonSize = 38.dp,
                    iconSize = 26.dp,
                    onRenameClick = {
                        updateOperation(ShaderOperation.Rename(shaderPackInfo))
                    },
                    onDeleteClick = {
                        updateOperation(ShaderOperation.Delete(shaderPackInfo))
                    }
                )
            }
        }
    }
}

@Composable
private fun ShadersOperationMenu(
    buttonSize: Dp,
    iconSize: Dp = buttonSize,
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Row {
        var menuExpanded by remember { mutableStateOf(false) }

        IconButton(
            modifier = Modifier.size(buttonSize),
            onClick = { menuExpanded = !menuExpanded }
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(R.drawable.ic_more_horiz),
                contentDescription = stringResource(R.string.generic_more)
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 3.dp,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.generic_rename)) },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_edit_filled),
                        contentDescription = stringResource(R.string.generic_rename)
                    )
                },
                onClick = {
                    onRenameClick()
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.generic_delete)) },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_delete_filled),
                        contentDescription = stringResource(R.string.generic_delete)
                    )
                },
                onClick = {
                    onDeleteClick()
                    menuExpanded = false
                }
            )
        }
    }
}

@Composable
private fun ShaderOperation(
    shaderOperation: ShaderOperation,
    updateOperation: (ShaderOperation) -> Unit,
    shadersDir: File,
    renameShaderPack: (ShaderPackInfo, String) -> Unit,
    deleteShaderPack: (ShaderPackInfo) -> Unit
) {
    when (shaderOperation) {
        is ShaderOperation.None -> {}
        is ShaderOperation.Progress -> {
            ProgressDialog()
        }
        is ShaderOperation.Rename -> {
            val info = shaderOperation.info
            FileNameInputDialog(
                initValue = info.file.nameWithoutExtension,
                existsCheck = { value ->
                    File(shadersDir, "$value.${info.file.extension}").exists()
                },
                existsMessage = stringResource(R.string.shader_pack_manage_exists),
                title = stringResource(R.string.generic_rename),
                label = stringResource(R.string.shader_pack_manage_name),
                onDismissRequest = {
                    updateOperation(ShaderOperation.None)
                },
                onConfirm = { newName ->
                    renameShaderPack(info, newName)
                    updateOperation(ShaderOperation.None)
                }
            )
        }
        is ShaderOperation.Delete -> {
            val info = shaderOperation.info
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.shader_pack_manage_delete_warning, info.file.name),
                onDismiss = {
                    updateOperation(ShaderOperation.None)
                },
                onConfirm = {
                    deleteShaderPack(info)
                    updateOperation(ShaderOperation.None)
                }
            )
        }
    }
}
