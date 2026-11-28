package com.cmc.launcher.ui.screens.content.versions

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.cmc.launcher.ui.components.StaticBusyIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.cmc.launcher.BuildKeys
import com.cmc.launcher.R
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionFolders
import com.cmc.launcher.ui.androidText
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.components.CardTitleLayout
import com.cmc.launcher.ui.components.EdgeDirection
import com.cmc.launcher.ui.components.ScalingLabel
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.components.SimpleTextInputField
import com.cmc.launcher.ui.components.animated
import com.cmc.launcher.ui.components.fadeEdge
import com.cmc.launcher.ui.components.rememberAnimatedContext
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.elements.SortByDropdownMenu
import com.cmc.launcher.ui.screens.content.elements.SortByEnum
import com.cmc.launcher.ui.screens.content.versions.elements.DeleteAllOperation
import com.cmc.launcher.ui.screens.content.versions.elements.LoadingState
import com.cmc.launcher.ui.screens.content.versions.elements.MinecraftColorTextNormal
import androidx.compose.material3.DropdownMenuItem
import com.cmc.launcher.ui.screens.content.versions.layouts.LibrarySelection
import com.cmc.launcher.ui.screens.content.versions.layouts.PackToolbar
import com.cmc.launcher.ui.screens.content.versions.layouts.VersionChunkBackground
import com.cmc.launcher.ui.theme.itemColor
import com.cmc.launcher.ui.theme.onItemColor
import com.cmc.launcher.utils.animation.getAnimateTween
import com.cmc.launcher.utils.logging.Logger
import com.cmc.launcher.utils.string.getMessageOrToString
import com.cmc.launcher.viewmodel.ErrorViewModel
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.sendToast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

private const val TAG = "ScreenshotsManager"

data class ScreenshotInfo(
    val file: File,
    val name: String = file.nameWithoutExtension,
    val lastModified: Long = file.lastModified(),
    val size: Long = file.length()
)

data class ScreenshotFilter(val filterName: String)

sealed interface ExportOperation {
    data object None : ExportOperation
    data object Ask : ExportOperation
}

