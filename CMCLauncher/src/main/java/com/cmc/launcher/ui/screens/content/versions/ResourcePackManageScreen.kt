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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.cmc.launcher.ui.components.StaticDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.cmc.launcher.ui.components.StaticBusyIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
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
import androidx.compose.ui.draw.clip
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
import com.cmc.launcher.game.version.resource_pack.ResourcePackInfo
import com.cmc.launcher.game.version.resource_pack.parseResourcePack
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.components.CardTitleLayout
import com.cmc.launcher.ui.components.ContentCheckBox
import com.cmc.launcher.ui.components.EdgeDirection
import com.cmc.launcher.ui.components.IconTextButton
import com.cmc.launcher.ui.components.ProgressDialog
import com.cmc.launcher.ui.components.ScalingLabel
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.components.SimpleTextInputField
import com.cmc.launcher.ui.components.TooltipIconButton
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
import com.cmc.launcher.ui.screens.content.versions.elements.ByteArrayIcon
import com.cmc.launcher.ui.screens.content.versions.elements.DeleteAllOperation
import com.cmc.launcher.ui.screens.content.versions.elements.FileNameInputDialog
import com.cmc.launcher.ui.screens.content.versions.elements.LoadingState
import com.cmc.launcher.ui.screens.content.versions.elements.MinecraftColorTextNormal
import com.cmc.launcher.ui.screens.content.versions.elements.ResourcePackFilter
import com.cmc.launcher.ui.screens.content.versions.elements.ResourcePackOperation
import com.cmc.launcher.ui.screens.content.versions.elements.filterPacks
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Checkbox
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

