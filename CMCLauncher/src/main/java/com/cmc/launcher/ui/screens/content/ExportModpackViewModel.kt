package com.cmc.launcher.ui.screens.content

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmc.launcher.BuildKeys
import com.cmc.launcher.R
import com.cmc.launcher.game.version.export.ExportInfo
import com.cmc.launcher.game.version.export.PackExporter
import com.cmc.launcher.game.version.export.PackType
import com.cmc.launcher.game.version.export.data.FileSelectionData
import com.cmc.launcher.game.version.export.data.Selected
import com.cmc.launcher.game.version.export.data.getSelectedFiles
import com.cmc.launcher.game.version.installed.Version
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

internal sealed interface PackExportOperation {
    data object None : PackExportOperation
    data object Exporting : PackExportOperation
    data object Finished : PackExportOperation
    data class Error(val throwable: Throwable) : PackExportOperation
}

internal class ExportModpackViewModel(
    mcVersion: String, private val versionName: String, private val gamePath: File,
    loader: ExportInfo.LoaderVersion?, gameArgs: String, javaArgs: String
) : ViewModel() {
    private val _allFiles = MutableStateFlow<List<FileSelectionData>>(emptyList())
    val allFiles = _allFiles.asStateFlow()
    private val _selectedFiles = MutableStateFlow(false)
    val selectedFiles = _selectedFiles.asStateFlow()
    private val _isRefreshingFiles = MutableStateFlow(false)
    val isRefreshingFiles = _isRefreshingFiles.asStateFlow()
    private val _changingSelection = MutableStateFlow(false)
    val changingSelection = _changingSelection.asStateFlow()
    private val _selectingFolder = MutableStateFlow(false)
    val selectingFolder = _selectingFolder.asStateFlow()
    private val _exportInfo = MutableStateFlow(ExportInfo(
        gamePath = gamePath, name = versionName, version = "1.0", mcVersion = mcVersion, loader = loader,
        gameArgs = gameArgs, javaArgs = javaArgs, packType = PackType.Modrinth, packModrinth = true, packCurseForge = true
    ))
    val exportInfo = _exportInfo.asStateFlow()
    private val drafts = mutableMapOf<PackType, ExportInfo>()
    private val _operation = MutableStateFlow<PackExportOperation>(PackExportOperation.None)
    val packExportOperation = _operation.asStateFlow()
    private val _exporter = MutableStateFlow<PackExporter?>(null)
    val packExporter = _exporter.asStateFlow()
    private val selectionMutex = Mutex()
    private var filesLoaded = false
    private var scanJob: Job? = null
    private var selectionJob: Job? = null
    private var preparationJob: Job? = null
    private var exportGeneration = 0L
    private var stopKeepingScreen: (() -> Unit)? = null

    fun editInfo(info: ExportInfo) {
        if (_operation.value == PackExportOperation.Exporting) return
        _exportInfo.value = info
        drafts[info.packType] = info
    }

    fun selectType(type: PackType) {
        if (_operation.value == PackExportOperation.Exporting) return
        val previous = _exportInfo.value
        if (previous.packType == type) return
        drafts[previous.packType] = previous
        _exportInfo.value = drafts[type] ?: previous.copy(
            packType = type, packModrinth = type == PackType.Modrinth,
            packCurseForge = type != PackType.MCBBS
        )
    }

    fun updateSelecting(value: Boolean) { _selectingFolder.value = value }

    fun updateOperation(operation: PackExportOperation) {
        if (_operation.value != PackExportOperation.Exporting) _operation.value = operation
    }

    fun prepareFiles() {
        if (filesLoaded || scanJob?.isActive == true || _operation.value == PackExportOperation.Exporting) return
        _isRefreshingFiles.value = true
        _selectedFiles.value = false
        scanJob = viewModelScope.launch {
            try {
                val (nodes, count) = withContext(Dispatchers.Default) {
                    val nodes = scanFiles()
                    nodes.forEach { node ->
                        currentCoroutineContext().ensureActive()
                        node.updateSelectState(if (excludedNames.any { node.file.name.contains(it) }) Selected.Unselected else Selected.Selected)
                    }
                    nodes to FileSelectionData.refreshTreeSelect(nodes)
                }
                _allFiles.value = nodes
                _selectedFiles.value = count > 0
                filesLoaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _operation.value = PackExportOperation.Error(error)
            } finally {
                _isRefreshingFiles.value = false
            }
        }
    }

    fun setFileSelection(data: FileSelectionData, state: Selected) {
        if (state == Selected.Indeterminate || selectionBlocked()) return
        recomputeSelection { data.updateSelectState(state) }
    }

    fun refreshRootSelect() {
        if (!selectionBlocked()) recomputeSelection {}
    }

    private fun selectionBlocked(): Boolean = _changingSelection.value || _isRefreshingFiles.value ||
        _selectingFolder.value || _operation.value == PackExportOperation.Exporting

    private fun recomputeSelection(change: () -> Unit) {
        _changingSelection.value = true
        _selectedFiles.value = false
        selectionJob = viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.Default) {
                    selectionMutex.withLock {
                        change()
                        FileSelectionData.refreshTreeSelect(_allFiles.value)
                    }
                }
                _selectedFiles.value = count > 0
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _operation.value = PackExportOperation.Error(error)
            } finally {
                _changingSelection.value = false
            }
        }
    }

    fun startExport(version: Version, context: Context, outputUri: Uri,
        onStart: () -> Unit = {}, onStop: () -> Unit = {}, onFinished: () -> Unit = {}) {
        if (selectionBlocked() || !_selectedFiles.value || !version.isValid()) return
        val draft = _exportInfo.value
        if (draft.name.isBlank() || draft.version.isBlank() || (draft.packType.options.requireAuthor && draft.author.isBlank())) return
        val generation = ++exportGeneration
        _operation.value = PackExportOperation.Exporting
        stopKeepingScreen = onStop
        onStart()
        preparationJob = viewModelScope.launch {
            try {
                val files = withContext(Dispatchers.Default) {
                    selectionMutex.withLock { _allFiles.value.getSelectedFiles() }
                }
                currentCoroutineContext().ensureActive()
                if (generation != exportGeneration) return@launch
                check(files.isNotEmpty()) { "No files selected for export" }
                val exporter = PackExporter(context.applicationContext, draft.copy(selectedFiles = files), viewModelScope)
                _exporter.value = exporter
                exporter.startExport(outputUri, version, onFinished = {
                    viewModelScope.launch {
                        if (generation == exportGeneration) {
                            endExport(PackExportOperation.Finished)
                            onFinished()
                        }
                    }
                }, onError = { error ->
                    viewModelScope.launch {
                        if (generation == exportGeneration) endExport(PackExportOperation.Error(error))
                    }
                }, onCancelled = {
                    viewModelScope.launch {
                        if (generation == exportGeneration) endExport(PackExportOperation.None)
                    }
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == exportGeneration) endExport(PackExportOperation.Error(error))
            }
        }
    }

    private fun endExport(operation: PackExportOperation) {
        _exporter.value = null
        _operation.value = operation
        val stop = stopKeepingScreen
        stopKeepingScreen = null
        stop?.invoke()
    }

    fun cancelExport() {
        ++exportGeneration
        preparationJob?.cancel()
        _exporter.value?.cancel()
        endExport(PackExportOperation.None)
    }

    private data class Directory(val file: File, val depth: Int, val target: MutableList<FileSelectionData>)

    private suspend fun scanFiles(): List<FileSelectionData> {
        check(gamePath.isDirectory) { "Game directory is not available" }
        val result = mutableListOf<FileSelectionData>()
        val stack = ArrayDeque<Directory>()
        val visited = mutableSetOf<String>()
        stack.add(Directory(gamePath, 1, result))
        while (stack.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (directory, depth, target) = stack.removeLast()
            if (!visited.add(directory.canonicalPath)) continue
            val children = directory.listFiles() ?: error("Cannot read directory: ${directory.name}")
            for (file in children) {
                currentCoroutineContext().ensureActive()
                if (depth == 1 && (file.name == "$versionName.json" || file.name == "$versionName.jar")) continue
                val nested = if (file.isDirectory) mutableListOf<FileSelectionData>() else null
                target.add(FileSelectionData(file, if (depth == 1) alias(file.name) else null, nested))
                if (nested != null) stack.add(Directory(file, depth + 1, nested))
            }
            target.sort()
        }
        return result
    }

    private fun alias(name: String): Int? = when (name) {
        "options.txt" -> R.string.versions_export_alias_options
        "resourcepacks" -> R.string.versions_export_alias_resource_packs
        "mods" -> R.string.versions_export_alias_mods
        "saves" -> R.string.versions_export_alias_saves
        "shaderpacks" -> R.string.versions_export_alias_shaderpacks
        "config" -> R.string.versions_export_alias_config
        BuildKeys.LAUNCHER_IDENTIFIER -> R.string.versions_export_alias_launcher
        else -> null
    }

    override fun onCleared() {
        scanJob?.cancel()
        selectionJob?.cancel()
        cancelExport()
        super.onCleared()
    }

    private companion object {
        val excludedNames = listOf(".fabric", "options.txt", "natives", "downloads", "PCL", BuildKeys.LAUNCHER_IDENTIFIER,
            "fclversion.cfg", "saves", "realms_persistence.json", "usercache.json", ".log", "logs")
    }
}