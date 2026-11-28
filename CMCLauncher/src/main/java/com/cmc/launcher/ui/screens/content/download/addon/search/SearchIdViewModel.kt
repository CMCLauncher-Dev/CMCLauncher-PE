package com.cmc.launcher.ui.screens.content.download.addon.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmc.launcher.game.download.addon.mapExceptionToMessage
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.game.download.addon.platform.PlatformProject
import com.cmc.launcher.game.download.addon.platform.UnsupportedClassesException
import com.cmc.launcher.game.download.addon.platform.getProjectByVersion
import com.cmc.launcher.game.download.addon.utils.getMcMod
import com.cmc.launcher.ui.AndroidStringText
import io.ktor.client.plugins.ClientRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jackhuang.hmcl.addon.AddonProjectInfo

internal sealed interface SearchIdOperation {
    data object None : SearchIdOperation
    data object Loading : SearchIdOperation
    data class Result(val project: PlatformProject, val classes: PlatformClasses, val mcMod: AddonProjectInfo?) : SearchIdOperation
    data object NotFound : SearchIdOperation
    data object Unsupported : SearchIdOperation
    data class Error(val message: AndroidStringText) : SearchIdOperation
}

internal class SearchIdViewModel : ViewModel() {
    private val _projectId = MutableStateFlow("")
    val projectId = _projectId.asStateFlow()
    private val _platform = MutableStateFlow(Platform.CURSEFORGE)
    val platform = _platform.asStateFlow()
    private val _operation = MutableStateFlow<SearchIdOperation>(SearchIdOperation.None)
    val operation = _operation.asStateFlow()
    private var searchJob: Job? = null
    private var generation = 0L

    fun editId(value: String) {
        if (value == _projectId.value) return
        _projectId.value = value
        invalidate()
    }

    fun selectPlatform(value: Platform) {
        if (value == _platform.value) return
        _platform.value = value
        invalidate()
    }

    private fun invalidate() {
        ++generation
        searchJob?.cancel()
        _operation.value = SearchIdOperation.None
    }

    fun search() {
        val id = _projectId.value.trim()
        if (id.isBlank()) {
            invalidate()
            return
        }
        val ticket = ++generation
        searchJob?.cancel()
        val platform = _platform.value
        _projectId.value = id
        _operation.value = SearchIdOperation.Loading
        searchJob = viewModelScope.launch {
            try {
                val project = getProjectByVersion(projectId = id, platform = platform)
                currentCoroutineContext().ensureActive()
                project.checkClasses()
                val classes = project.platformClasses(PlatformClasses.MOD)
                val state = if (project.platformAvailable()) {
                    SearchIdOperation.Result(project, classes, project.getMcMod(classes))
                } else SearchIdOperation.NotFound
                if (ticket == generation) _operation.value = state
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (ticket == generation) _operation.value = when {
                    error is UnsupportedClassesException -> SearchIdOperation.Unsupported
                    error is NotFoundException || (error is ClientRequestException && error.response.status.value == 404) -> SearchIdOperation.NotFound
                    else -> SearchIdOperation.Error(mapExceptionToMessage(error))
                }
            }
        }
    }

    override fun onCleared() {
        ++generation
        searchJob?.cancel()
        super.onCleared()
    }
}