private class ResourcePackManageViewModel(
    val resourcePackDir: File
) : ViewModel() {
    var packFilter by mutableStateOf(ResourcePackFilter(false, ""))
        private set

    var allPacks by mutableStateOf<List<ResourcePackInfo>>(emptyList())
        private set
    var filteredPacks by mutableStateOf<List<ResourcePackInfo>?>(null)
        private set
    var sortByEnum by mutableStateOf(SortByEnum.Name)
        private set
    var isAscending by mutableStateOf(true)
        private set

    var packState by mutableStateOf<LoadingState>(LoadingState.None)
        private set

    val selectedPacks = mutableStateListOf<ResourcePackInfo>()

    var deleteAllOperation by mutableStateOf<DeleteAllOperation>(DeleteAllOperation.None)

    private var packCount = FolderFileCounter(resourcePackDir)

    fun selectAllFiles() {
        filteredPacks?.forEach { pack ->
            if (!selectedPacks.contains(pack)) selectedPacks.add(pack)
        }
    }

    fun clearSelected() {
        filteredPacks?.let {
            selectedPacks.removeAll(it)
        }
    }

    private var job: Job? = null

    fun refresh(
        checkCount: Boolean = true
    ) {
        job = viewModelScope.launch {
            packState = LoadingState.Loading
            selectedPacks.clear()
            if (checkCount) packCount.checkDir()

            withContext(Dispatchers.IO) {
                val tempList = mutableListOf<ResourcePackInfo>()
                try {
                    resourcePackDir.listFiles()?.forEach { file ->
                        parseResourcePack(file)?.let {
                            ensureActive()
                            tempList.add(it)
                        }
                    }
                } catch (_: CancellationException) {
                    return@withContext
                }
                allPacks = tempList.sortedBy { it.rawName }
                filterPacks()
            }

            packState = LoadingState.None
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

    fun updateFilter(filter: ResourcePackFilter) {
        this.packFilter = filter
        filterPacks()
    }

    fun updateSortBy(sortByEnum: SortByEnum) {
        this.sortByEnum = sortByEnum
        filterPacks()
    }

    fun updateSortOrder() {
        this.isAscending = !this.isAscending
        filterPacks()
    }

    val supportedSortByEnums = listOf(
        SortByEnum.Name, SortByEnum.FileModifiedTime
    )

    private fun filterPacks() {
        filteredPacks = allPacks
            .takeIf { it.isNotEmpty() }
            ?.filterPacks(packFilter)
            ?.sortedWith { o1, o2 ->
                val value = when (sortByEnum) {
                    SortByEnum.Name -> o1.displayName.compareTo(o2.displayName)
                    SortByEnum.FileModifiedTime -> o2.file.lastModified().compareTo(o1.file.lastModified())
                    else -> error("This sorting method is not supported: $sortByEnum")
                }
                if (isAscending) {
                    value
                } else {
                    -value
                }
            }
    }

    override fun onCleared() {
        job?.cancel()
        job = null
    }
}

@Composable
private fun rememberResourcePackManageViewModel(
    resourcePackDir: File,
    version: Version
) = viewModel(
    key = version.toString() + "_" + VersionFolders.RESOURCE_PACK.folderName
) {
    ResourcePackManageViewModel(resourcePackDir)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ResourcePackManageScreen(
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

    val resourcePackDir = remember(version) {
        VersionFolders.RESOURCE_PACK.getDir(version.getGameDir())
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.ResourcePackManager, versionsScreenKey, false)
    ) { isVisible ->
        val viewModel = rememberResourcePackManageViewModel(resourcePackDir, version)

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
                enabled = viewModel.packState !is LoadingState.Loading,
                onRefresh = viewModel::refresh,
            ) {
                when (viewModel.packState) {
                    is LoadingState.None -> {
                        var resourcePackOperation by remember { mutableStateOf<ResourcePackOperation>(ResourcePackOperation.None) }
                        fun runProgress(task: () -> Unit) {
                            operationScope.launch(Dispatchers.IO) {
                                resourcePackOperation = ResourcePackOperation.Progress
                                task()
                                resourcePackOperation = ResourcePackOperation.None
                                viewModel.refresh()
                            }
                        }
                        ResourcePackOperation(
                            resourcePackDir = resourcePackDir,
                            resourcePackOperation = resourcePackOperation,
                            updateOperation = { resourcePackOperation = it },
                            onRename = { newName, packInfo ->
                                runProgress {
                                    val extension = if (packInfo.file.isFile) {
                                        ".${packInfo.file.extension}"
                                    } else ""
                                    packInfo.file.renameTo(File(resourcePackDir, "$newName$extension"))
                                }
                            },
                            onDelete = { packInfo ->
                                runProgress {
                                    FileUtils.deleteQuietly(packInfo.file)
                                }
                            }
                        )

                        Column {
                            val filter = viewModel.packFilter
                            PackToolbar(
                                modifier = Modifier.fillMaxWidth(),
                                query = filter.filterName,
                                onQuery = { viewModel.updateFilter(filter.copy(filterName = it)) },
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
                                onImport = rememberPackImport("ContentManager.ResourcePacks.Import", resourcePackDir, submitError) { viewModel.refresh() },
                                onRefresh = { viewModel.refresh() },
                                filterMenuItem = { dismiss ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.manage_only_show_valid)) },
                                        trailingIcon = { Checkbox(checked = filter.onlyShowValid, onCheckedChange = null) },
                                        onClick = {
                                            viewModel.updateFilter(filter.copy(onlyShowValid = !filter.onlyShowValid))
                                            dismiss()
                                        }
                                    )
                                }
                            )

                            ResourcePackList(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                packList = viewModel.filteredPacks,
                                selectedPacks = viewModel.selectedPacks,
                                removeFromSelected = { viewModel.selectedPacks.remove(it) },
                                addToSelected = { viewModel.selectedPacks.add(it) },
                                updateOperation = { resourcePackOperation = it }
                            )
                        }
                    }
                    is LoadingState.Loading -> {
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
private fun ResourcePackList(
    modifier: Modifier = Modifier,
    packList: List<ResourcePackInfo>?,
    selectedPacks: List<ResourcePackInfo>,
    removeFromSelected: (ResourcePackInfo) -> Unit,
    addToSelected: (ResourcePackInfo) -> Unit,
    updateOperation: (ResourcePackOperation) -> Unit
) {
    packList?.let { list ->
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
                items(list, key = { it.file.absolutePath }) { pack ->
                    ResourcePackItemLayout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        resourcePackInfo = pack,
                        selected = selectedPacks.contains(pack),
                        onClick = {
                            if (selectedPacks.contains(pack)) {
                                removeFromSelected(pack)
                            } else {
                                addToSelected(pack)
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
                text = stringResource(R.string.resource_pack_manage_no_packs)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourcePackItemLayout(
    modifier: Modifier = Modifier,
    resourcePackInfo: ResourcePackInfo,
    selected: Boolean,
    onClick: () -> Unit = {},
    itemColor: Color = itemColor(),
    itemContentColor: Color = onItemColor(),
    borderColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = MaterialTheme.shapes.large,
    updateOperation: (ResourcePackOperation) -> Unit
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
            ByteArrayIcon(
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape = RoundedCornerShape(10.dp)),
                triggerRefresh = resourcePackInfo,
                icon = resourcePackInfo.icon
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                MinecraftColorTextNormal(
                    inputText = resourcePackInfo.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2
                )
                resourcePackInfo.description?.let { description ->
                    MinecraftColorTextNormal(
                        inputText = description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (resourcePackInfo.isValid) {

                    TooltipIconButton(
                        modifier = Modifier.size(38.dp),
                        tooltip = {
                            RichTooltip(
                                modifier = Modifier.padding(all = 3.dp),
                                title = { Text(text = stringResource(R.string.resource_pack_manage_info)) },
                                shadowElevation = 3.dp
                            ) {
                                ResourcePackInfoTooltip(resourcePackInfo)
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info_outlined),
                            contentDescription = stringResource(R.string.saves_manage_info)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.resource_pack_manage_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ResourcePackOperationMenu(
                    resourcePackInfo = resourcePackInfo,
                    buttonSize = 38.dp,
                    iconSize = 26.dp,
                    onRenameClick = {
                        updateOperation(ResourcePackOperation.RenamePack(resourcePackInfo))
                    },
                    onDeleteClick = {
                        updateOperation(ResourcePackOperation.DeletePack(resourcePackInfo))
                    }
                )
            }
        }
    }
}

@Composable
private fun ResourcePackOperationMenu(
    resourcePackInfo: ResourcePackInfo,
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
                enabled = resourcePackInfo.isValid,
                text = {
                    Text(text = stringResource(R.string.generic_rename))
                },
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
                text = {
                    Text(text = stringResource(R.string.generic_delete))
                },
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
private fun ResourcePackOperation(
    resourcePackDir: File,
    resourcePackOperation: ResourcePackOperation,
    updateOperation: (ResourcePackOperation) -> Unit,
    onRename: (String, ResourcePackInfo) -> Unit,
    onDelete: (ResourcePackInfo) -> Unit
) {
    when (resourcePackOperation) {
        is ResourcePackOperation.None -> {}
        is ResourcePackOperation.Progress -> {
            ProgressDialog()
        }
        is ResourcePackOperation.RenamePack -> {
            val packInfo = resourcePackOperation.packInfo
            FileNameInputDialog(
                initValue = packInfo.displayName,
                existsCheck = { value ->
                    val fileName = if (packInfo.file.isDirectory) {
                        value
                    } else {
                        "$value.${packInfo.file.extension}"
                    }
                    File(resourcePackDir, fileName).exists()
                },
                existsMessage = stringResource(R.string.resource_pack_manage_exists),
                title = stringResource(R.string.generic_rename),
                label = stringResource(R.string.resource_pack_manage_name),
                onDismissRequest = {
                    updateOperation(ResourcePackOperation.None)
                },
                onConfirm = { value ->
                    onRename(value, packInfo)
                }
            )
        }
        is ResourcePackOperation.DeletePack -> {
            val packInfo = resourcePackOperation.packInfo
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.resource_pack_manage_delete_warning, packInfo.file.name),
                onDismiss = {
                    updateOperation(ResourcePackOperation.None)
                },
                onConfirm = {
                    onDelete(packInfo)
                    updateOperation(ResourcePackOperation.None)
                }
            )
        }
    }
}

@Composable
private fun ResourcePackInfoTooltip(
    resourcePackInfo: ResourcePackInfo
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

        Text(
            text = stringResource(
                R.string.resource_pack_manage_type,
                if (resourcePackInfo.file.isDirectory) {
                    stringResource(R.string.resource_pack_manage_type_folder)
                } else {
                    stringResource(R.string.resource_pack_manage_type_zip)
                }
            )
        )

        Text(text = stringResource(R.string.generic_file_name, resourcePackInfo.file.name))

        resourcePackInfo.fileSize?.let { fileSize ->
            Text(text = stringResource(R.string.generic_file_size, formatFileSize(fileSize)))
        }

        resourcePackInfo.packFormat?.let { packFormat ->
            Text(text = stringResource(R.string.resource_pack_manage_formats, packFormat.toString()))
        }
    }
}
