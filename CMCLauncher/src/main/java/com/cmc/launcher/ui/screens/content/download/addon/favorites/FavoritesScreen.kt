package com.cmc.launcher.ui.screens.content.download.addon.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.favorites.FavoriteEntry
import com.cmc.launcher.game.download.addon.favorites.FavoriteProjectsRepository
import com.cmc.launcher.game.download.addon.mapExceptionToMessage
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.game.download.addon.platform.PlatformDisplayLabel
import com.cmc.launcher.game.download.addon.platform.curseforge.models.curseForgeModLoaderFilters
import com.cmc.launcher.game.download.addon.platform.modrinth.models.modrinthModLoaderFilters
import com.cmc.launcher.ui.AndroidStringText
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.download.addon.elements.FilterListLayout
import com.cmc.launcher.ui.screens.content.download.addon.elements.FilterSelectionMode
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class FavoriteSortBy { FOLLOW_TIME, TITLE }

private data class FavoriteFilter(
    val platform: Platform? = null,
    val sort: FavoriteSortBy = FavoriteSortBy.FOLLOW_TIME,
    val loaders: List<PlatformDisplayLabel> = emptyList()
)

private val favoriteModLoaderFilters: List<PlatformDisplayLabel> = buildList<PlatformDisplayLabel> {
    addAll(modrinthModLoaderFilters)
    addAll(curseForgeModLoaderFilters)
}.distinctBy { it.getDisplayName() }

private class FavoritesScreenViewModel : ViewModel() {
    var category by mutableStateOf<PlatformClasses?>(null)
        private set
    var searchName by mutableStateOf("")
    var filter by mutableStateOf(FavoriteFilter())
    var refreshing by mutableStateOf(false)
        private set
    var failure by mutableStateOf<AndroidStringText?>(null)
        private set
    var started = false
        private set

    val items: List<FavoriteEntry> by derivedStateOf {
        val keyword = searchName.trim()
        val loaders = filter.loaders.map { it.getDisplayName() }.toSet()
        FavoriteProjectsRepository.projects.values.asSequence()
            .filter { category == null || it.project.classes == category }
            .filter { keyword.isEmpty() || it.displayTitle().contains(keyword, ignoreCase = true) }
            .filter { filter.platform == null || it.platform == filter.platform }
            .filter { entry -> loaders.isEmpty() || entry.remote?.platformModLoaders()?.any { it.getDisplayName() in loaders } == true }
            .sortedWith(when (filter.sort) {
                FavoriteSortBy.FOLLOW_TIME -> compareByDescending { it.project.followTime }
                FavoriteSortBy.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle() }
            }).toList()
    }

    fun selectCategory(value: PlatformClasses?) {
        category = value
        if (value != PlatformClasses.MOD && value != PlatformClasses.MOD_PACK) filter = filter.copy(loaders = emptyList())
    }

    fun reload() {
        if (refreshing) return
        started = true
        refreshing = true
        failure = null
        viewModelScope.launch {
            try {
                FavoriteProjectsRepository.reload()
                FavoriteProjectsRepository.refreshRemote()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failure = mapExceptionToMessage(error)
            } finally {
                refreshing = false
            }
        }
    }
}

private fun FavoriteEntry.displayTitle(): String = remote?.platformTitle() ?: project.title