@HiltViewModel
class ScreenshotsManageViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private var screenshotDir: File? = null

    var filter by mutableStateOf(ScreenshotFilter(""))
        private set

    var allScreenshots by mutableStateOf<List<ScreenshotInfo>>(emptyList())
        private set
    var filteredScreenshots by mutableStateOf<List<ScreenshotInfo>?>(null)
        private set
    var sortByEnum by mutableStateOf(SortByEnum.Name)
        private set
    var isAscending by mutableStateOf(false)
        private set
    var listState by mutableStateOf<LoadingState>(LoadingState.None)
        private set

    val selectedShots = mutableStateListOf<ScreenshotInfo>()

    var deleteAllOperation by mutableStateOf<DeleteAllOperation>(DeleteAllOperation.None)
    var exportOperation by mutableStateOf<ExportOperation>(ExportOperation.None)

    fun initDirectory(dir: File) {
        if (screenshotDir != dir) {
            screenshotDir = dir
            refresh()
        }
    }

    fun selectAllFiles() {
        filteredScreenshots?.forEach { shot ->
            if (!selectedShots.contains(shot)) selectedShots.add(shot)
        }
    }

    fun clearSelected() {
        filteredScreenshots?.let {
            selectedShots.removeAll(it)
        }
    }

    fun requestDeleteSelected() {
        if (deleteAllOperation == DeleteAllOperation.None && selectedShots.isNotEmpty()) {
            deleteAllOperation = DeleteAllOperation.Warning(selectedShots.map { it.file })
        }
    }

    private var refreshJob: Job? = null

    fun refresh() {
        val dir = screenshotDir ?: return

        refreshJob = viewModelScope.launch {
            listState = LoadingState.Loading
            selectedShots.clear()

            withContext(Dispatchers.IO) {
                val tempList = mutableListOf<ScreenshotInfo>()
                try {
                    if (dir.exists() && dir.isDirectory) {
                        dir.listFiles { file ->
                            file.isFile && file.extension.lowercase() == "png"
                        }?.forEach { file ->
                            ensureActive()
                            tempList.add(ScreenshotInfo(file))
                        }
                    }
                } catch (_: CancellationException) {
                    return@withContext
                }
                allScreenshots = tempList
                applyFilterAndSort()
            }

            listState = LoadingState.None
            refreshJob = null
        }
    }

    private var exportsJob: Job? = null

    fun exports(
        onSuccess: suspend () -> Unit,
        onFailed: suspend (e: Exception) -> Unit
    ) {
        val infos = selectedShots.takeIf { it.isNotEmpty() } ?: allScreenshots
        exportsJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                exportOperation = ExportOperation.None
            }

            try {
                val resolver = context.contentResolver
                for (info in infos) {
                    exportSingleImage(resolver, info.file)
                }
                onSuccess()
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to export screenshots!", e)
                onFailed(e)
            }
        }
    }

    private fun exportSingleImage(
        resolver: ContentResolver,
        file: File
    ) {
        val fileName = file.name
        val relativePath = Environment.DIRECTORY_PICTURES + "/" + BuildKeys.LAUNCHER_IDENTIFIER + "/"

        val existingUri = queryExistingUri(resolver, fileName, relativePath)
        val isNewFile = (existingUri == null)

        val uri = if (isNewFile) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Failed to insert new MediaStore entry for $fileName")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pendingValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                resolver.update(existingUri, pendingValues, null, null)
            }
            existingUri
        }

        try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: throw IOException("Failed to open output stream for $fileName")
        } catch (e: Exception) {

            if (isNewFile && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.delete(uri, null, null)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val clearPending = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, clearPending, null, null)
            }
            throw IOException("Failed to write image $fileName", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val finalValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, finalValues, null, null)
        }
    }

    private fun queryExistingUri(
        resolver: ContentResolver,
        fileName: String,
        relativePath: String
    ): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(fileName, relativePath)

        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        return null
    }

    init {
        refresh()
    }

    fun updateFilter(newFilter: ScreenshotFilter) {
        this.filter = newFilter
        applyFilterAndSort()
    }

    fun updateSortBy(sortByEnum: SortByEnum) {
        this.sortByEnum = sortByEnum
        applyFilterAndSort()
    }

    fun updateSortOrder() {
        this.isAscending = !this.isAscending
        applyFilterAndSort()
    }

    val supportedSortByEnums = listOf(
        SortByEnum.Name, SortByEnum.FileModifiedTime
    )

    private var filterJob: Job? = null

    private fun applyFilterAndSort() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            val result = allScreenshots
                .takeIf { it.isNotEmpty() }
                ?.filter { info ->
                    filter.filterName.isBlank() || info.name.contains(filter.filterName, ignoreCase = true)
                }
                ?.sortedWith { o1, o2 ->
                    val value = when (sortByEnum) {
                        SortByEnum.Name -> o1.name.compareTo(o2.name)
                        SortByEnum.FileModifiedTime -> o1.lastModified.compareTo(o2.lastModified)
                        else -> error("This sorting method is not supported: $sortByEnum")
                    }
                    if (isAscending) value else -value
                }
            if (!isActive) return@launch
            filteredScreenshots = result
        }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        refreshJob = null
        exportsJob?.cancel()
        exportsJob = null
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScreenshotsManagerScreen(
    mainScreenKey: TitledNavKey?,
    versionsScreenKey: TitledNavKey?,
    version: Version,
    backToMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    if (!version.isValid()) {
        backToMainScreen()
        return
    }

    val screenshotDir = remember(version) {
        VersionFolders.SCREENSHOTS.getDir(version.getGameDir())
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.ScreenshotsManager, versionsScreenKey, false)
    ) { isVisible ->
        val viewModel: ScreenshotsManageViewModel = hiltViewModel(
            key = version.toString() + "_" + VersionFolders.SCREENSHOTS.folderName
        )

        LaunchedEffect(screenshotDir) {

            viewModel.initDirectory(screenshotDir)
        }

        DeleteAllOperation(
            operation = viewModel.deleteAllOperation,
            changeOperation = { viewModel.deleteAllOperation = it },
            submitError = submitError,
            onRefresh = { viewModel.refresh() }
        )

        ExportDialogHandler(
            operation = viewModel.exportOperation,
            updateOperation = { viewModel.exportOperation = it },
            selectedShots = viewModel.selectedShots,
            onExport = {
                viewModel.exports(
                    onSuccess = {
                        withContext(Dispatchers.Main) {
                            eventViewModel.sendToast(androidText(R.string.generic_saved))
                        }
                    },
                    onFailed = { e ->
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(R.string.screenshots_manage_export_failed),
                                message = androidText(e.getMessageOrToString())
                            )
                        )
                    }
                )
            }
        )

        val ctx = rememberAnimatedContext(isVisible = isVisible)

        VersionChunkBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
                .animated(ctx),
            paddingValues = PaddingValues()
        ) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = false,
                enabled = viewModel.listState !is LoadingState.Loading,
                onRefresh = viewModel::refresh,
            ) {
                when (viewModel.listState) {
                    is LoadingState.None -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                PackToolbar(
                                    modifier = Modifier.fillMaxWidth(),
                                    query = viewModel.filter.filterName,
                                    onQuery = { viewModel.updateFilter(ScreenshotFilter(it)) },
                                    sortOptions = viewModel.supportedSortByEnums,
                                    sortBy = viewModel.sortByEnum,
                                    onSortBy = viewModel::updateSortBy,
                                    ascending = viewModel.isAscending,
                                    onReverse = viewModel::updateSortOrder,
                                    selection = LibrarySelection(
                                        viewModel.selectedShots.size,
                                        viewModel::selectAllFiles,
                                        viewModel::clearSelected,
                                        viewModel::requestDeleteSelected
                                    ),
                                    onRefresh = viewModel::refresh,
                                    filterMenuItem = { dismiss ->
                                        DropdownMenuItem(
                                            enabled = viewModel.allScreenshots.isNotEmpty(),
                                            text = { Text(stringResource(R.string.screenshots_manage_export)) },
                                            leadingIcon = { Icon(painterResource(R.drawable.ic_file_export_outlined), null) },
                                            onClick = { dismiss(); viewModel.exportOperation = ExportOperation.Ask }
                                        )
                                    }
                                )

                                ScreenshotGrid(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    list = viewModel.filteredScreenshots,
                                    selected = viewModel.selectedShots,
                                    removeFromSelected = { viewModel.selectedShots.remove(it) },
                                    addToSelected = { viewModel.selectedShots.add(it) }
                                )
                            }

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
private fun ExportDialogHandler(
    operation: ExportOperation,
    updateOperation: (ExportOperation) -> Unit,
    selectedShots: List<ScreenshotInfo>,
    onExport: () -> Unit
) {
    when (operation) {
        is ExportOperation.None -> {}
        is ExportOperation.Ask -> {
            val isAll = selectedShots.isEmpty()
            SimpleAlertDialog(
                title = stringResource(R.string.screenshots_manage_export_title),
                text = {
                    Text(
                        text = if (isAll) {
                            stringResource(R.string.screenshots_manage_export_all_message)
                        } else {
                            stringResource(
                                R.string.screenshots_manage_export_selected_message,
                                selectedShots.size
                            )
                        }
                    )
                },
                confirmText = stringResource(R.string.screenshots_manage_export),
                dismissText = stringResource(R.string.generic_cancel),
                onConfirm = onExport,
                onCancel = { updateOperation(ExportOperation.None) },
                onDismissRequest = { updateOperation(ExportOperation.None) }
            )
        }
    }
}

@Composable
private fun ScreenshotGrid(
    modifier: Modifier = Modifier,
    list: List<ScreenshotInfo>?,
    selected: List<ScreenshotInfo>,
    removeFromSelected: (ScreenshotInfo) -> Unit,
    addToSelected: (ScreenshotInfo) -> Unit
) {
    list?.let { items ->
        if (items.isNotEmpty()) {
            val context = LocalContext.current
            val isSelectionMode = selected.isNotEmpty()

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = modifier,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.file.absolutePath }) { info ->
                    ScreenshotItemLayout(
                        info = info,
                        selected = selected.contains(info),
                        isSelectionMode = isSelectionMode,
                        onToggleSelect = {
                            if (selected.contains(info)) {
                                removeFromSelected(info)
                            } else {
                                addToSelected(info)
                            }
                        },
                        onOpen = {
                            try {

                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    info.file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "image/png")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Logger.error(TAG, "Failed to open image", e)
                            }
                        }
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
                text = stringResource(R.string.screenshots_manage_no_screenshots)
            )
        }
    }
}

@Composable
private fun ScreenshotItemLayout(
    modifier: Modifier = Modifier,
    info: ScreenshotInfo,
    selected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
    itemColor: Color = itemColor(),
    itemContentColor: Color = onItemColor(),
    borderColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = MaterialTheme.shapes.large,
) {
    val isChecked = selected
    Surface(
        modifier = modifier,
        shape = shape,
        border = if (selected) BorderStroke(2.dp, borderColor) else null,
        color = itemColor,
        contentColor = itemContentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (isSelectionMode) this.selected = isChecked }
                .combinedClickable(
                    role = Role.Button,
                    onClick = if (isSelectionMode) onToggleSelect else onOpen,
                    onLongClick = { if (!isSelectionMode) onToggleSelect() }
                )
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                AsyncImage(
                    model = info.file,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (selected) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.padding(4.dp).size(16.dp))
                    }
                }
            }
            Text(
                text = info.name,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
