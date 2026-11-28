package com.cmc.launcher.ui.screens.content.download.addon.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmc.launcher.game.download.addon.mapExceptionToMessage
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.game.download.addon.platform.PlatformSearchFilter
import com.cmc.launcher.game.download.addon.platform.searchAddons
import com.cmc.launcher.game.download.addon.utils.searchMcMods
import com.cmc.launcher.game.versioninfo.MinecraftVersion
import com.cmc.launcher.game.versioninfo.MinecraftVersions
import com.cmc.launcher.game.versioninfo.popularVersions
import com.cmc.launcher.ui.screens.content.download.addon.elements.AddonPage
import com.cmc.launcher.ui.screens.content.download.addon.elements.SearchAddonState
import com.cmc.launcher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jackhuang.hmcl.addon.AddonProjectInfo

internal class SearchAddonViewModel(initialPlatform: Platform, private val classes: PlatformClasses) : ViewModel() {
    private val _platform = MutableStateFlow(initialPlatform)
    val platform = _platform.asStateFlow()
    private val _filter = MutableStateFlow(PlatformSearchFilter())
    val filter = _filter.asStateFlow()
    private val _result = MutableStateFlow<SearchAddonState>(SearchAddonState.Searching)
    val result = _result.asStateFlow()
    private val _names = MutableStateFlow<List<AddonProjectInfo>>(emptyList())
    val names = _names.asStateFlow()
    private val _versions = MutableStateFlow<List<String>>(popularVersions)
    val versions = _versions.asStateFlow()
    private val pages = mutableMapOf<Int, AddonPage>()
    private var searchJob: Job? = null
    private var nameJob: Job? = null
    private var versionJob: Job? = null
    private var searchGeneration = 0L
    private var nameGeneration = 0L
    private var versionGeneration = 0L
    private var versionQuery = ""
    private var versionPlatform = initialPlatform

    init {
        search()
        viewModelScope.launch {
            try {
                MinecraftVersions.refreshVersions(force = false)
                suggestVersion(versionQuery, versionPlatform)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.warning("SearchAddon", "Unable to refresh version suggestions", error)
            }
        }
    }

    fun apply(platform: Platform, filter: PlatformSearchFilter) {
        _platform.value = platform
        _filter.value = filter.copy(index = 0, limit = filter.limit.coerceIn(1, 100))
        pages.clear()
        search()
    }

    fun reload() { search() }

    fun goToPage(number: Int) {
        val current = (_result.value as? SearchAddonState.Success)?.page ?: return
        if (number < 1 || number == current.pageNumber) return
        if (current.totalPage > 0 && number > current.totalPage) return
        if (current.isLastPage && number > current.pageNumber) return
        val offset = (number.toLong() - 1) * _filter.value.limit
        if (offset > Int.MAX_VALUE) return
        val target = offset.toInt()
        _filter.value = _filter.value.copy(index = target)
        val cached = pages[target]
        if (cached != null) {
            ++searchGeneration
            searchJob?.cancel()
            _result.value = SearchAddonState.Success(cached)
        } else search()
    }

    private fun search() {
        val generation = ++searchGeneration
        searchJob?.cancel()
        val platform = _platform.value
        val filter = _filter.value
        _result.value = SearchAddonState.Searching
        searchJob = viewModelScope.launch {
            try {
                searchAddons(platform, filter, classes, onSuccess = { result ->
                    val page = withContext(Dispatchers.Default) { result.getAddonPage(classes) }
                    currentCoroutineContext().ensureActive()
                    if (generation == searchGeneration) {
                        pages[page.pageIndex] = page
                        _filter.value = filter.copy(index = page.pageIndex)
                        _result.value = SearchAddonState.Success(page)
                    }
                }, onError = { error ->
                    if (generation == searchGeneration) _result.value = error
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == searchGeneration) _result.value = SearchAddonState.Error(mapExceptionToMessage(error))
            }
        }
    }

    fun suggestName(query: String) {
        val generation = ++nameGeneration
        nameJob?.cancel()
        _names.value = emptyList()
        if (query.isBlank()) return
        nameJob = viewModelScope.launch {
            try {
                delay(180)
                val matches = query.searchMcMods(classes).orEmpty().take(20)
                currentCoroutineContext().ensureActive()
                if (generation == nameGeneration) _names.value = matches
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.warning("SearchAddon", "Unable to suggest project names", error)
            }
        }
    }

    fun suggestVersion(query: String, platform: Platform) {
        versionQuery = query
        versionPlatform = platform
        val generation = ++versionGeneration
        versionJob?.cancel()
        _versions.value = emptyList()
        versionJob = viewModelScope.launch {
            val matches = withContext(Dispatchers.Default) {
                val all = MinecraftVersions.allVersions.value
                when {
                    query.isEmpty() -> popularVersions
                    all.isEmpty() -> popularVersions.filter { it.contains(query, ignoreCase = true) }.take(20)
                    else -> all.asSequence().filter {
                        it.version.id.contains(query, ignoreCase = true) &&
                            (platform != Platform.CURSEFORGE || it.type == MinecraftVersion.Type.Release)
                    }.take(20).map { it.version.id }.toList()
                }
            }
            if (generation == versionGeneration) _versions.value = matches
        }
    }

    fun closeSuggestions() {
        ++nameGeneration
        ++versionGeneration
        nameJob?.cancel()
        versionJob?.cancel()
        _names.value = emptyList()
    }

    override fun onCleared() {
        ++searchGeneration
        searchJob?.cancel()
        closeSuggestions()
        super.onCleared()
    }
}