@Composable
fun FavoritesScreen(
    mainScreenKey: TitledNavKey?, parentScreenKey: TitledNavKey, parentCurrentKey: TitledNavKey?,
    screenKey: TitledNavKey, currentKey: TitledNavKey?,
    swapToDownload: (Platform, PlatformClasses, projectId: String, iconUrl: String?) -> Unit
) {
    val model: FavoritesScreenViewModel = viewModel { FavoritesScreenViewModel() }
    if (mainScreenKey !is NestedNavKey.Download || parentCurrentKey != parentScreenKey || currentKey != screenKey) return
    LaunchedEffect(model) { if (!model.started) model.reload() }
    var showFilter by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
            OwnOutlinedTextField(model.searchName, { model.searchName = it }, Modifier.weight(1f), singleLine = true,
                label = { Text(stringResource(R.string.download_addon_filter_search_name)) }, trailingIcon = {
                    if (model.searchName.isNotEmpty()) IconButton(onClick = { model.searchName = "" }) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.generic_clear))
                    }
                })
            IconButton(enabled = !model.refreshing, onClick = model::reload) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.generic_refresh))
            }
            IconButton(onClick = { showFilter = true }) {
                Icon(painterResource(R.drawable.ic_filter_alt_outlined), stringResource(R.string.pe_search_filters))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup().padding(horizontal = Space.md),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            FavoriteCategory(stringResource(R.string.generic_all), model.category == null) { model.selectCategory(null) }
            PlatformClasses.entries.forEach { category ->
                FavoriteCategory(stringResource(category.categoryText()), model.category == category) { model.selectCategory(category) }
            }
        }
        if (model.refreshing) Text(stringResource(R.string.generic_loading), Modifier.padding(horizontal = Space.lg, vertical = Space.xs),
            style = MaterialTheme.typography.labelMedium)
        HorizontalDivider(Modifier.padding(top = Space.sm))
        model.failure?.let { message ->
            Row(Modifier.fillMaxWidth().padding(Space.md), verticalAlignment = Alignment.CenterVertically) {
                AndroidStringText(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = !model.refreshing, onClick = model::reload) { Text(stringResource(R.string.generic_retry)) }
            }
        }
        val entries = model.items
        when {
            !FavoriteProjectsRepository.initialized && model.refreshing -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.generic_loading))
            }
            entries.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth().padding(Space.lg), contentAlignment = Alignment.Center) {
                Text(stringResource(if (FavoriteProjectsRepository.projects.isEmpty()) R.string.favorites_empty else R.string.favorites_empty_filtered))
            }
            else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                items(entries, key = { "${it.platform}:${it.project.projectId}" }) { entry ->
                    FavoriteProjectLayout(entry, onClick = {
                        swapToDownload(entry.platform, entry.project.classes, entry.project.projectId,
                            entry.remote?.platformIconUrl() ?: entry.project.iconUrl)
                    }, onUnfavorite = { FavoriteProjectsRepository.unfavorite(entry.platform, entry.project.projectId) })
                }
            }
        }
    }
    if (showFilter) {
        var draft by remember { mutableStateOf(model.filter) }
        DialogFrame(stringResource(R.string.pe_search_filters), { showFilter = false }, actions = {
            TextButton(onClick = { draft = FavoriteFilter() }) { Text(stringResource(R.string.generic_reset)) }
            TextButton(onClick = { showFilter = false }) { Text(stringResource(R.string.generic_cancel)) }
            TextButton(onClick = { model.filter = draft; showFilter = false }) { Text(stringResource(R.string.generic_confirm)) }
        }) {
            LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth(), contentPadding = PaddingValues(Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.md)) {
                item {
                    FilterListLayout(title = stringResource(R.string.download_addon_filter_search_platform),
                        items = listOf<Platform?>(null) + Platform.entries, selectionMode = FilterSelectionMode.Single,
                        selectedItems = listOf(draft.platform), cancelable = false,
                        onSelectionChange = { draft = draft.copy(platform = it.firstOrNull()) },
                        getItemLabel = { it?.displayName ?: stringResource(R.string.generic_all) })
                }
                item {
                    FilterListLayout(title = stringResource(R.string.sort_by), items = FavoriteSortBy.entries,
                        selectionMode = FilterSelectionMode.Single, selectedItems = listOf(draft.sort), cancelable = false,
                        onSelectionChange = { it.firstOrNull()?.let { sort -> draft = draft.copy(sort = sort) } },
                        getItemLabel = { stringResource(if (it == FavoriteSortBy.TITLE) R.string.favorites_sort_title else R.string.favorites_sort_follow_time) })
                }
                if (model.category == PlatformClasses.MOD || model.category == PlatformClasses.MOD_PACK) item {
                    FilterListLayout(title = stringResource(R.string.download_addon_filter_modloader), items = favoriteModLoaderFilters,
                        selectionMode = FilterSelectionMode.Multiple, selectedItems = draft.loaders,
                        onSelectionChange = { draft = draft.copy(loaders = it) }, getItemLabel = { it.getDisplayName() })
                }
            }
        }
    }
}

private fun PlatformClasses.categoryText(): Int = when (this) {
    PlatformClasses.MOD -> R.string.download_category_mod
    PlatformClasses.MOD_PACK -> R.string.download_category_modpack
    PlatformClasses.RESOURCE_PACK -> R.string.download_category_resource_pack
    PlatformClasses.SAVES -> R.string.download_category_saves
    PlatformClasses.SHADERS -> R.string.download_category_shaders
}

@Composable
private fun FavoriteCategory(text: String, selected: Boolean, choose: () -> Unit) {
    Text(text, modifier = Modifier.clip(FieldShape)
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
        .selectable(selected, interactionSource = null, indication = null, role = Role.RadioButton, onClick = choose)
        .heightIn(min = Size.touch).padding(horizontal = Space.md, vertical = Space.sm),
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
}
