package com.cmc.launcher.ui.screens.content.download.addon.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.game.download.addon.platform.PlatformDisplayLabel
import com.cmc.launcher.game.download.addon.platform.PlatformFilterCode
import com.cmc.launcher.game.download.addon.utils.getMcmodTitle
import com.cmc.launcher.game.version.mod.InstalledMod
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.ui.AndroidStringText
import com.cmc.launcher.ui.androidText
import com.cmc.launcher.ui.components.SimpleEditDialog
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.cardColor
import com.cmc.launcher.ui.theme.onCardColor
import com.cmc.launcher.utils.formatNumberByLocale

sealed interface SearchAddonState {
    data object Searching : SearchAddonState
    data class Success(val page: AddonPage) : SearchAddonState
    data class Error(val message: AndroidStringText) : SearchAddonState
}

@Composable
fun ResultListLayout(
    modifier: Modifier = Modifier,
    classes: PlatformClasses,
    searchState: SearchAddonState,
    controllerHeight: Dp = 54.dp,
    controllerMinScale: Float = 0.9f,
    controllerMinAlpha: Float = 0.8f,
    onReload: () -> Unit = {},
    onPreviousPage: (pageNumber: Int) -> Unit,
    onNextPage: (pageNumber: Int, isLastPage: Boolean) -> Unit,
    onNavigatePage: (Int) -> Unit,
    swapToDownload: (Platform, projectId: String, iconUrl: String?) -> Unit = { _, _, _ -> },
    installedInfo: ((Platform, projectId: String) -> InstalledMod?)? = null
) {
    val context = LocalContext.current
    Column(modifier) {
        when (searchState) {
            SearchAddonState.Searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.generic_loading), style = MaterialTheme.typography.bodyMedium)
            }
            is SearchAddonState.Error -> Column(Modifier.fillMaxSize().padding(Space.lg),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                AndroidStringText(text = androidText(R.string.download_addon_failed_to_get_result, searchState.message))
                TextButton(onClick = onReload) { Text(stringResource(R.string.generic_retry)) }
            }
            is SearchAddonState.Success -> {
                val page = searchState.page
                if (page.data.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth().padding(Space.lg), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.pe_search_empty))
                } else key(page.pageIndex, page.data) {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(Space.md),
                        verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        items(page.data) { (item, mcmod) ->
                            val platform = item.platform()
                            val id = item.platformId()
                            ResultProjectLayout(
                                platform = platform, title = mcmod.getMcmodTitle(item.platformTitle(), context),
                                description = item.platformDescription(), iconUrl = item.platformIconUrl(), authors = item.platformAuthors(),
                                downloads = item.platformDownloadCount(), followsCount = item.platformFollows(),
                                modloaders = item.platformModLoaders(), categories = item.platformCategories(classes)?.sortedBy { it.index() },
                                isInstalled = installedInfo?.invoke(platform, id) != null,
                                onClick = { swapToDownload(platform, id, item.platformIconUrl()) }
                            )
                        }
                    }
                }
                HorizontalDivider()
                PageController(page, onReload, { onPreviousPage(page.pageNumber) },
                    { onNextPage(page.pageNumber, page.isLastPage) }, onNavigatePage, controllerHeight)
            }
        }
    }
}

@Composable
private fun PageController(page: AddonPage, reload: () -> Unit, previous: () -> Unit, next: () -> Unit,
    navigate: (Int) -> Unit, minimumHeight: Dp) {
    var editing by remember(page.pageNumber) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = minimumHeight.coerceAtLeast(Size.touch)).padding(horizontal = Space.sm),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = reload) { Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.generic_refresh)) }
        TextButton(enabled = page.totalPage > 0, onClick = { editing = true }, modifier = Modifier.weight(1f)) {
            Text(if (page.totalPage > 0) "${page.pageNumber} / ${page.totalPage}" else "${page.pageNumber}",
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(enabled = page.pageNumber > 1, onClick = previous) {
            Icon(painterResource(R.drawable.ic_arrow_left_rounded), stringResource(R.string.download_addon_result_previous_page))
        }
        IconButton(enabled = !page.isLastPage, onClick = next) {
            Icon(painterResource(R.drawable.ic_arrow_right_rounded), stringResource(R.string.download_addon_result_next_page))
        }
    }
    if (editing) {
        var input by remember { mutableStateOf(page.pageNumber.toString()) }
        val target = input.toIntOrNull()
        val valid = target != null && target in 1..page.totalPage
        SimpleEditDialog(title = stringResource(R.string.pe_search_page), value = input,
            onValueChange = { input = it }, singleLine = true, isError = !valid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onDismissRequest = { editing = false }, onConfirm = {
                if (valid && target != null) {
                    editing = false
                    if (target != page.pageNumber) navigate(target)
                }
            })
    }
}

@Composable
fun ResultProjectLayout(
    modifier: Modifier = Modifier,
    platform: Platform,
    title: String,
    description: String,
    classes: PlatformClasses? = null,
    iconUrl: String? = null,
    authors: List<String>? = null,
    downloads: Long = 0L,
    modloaders: List<PlatformDisplayLabel>? = null,
    categories: List<PlatformFilterCode>? = null,
    followsCount: Long? = null,
    isInstalled: Boolean = false,
    shape: Shape = FieldShape,
    influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground),
    contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Surface(modifier.fillMaxWidth(), shape = shape, color = color, contentColor = contentColor) {
        Column(Modifier.clip(shape).clickable(role = Role.Button, onClick = onClick).padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
                AddonIcon(Modifier.clip(FieldShape), size = Size.touch, iconUrl = iconUrl)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!authors.isNullOrEmpty()) Text(authors.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                PlatformIdentifier(platform = platform)
                if (classes != null) ClassesIdentifier(classes)
                if (isInstalled) InstalledModBadge()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Icon(painterResource(R.drawable.ic_download_2_filled), null, Modifier.size(Size.iconXs))
                    Text(formatNumberByLocale(context, downloads), style = MaterialTheme.typography.labelSmall)
                }
                followsCount?.let { count ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Icon(painterResource(R.drawable.ic_star_filled), null, Modifier.size(Size.iconXs))
                        Text(formatNumberByLocale(context, count), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (!modloaders.isNullOrEmpty() || !categories.isNullOrEmpty()) {
                val tags = (modloaders.orEmpty().map { it.getDisplayName() } +
                    categories.orEmpty().map { stringResource(it.getDisplayName()) }).distinct()
                Text(tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ProjectTitleHead(modifier: Modifier = Modifier, platform: Platform, title: String) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        PlatformIdentifier(platform = platform)
    }
}