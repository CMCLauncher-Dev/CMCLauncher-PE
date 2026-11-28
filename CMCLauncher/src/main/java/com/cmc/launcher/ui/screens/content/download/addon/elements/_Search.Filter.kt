package com.cmc.launcher.ui.screens.content.download.addon.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformDisplayLabel
import com.cmc.launcher.game.download.addon.platform.PlatformFilterCode
import com.cmc.launcher.game.download.addon.platform.PlatformSortField
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.ui.components.LittleTextLabel
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.cardColor
import com.cmc.launcher.ui.theme.onCardColor
import org.jackhuang.hmcl.addon.AddonProjectInfo

@Composable
fun SearchFilter(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    enablePlatform: Boolean = true,
    showSearchName: Boolean = true,
    showGameVersion: Boolean = true,
    searchPlatform: Platform,
    onPlatformChange: (Platform) -> Unit = {},
    searchName: String,
    onSearchNameChange: (String) -> Unit = {},
    onSearch: () -> Unit,
    searchedMcMods: List<AddonProjectInfo>,
    searchedVersions: List<String>,
    gameVersion: String,
    onGameVersionChange: (String) -> Unit = {},
    sortField: PlatformSortField,
    onSortFieldChange: (PlatformSortField) -> Unit = {},
    allCategories: List<PlatformFilterCode>,
    categories: List<PlatformFilterCode>,
    onCategoryChanged: (List<PlatformFilterCode>) -> Unit = {},
    enableModLoader: Boolean = true,
    modloaders: List<PlatformDisplayLabel> = emptyList(),
    modloader: PlatformDisplayLabel? = null,
    onModLoaderChange: (PlatformDisplayLabel?) -> Unit = {},
    extraFilter: (LazyListScope.() -> Unit)? = null
) {
    LazyColumn(modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(Space.md)) {
        if (enablePlatform) item(key = "platform") {
            PlatformListLayout(searchPlatform, onPlatformChange, Modifier.fillMaxWidth())
        }
        if (showSearchName) item(key = "query") {
            SuggestionsText(searchName, onSearchNameChange, stringResource(R.string.download_addon_filter_search_name),
                onSearch, searchedMcMods, suggestionLabel = {
                    Text(it.mainName(), style = MaterialTheme.typography.bodyMedium)
                    if (it.subName().isNotBlank()) Text(it.subName(), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }, onSuggestionClick = {
                    onSearchNameChange(it.subName().ifEmpty { it.abbrName() }.ifEmpty { it.mainName() })
                    onSearch()
                })
        }
        if (showGameVersion) item(key = "game-version") {
            SuggestionsText(gameVersion, onGameVersionChange, stringResource(R.string.download_addon_filter_game_version),
                onSearch, searchedVersions, suggestionLabel = { Text(it) }, onSuggestionClick = { onGameVersionChange(it); onSearch() })
        }
        extraFilter?.invoke(this)
        item(key = "sort") {
            FilterListLayout(title = stringResource(R.string.sort_by), items = PlatformSortField.entries,
                selectionMode = FilterSelectionMode.Single, selectedItems = listOf(sortField), cancelable = false,
                onSelectionChange = { next -> next.firstOrNull()?.takeIf { it != sortField }?.let(onSortFieldChange) },
                getItemLabel = { stringResource(it.getDisplayName()) })
        }
        if (allCategories.isNotEmpty()) item(key = "categories") {
            FilterListLayout(title = stringResource(R.string.download_addon_filter_category), items = allCategories,
                selectionMode = FilterSelectionMode.Multiple, selectedItems = categories,
                onSelectionChange = { if (it.toSet() != categories.toSet()) onCategoryChanged(it) },
                getItemLabel = { stringResource(it.getDisplayName()) })
        }
        if (enableModLoader && modloaders.isNotEmpty()) item(key = "loader") {
            FilterListLayout(title = stringResource(R.string.download_addon_filter_modloader), items = modloaders,
                selectionMode = FilterSelectionMode.Single, selectedItems = listOfNotNull(modloader),
                onSelectionChange = { next -> if (next.firstOrNull() != modloader) onModLoaderChange(next.firstOrNull()) },
                getItemLabel = { it.getDisplayName() })
        }
    }
}

enum class FilterSelectionMode { Single, Multiple }

@Composable
private fun <E> SuggestionsText(
    value: String, onValueChange: (String) -> Unit, label: String, onSearch: () -> Unit,
    suggestions: List<E>, suggestionLabel: @Composable FlowRowScope.(E) -> Unit, onSuggestionClick: (E) -> Unit
) {
    val focus = LocalFocusManager.current
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        OwnOutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
            interactionSource = interactions, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); onSearch() }),
            trailingIcon = { IconButton(onClick = { focus.clearFocus(); onSearch() }) {
                Icon(painterResource(R.drawable.ic_search), stringResource(R.string.generic_search))
            } })
        if (focused && suggestions.isNotEmpty()) {
            Surface(shape = FieldShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Column {
                    for (item in suggestions.take(8)) {
                        FlowRow(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = {
                            focus.clearFocus()
                            onSuggestionClick(item)
                        }).heightIn(min = Size.touch).padding(Space.sm),
                            horizontalArrangement = Arrangement.spacedBy(Space.sm)) { suggestionLabel(item) }
                    }
                }
            }
        }
    }
}

