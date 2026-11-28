package com.cmc.launcher.ui.screens.content.download.addon.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.game.download.addon.platform.PlatformDisplayLabel
import com.cmc.launcher.game.download.addon.platform.PlatformFilterCode
import com.cmc.launcher.game.download.addon.platform.PlatformSearchFilter
import com.cmc.launcher.game.version.mod.InstalledMod
import com.cmc.launcher.ui.components.DialogFrame
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.download.addon.elements.ResultListLayout
import com.cmc.launcher.ui.screens.content.download.addon.elements.SearchFilter
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

@Composable
fun SearchAddonScreen(
    mainScreenKey: TitledNavKey?,
    parentScreenKey: TitledNavKey,
    parentCurrentKey: TitledNavKey?,
    screenKey: TitledNavKey,
    currentKey: TitledNavKey?,
    platformClasses: PlatformClasses,
    initialPlatform: Platform,
    onPlatformChange: (Platform) -> Unit = {},
    enablePlatform: Boolean = true,
    getCategories: (Platform) -> List<PlatformFilterCode>,
    enableModLoader: Boolean = false,
    getModloaders: (Platform) -> List<PlatformDisplayLabel> = { emptyList() },
    mapCategories: (Platform, String) -> PlatformFilterCode?,
    swapToDownload: (Platform, projectId: String, iconUrl: String?) -> Unit = { _, _, _ -> },
    installedInfo: ((Platform, projectId: String) -> InstalledMod?)? = null,
    extraFilter: (LazyListScope.() -> Unit)? = null
) {
    val model: SearchAddonViewModel = viewModel(key = "${parentScreenKey}_${screenKey}_${platformClasses}_search") {
        SearchAddonViewModel(initialPlatform, platformClasses)
    }
    if (mainScreenKey !is NestedNavKey.Download || parentScreenKey != parentCurrentKey || screenKey != currentKey) return
    val platform by model.platform.collectAsStateWithLifecycle()
    val filter by model.filter.collectAsStateWithLifecycle()
    val result by model.result.collectAsStateWithLifecycle()
    var query by rememberSaveable(filter.searchName) { mutableStateOf(filter.searchName) }
    var showFilter by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    fun submitName() {
        focus.clearFocus()
        model.apply(platform, filter.copy(searchName = query.trim()))
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
            OwnOutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.download_addon_filter_search_name)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitName() }),
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.generic_clear), Modifier.size(Size.iconSm))
                    }
                })
            IconButton(onClick = ::submitName) {
                Icon(painterResource(R.drawable.ic_search), stringResource(R.string.generic_search))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.md), verticalAlignment = Alignment.CenterVertically) {
            Text(platform.displayName + filter.gameVersion.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { focus.clearFocus(); showFilter = true }) {
                Icon(painterResource(R.drawable.ic_filter_alt_outlined), null, Modifier.size(Size.iconSm))
                Text(stringResource(R.string.pe_search_filters), Modifier.padding(start = Space.xs))
            }
        }
        if (extraFilter != null) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 112.dp),
                contentPadding = PaddingValues(horizontal = Space.md, vertical = Space.xs), content = extraFilter)
        }
        HorizontalDivider()
        ResultListLayout(modifier = Modifier.fillMaxWidth().weight(1f), classes = platformClasses, searchState = result,
            onReload = model::reload, onPreviousPage = { model.goToPage(it - 1) },
            onNextPage = { page, last -> if (!last && page < Int.MAX_VALUE) model.goToPage(page + 1) },
            onNavigatePage = model::goToPage, swapToDownload = swapToDownload, installedInfo = installedInfo)
    }
    if (showFilter) {
        SearchOptionsDialog(model, platform, filter.copy(searchName = query), enablePlatform, enableModLoader,
            getCategories, getModloaders, null, dismiss = { showFilter = false }, apply = { nextPlatform, nextFilter ->
                showFilter = false
                query = nextFilter.searchName.trim()
                model.apply(nextPlatform, nextFilter.copy(searchName = query))
                if (nextPlatform != platform) onPlatformChange(nextPlatform)
            })
    }
}

@Composable
private fun SearchOptionsDialog(
    model: SearchAddonViewModel, platform: Platform, filter: PlatformSearchFilter,
    enablePlatform: Boolean, enableModLoader: Boolean,
    categories: (Platform) -> List<PlatformFilterCode>, loaders: (Platform) -> List<PlatformDisplayLabel>,
    extra: (LazyListScope.() -> Unit)?, dismiss: () -> Unit, apply: (Platform, PlatformSearchFilter) -> Unit
) {
    var draftPlatform by remember { mutableStateOf(platform) }
    var draft by remember { mutableStateOf(filter) }
    val names by model.names.collectAsStateWithLifecycle()
    val versions by model.versions.collectAsStateWithLifecycle()
    val choices = categories(draftPlatform)
    val modloaders = loaders(draftPlatform)
    DisposableEffect(model) {
        model.suggestName(draft.searchName)
        model.suggestVersion(draft.gameVersion, draftPlatform)
        onDispose { model.closeSuggestions() }
    }
    val submit = { apply(draftPlatform, draft) }
    DialogFrame(stringResource(R.string.pe_search_filters), dismiss, actions = {
        TextButton(onClick = { draft = PlatformSearchFilter(); model.suggestName(""); model.suggestVersion("", draftPlatform) }) {
            Text(stringResource(R.string.generic_reset))
        }
        TextButton(onClick = dismiss) { Text(stringResource(R.string.generic_cancel)) }
        TextButton(onClick = submit) { Text(stringResource(R.string.generic_search)) }
    }) {
        SearchFilter(
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.sm), enablePlatform = enablePlatform,
            searchPlatform = draftPlatform, onPlatformChange = {
                draftPlatform = it
                draft = draft.copy(categories = emptyList(), modloader = null)
                model.suggestVersion(draft.gameVersion, it)
            },
            searchName = draft.searchName, onSearchNameChange = { draft = draft.copy(searchName = it); model.suggestName(it) },
            onSearch = submit, searchedMcMods = names, searchedVersions = versions,
            gameVersion = draft.gameVersion, onGameVersionChange = {
                draft = draft.copy(gameVersion = it)
                model.suggestVersion(it, draftPlatform)
            },
            sortField = draft.sortField, onSortFieldChange = { draft = draft.copy(sortField = it) },
            allCategories = choices, categories = draft.categories, onCategoryChanged = { draft = draft.copy(categories = it) },
            enableModLoader = enableModLoader, modloaders = modloaders, modloader = draft.modloader,
            onModLoaderChange = { draft = draft.copy(modloader = it) }, extraFilter = extra
        )
    }
}