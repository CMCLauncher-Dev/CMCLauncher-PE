package com.cmc.launcher.ui.screens.content.download.addon.download

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.favorites.FavoriteProjectsRepository
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.game.download.addon.platform.PlatformProject
import com.cmc.launcher.game.download.addon.platform.PlatformVersion
import com.cmc.launcher.game.download.addon.platform.cacheKey
import com.cmc.launcher.game.download.addon.platform.getProject
import com.cmc.launcher.game.download.addon.platform.getVersionById
import com.cmc.launcher.game.download.addon.platform.getVersions
import com.cmc.launcher.game.download.addon.platform.isAllNull
import com.cmc.launcher.game.download.addon.utils.getMcmodTitle
import com.cmc.launcher.game.download.addon.utils.getTranslations
import com.cmc.launcher.game.version.mod.InstalledMod
import com.cmc.launcher.game.versioninfo.filterRelease
import com.cmc.launcher.ui.AndroidStringText
import com.cmc.launcher.ui.androidText
import com.cmc.launcher.ui.components.CheckChip
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.download.addon.elements.AddonIcon
import com.cmc.launcher.ui.screens.content.download.addon.elements.AddonVersionItemLayout
import com.cmc.launcher.ui.screens.content.download.addon.elements.ClassesIdentifier
import com.cmc.launcher.ui.screens.content.download.addon.elements.DependencyEntry
import com.cmc.launcher.ui.screens.content.download.addon.elements.DownloadAddonState
import com.cmc.launcher.ui.screens.content.download.addon.elements.DownloadAddonVersionLoading
import com.cmc.launcher.ui.screens.content.download.addon.elements.ProjectUrlsContent
import com.cmc.launcher.ui.screens.content.download.addon.elements.ScreenshotItemLayout
import com.cmc.launcher.ui.screens.content.download.addon.elements.VersionInfoMap
import com.cmc.launcher.ui.screens.content.download.addon.elements.initAll
import com.cmc.launcher.ui.screens.content.download.addon.elements.mapWithVersions
import com.cmc.launcher.ui.screens.main.control_editor.EditorChoices
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.viewmodel.EventViewModel
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jackhuang.hmcl.addon.AddonProjectInfo
private class DownloadScreenViewModel(
    private val platform: Platform,
    private val projectId: String,
    initialClasses: PlatformClasses
): ViewModel() {

    var classes by mutableStateOf(initialClasses)
        private set

    private var _versionsList by mutableStateOf<List<VersionInfoMap>>(emptyList())

    private var rawVersions: List<PlatformVersion> = emptyList()
    var versionsResult by mutableStateOf<DownloadAddonState<List<VersionInfoMap>>>(DownloadAddonState.Getting())
    private var loadingVersions = false
    var versionsLoading by mutableStateOf<DownloadAddonVersionLoading>(DownloadAddonVersionLoading.None)
        private set

    var showOnlyMCRelease by mutableStateOf(true)
    var searchMCVersion by mutableStateOf("")

    fun filterWith(
        showOnlyMCRelease: Boolean = this.showOnlyMCRelease,
        searchMCVersion: String = this.searchMCVersion
    ) {
        this.showOnlyMCRelease = showOnlyMCRelease
        this.searchMCVersion = searchMCVersion
        if (!loadingVersions && versionsResult is DownloadAddonState.Success) {
            versionsResult = DownloadAddonState.Success(_versionsList.filterInfos())
        }
    }

    private fun List<VersionInfoMap>.filterInfos(): List<VersionInfoMap> {
        return filter { info ->
            (!showOnlyMCRelease || filterRelease(info.gameVersion)) &&
                    (searchMCVersion.isEmpty() || info.gameVersion.contains(searchMCVersion, true))
        }
    }

    private fun remapVersions() {
        _versionsList = rawVersions.mapWithVersions(classes)
        if (!loadingVersions && versionsResult is DownloadAddonState.Success) {
            versionsResult = DownloadAddonState.Success(_versionsList.filterInfos())
        }
    }

    fun getVersions() {
        if (loadingVersions) return
        loadingVersions = true
        versionsResult = DownloadAddonState.Getting()
        viewModelScope.launch {
            try {

            failedDependencyProjects.clear()
            if (platform == Platform.CURSEFORGE) {
                versionsLoading = DownloadAddonVersionLoading.StartLoadPage
            }

            getVersions(
                projectID = projectId,
                platform = platform,
                pageCallback = { chunk, page ->
                    versionsLoading = DownloadAddonVersionLoading.LoadingPage(chunk, page)
                },
                onSuccess = { result ->
                    val versions: List<PlatformVersion> = result.initAll(projectId)
                    rawVersions = versions

                    _versionsList = versions.mapWithVersions(classes)
                    versionsResult = DownloadAddonState.Success(_versionsList.filterInfos())
                    versionsLoading = DownloadAddonVersionLoading.None

                    if (classes == PlatformClasses.MOD_PACK) return@getVersions
                    val dependencies = versions
                        .flatMap { it.platformDependencies() }
                        .distinctBy { it.cacheKey() }
                    if (dependencies.isEmpty()) return@getVersions

                    viewModelScope.launch {
                        val semaphore = Semaphore(8)
                        dependencies.map { dependency ->
                            async {
                                semaphore.withPermit {
                                    cacheDependencyProject(
                                        platform = dependency.platform,
                                        dependency = dependency
                                    )
                                }
                            }
                        }.awaitAll()
                    }
                },
                onError = {
                    versionsResult = it
                    versionsLoading = DownloadAddonVersionLoading.None
                }
            )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                versionsResult = DownloadAddonState.Error(com.cmc.launcher.game.download.addon.mapExceptionToMessage(error))
            } finally {
                loadingVersions = false
                versionsLoading = DownloadAddonVersionLoading.None
            }
        }
    }

    var projectResult by mutableStateOf<DownloadAddonState<Pair<PlatformProject, AddonProjectInfo?>>>(DownloadAddonState.Getting())
    private var loadingProject = false

    fun getProject() {
        if (loadingProject) return
        loadingProject = true
        projectResult = DownloadAddonState.Getting()
        viewModelScope.launch {
            try {
                getProject(
                    projectID = projectId,
                    platform = platform,
                    onSuccess = { result ->
                        val accurateClasses = result.platformClasses(classes)
                        if (accurateClasses != classes) {
                            classes = accurateClasses
                            remapVersions()
                        }
                        val mcmod = classes.getTranslations()?.getInfoByCurseForgeId(result.platformSlug())
                        projectResult = DownloadAddonState.Success(Pair(result, mcmod))
                    },
                    onError = { state, _ -> projectResult = state }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                projectResult = DownloadAddonState.Error(com.cmc.launcher.game.download.addon.mapExceptionToMessage(error))
            } finally {
                loadingProject = false
            }
        }
    }

    val cachedDependencyProject = mutableStateMapOf<String, PlatformProject>()

    val notFoundDependencyProjects = mutableStateListOf<String>()

    val failedDependencyProjects = mutableStateListOf<String>()

    private suspend fun cacheDependencyProject(
        platform: Platform,
        dependency: PlatformVersion.PlatformDependency
    ) {
        val key = dependency.cacheKey()
        if (notFoundDependencyProjects.contains(key) || cachedDependencyProject.containsKey(key)) return

        try {
            val projectId = dependency.projectId ?: run {

                getVersionById(
                    versionId = dependency.versionId
                        ?: error("The dependency does not provide a project id or a version id."),
                    platform = platform,
                    printLog = false
                ).platformProjectId()
            }
            getProject<PlatformProject>(
                projectID = projectId,
                platform = platform,
                onSuccess = { result ->
                    cachedDependencyProject[key] = result
                },
                onError = { _, e ->
                    if (e.isNotFound()) {
                        notFoundDependencyProjects.add(key)
                    } else {
                        if (!failedDependencyProjects.contains(key)) failedDependencyProjects.add(key)
                    }
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (e.isNotFound()) {
                notFoundDependencyProjects.add(key)
            } else {
                if (!failedDependencyProjects.contains(key)) failedDependencyProjects.add(key)
            }
        }
    }

    init {

        getVersions()
        getProject()
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

private fun Throwable.isNotFound(): Boolean =
    this is ClientRequestException && response.status.value == 404

@Composable
private fun rememberDownloadAddonViewModel(
    key: NormalNavKey.DownloadAddon
): DownloadScreenViewModel {
    return viewModel(
        key = key.toString()
    ) {
        DownloadScreenViewModel(
            platform = key.platform,
            projectId = key.projectId,
            initialClasses = key.classes
        )
    }
}

@Composable
fun DownloadAddonScreen(
    mainScreenKey: TitledNavKey?,
    parentScreenKey: TitledNavKey,
    parentCurrentKey: TitledNavKey?,
    currentKey: TitledNavKey?,
    key: NormalNavKey.DownloadAddon,
    eventViewModel: EventViewModel,
    onItemClicked: (PlatformClasses, PlatformVersion, iconUrl: String?, deps: List<DependencyEntry>) -> Unit,
    nestedNavKeyClass: Class<out TitledNavKey>? = null,
    versionsUIWeight: Float = 6.5f,
    projectUIWeight: Float = 3.5f,
    installedChecker: ((PlatformVersion) -> InstalledMod?)? = null
) {
    val model = rememberDownloadAddonViewModel(key)
    if (!(nestedNavKeyClass ?: NestedNavKey.Download::class.java).isInstance(mainScreenKey) ||
        parentScreenKey != parentCurrentKey || key != currentKey) return
    var overview by rememberSaveable(key.platform.name, key.projectId) { mutableStateOf(false) }
    val context = LocalContext.current
    val project = (model.projectResult as? DownloadAddonState.Success)?.result?.first
    val mcmod = (model.projectResult as? DownloadAddonState.Success)?.result?.second
    val title = project?.let { mcmod.getMcmodTitle(it.platformTitle(), context) } ?: key.projectId
    val icon = project?.platformIconUrl() ?: key.iconUrl
    BackHandler(enabled = overview) { overview = false }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            AddonIcon(modifier = Modifier.clip(FieldShape), size = Size.touch, iconUrl = icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(key.platform.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val favorite = FavoriteProjectsRepository.isFavorite(key.platform, key.projectId)
            IconButton(enabled = project != null || favorite, onClick = {
                if (favorite) FavoriteProjectsRepository.unfavorite(key.platform, key.projectId)
                else project?.let { FavoriteProjectsRepository.favorite(it, model.classes) }
            }) {
                Icon(painterResource(if (favorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outlined),
                    stringResource(if (favorite) R.string.favorite_added else R.string.favorite_add),
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.md), verticalAlignment = Alignment.CenterVertically) {
            EditorChoices(listOf(stringResource(R.string.pe_addon_versions), stringResource(R.string.pe_addon_overview)),
                if (overview) 1 else 0, { overview = it == 1 }, Modifier.weight(1f))
            IconButton(onClick = { if (overview) model.getProject() else model.getVersions() },
                enabled = if (overview) model.projectResult !is DownloadAddonState.Getting else model.versionsResult !is DownloadAddonState.Getting) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.generic_refresh))
            }
        }
        HorizontalDivider(Modifier.padding(top = Space.sm))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (overview) ProjectDetails(model.projectResult, model.classes, model::getProject) {
                eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it))
            } else ProjectVersions(model, installedChecker) { version ->
                val deps = version.platformDependencies().mapNotNull { dependency ->
                    val id = dependency.cacheKey()
                    val cached = model.cachedDependencyProject[id]
                    when {
                        cached != null -> DependencyEntry(dependency, cached)
                        id in model.notFoundDependencyProjects -> DependencyEntry(dependency, null, notFound = true)
                        id in model.failedDependencyProjects -> DependencyEntry(dependency, null)
                        else -> null
                    }
                }
                onItemClicked(model.classes, version, icon, deps)
            }
        }
    }
}

@Composable
private fun ProjectVersions(model: DownloadScreenViewModel, installed: ((PlatformVersion) -> InstalledMod?)?, pick: (PlatformVersion) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            OwnOutlinedTextField(value = model.searchMCVersion, onValueChange = { model.filterWith(searchMCVersion = it) },
                modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.download_addon_search_mc_versions)) }, singleLine = true)
            CheckChip(selected = model.showOnlyMCRelease, onClick = { model.filterWith(showOnlyMCRelease = !model.showOnlyMCRelease) },
                label = { Text(stringResource(R.string.download_addon_show_only_mc_release)) })
        }
        when (val state = model.versionsResult) {
            is DownloadAddonState.Getting -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(stringResource(R.string.generic_loading))
                    when (val progress = model.versionsLoading) {
                        DownloadAddonVersionLoading.None -> Unit
                        DownloadAddonVersionLoading.StartLoadPage -> Text(stringResource(R.string.download_addon_loading_page_data), style = MaterialTheme.typography.bodySmall)
                        is DownloadAddonVersionLoading.LoadingPage -> Text(stringResource(R.string.download_addon_loaded_chunk_page, progress.chunk, progress.page), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            is DownloadAddonState.Error -> ProjectFailure(Modifier.weight(1f).fillMaxWidth(),
                androidText(R.string.download_addon_failed_to_get_versions, state.message), model::getVersions)
            is DownloadAddonState.Success -> {
                if (state.result.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth().padding(Space.lg), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.pe_search_empty))
                } else androidx.compose.runtime.key(model.searchMCVersion, model.showOnlyMCRelease) {
                    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = state.result.indexOfFirst { it.isAdapt }.coerceAtLeast(0))
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = scroll, contentPadding = PaddingValues(Space.md),
                        verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        items(state.result, key = { "${it.gameVersion}_${it.loader?.getDisplayName()}" }) { group ->
                            AddonVersionItemLayout(modifier = Modifier.fillMaxWidth(), infoMap = group,
                                installedChecker = installed, onItemClicked = pick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectDetails(state: DownloadAddonState<Pair<PlatformProject, AddonProjectInfo?>>, classes: PlatformClasses,
    reload: () -> Unit, open: (String) -> Unit) {
    val context = LocalContext.current
    when (state) {
        is DownloadAddonState.Getting -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.generic_loading)) }
        is DownloadAddonState.Error -> ProjectFailure(Modifier.fillMaxSize(), androidText(R.string.download_addon_failed_to_get_project, state.message), reload)
        is DownloadAddonState.Success -> {
            val (project, mcmod) = state.result
            val urls = project.platformUrls(classes)
            val screenshots = project.platformScreenshots()
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                item {
                    Text(mcmod.getMcmodTitle(project.platformTitle(), context), style = MaterialTheme.typography.titleLarge)
                    ClassesIdentifier(classes)
                }
                item { Text(project.platformSummary(), style = MaterialTheme.typography.bodyLarge) }
                if (!urls.isAllNull()) item {
                    Text(stringResource(R.string.download_addon_links), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        ProjectUrlsContent(platform = project.platform(), urls = urls, mcmod = mcmod, openLink = open)
                    }
                }
                items(screenshots) { ScreenshotItemLayout(modifier = Modifier.fillMaxWidth(), screenshot = it) }
            }
        }
    }
}

@Composable
private fun ProjectFailure(modifier: Modifier, message: AndroidStringText, retry: () -> Unit) {
    Column(modifier.padding(Space.lg), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidStringText(message)
        TextButton(onClick = retry) { Text(stringResource(R.string.generic_retry)) }
    }
}