@Composable
fun BaseFilterLayout(
    modifier: Modifier = Modifier, shape: Shape = FieldShape, influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground), contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state, onClick: (() -> Unit)? = null, content: @Composable () -> Unit
) {
    Surface(modifier.clip(shape).then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)),
        shape = shape, color = color, contentColor = contentColor) { content() }
}

@Composable
fun <E> FilterListLayout(
    title: String,
    items: List<E>,
    selectionMode: FilterSelectionMode,
    selectedItems: List<E>,
    onSelectionChange: (List<E>) -> Unit,
    getItemLabel: @Composable (E) -> String,
    modifier: Modifier = Modifier,
    selectedLabel: @Composable FlowRowScope.(E) -> Unit = { LittleTextLabel(text = getItemLabel(it), shape = FieldShape) },
    itemLayout: @Composable (E) -> Unit = { Text(getItemLabel(it), style = MaterialTheme.typography.bodyMedium) },
    cancelable: Boolean = true,
    maxListHeight: Dp = 200.dp
) {
    var expanded by remember { mutableStateOf(false) }
    BaseFilterLayout(modifier) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable(enabled = items.isNotEmpty(), role = Role.Button) { expanded = !expanded }
                    .heightIn(min = Size.touch).padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        if (selectedItems.isEmpty()) Text(stringResource(R.string.download_addon_filter_none), style = MaterialTheme.typography.bodySmall)
                        else selectedItems.forEach { selectedLabel(it) }
                    }
                }
                IconButton(enabled = items.isNotEmpty(), onClick = { expanded = !expanded }) {
                    Icon(painterResource(if (expanded) R.drawable.ic_arrow_drop_up_rounded else R.drawable.ic_arrow_drop_down_rounded),
                        stringResource(if (expanded) R.string.generic_collapse else R.string.generic_expand))
                }
                if (cancelable && selectedItems.isNotEmpty()) IconButton(onClick = { onSelectionChange(emptyList()) }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.generic_clear))
                }
            }
            if (expanded) LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxListHeight.coerceAtLeast(Size.touch))
                .then(if (selectionMode == FilterSelectionMode.Single) Modifier.selectableGroup() else Modifier),
                contentPadding = PaddingValues(Space.xs)) {
                items(items) { entry ->
                    val selected = entry in selectedItems
                    val single = selectionMode == FilterSelectionMode.Single
                    fun update(checked: Boolean) {
                        val next = when {
                            single && checked -> listOf(entry)
                            single && !cancelable -> selectedItems
                            single -> emptyList()
                            checked -> (selectedItems + entry).distinct()
                            else -> selectedItems - entry
                        }
                        if (next != selectedItems) onSelectionChange(next)
                        if (single) expanded = false
                    }
                    val action = if (single) Modifier.selectable(selected, role = Role.RadioButton,
                        interactionSource = null, indication = null, onClick = { update(true) })
                    else Modifier.toggleable(selected, role = Role.Checkbox, interactionSource = null, indication = null, onValueChange = ::update)
                    Row(Modifier.fillMaxWidth().clip(FieldShape).then(action).heightIn(min = Size.touch).padding(horizontal = Space.sm),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        if (single) RadioButton(selected, onClick = null) else Checkbox(selected, onCheckedChange = null)
                        Box(Modifier.weight(1f).padding(vertical = Space.sm)) { itemLayout(entry) }
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformListLayout(searchPlatform: Platform, onPlatformChange: (Platform) -> Unit, modifier: Modifier = Modifier) {
    FilterListLayout(title = stringResource(R.string.download_addon_filter_search_platform), items = Platform.entries,
        selectionMode = FilterSelectionMode.Single, selectedItems = listOf(searchPlatform), cancelable = false,
        onSelectionChange = { it.firstOrNull()?.takeIf { next -> next != searchPlatform }?.let(onPlatformChange) },
        getItemLabel = { it.displayName }, modifier = modifier,
        selectedLabel = { PlatformIdentifier(platform = it, shape = MaterialTheme.shapes.small) },
        itemLayout = { platform ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Icon(painterResource(platform.getDrawable()), null, Modifier.size(Size.iconSm))
                Text(platform.displayName, style = MaterialTheme.typography.bodyMedium)
            }
        })
}