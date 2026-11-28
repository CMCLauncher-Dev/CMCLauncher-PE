package com.cmc.launcher.ui.screens.content

import androidx.compose.foundation.basicMarquee
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.favorites.FavoriteProjectsRepository
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.components.AnimatedDirection
import com.cmc.launcher.ui.components.animated
import com.cmc.launcher.ui.components.fadeEdge
import com.cmc.launcher.ui.components.rememberAnimatedContext
import com.cmc.launcher.ui.screens.NestedNavDisplay
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.download.DownloadFavoritesScreen
import com.cmc.launcher.ui.screens.content.download.DownloadGameScreen
import com.cmc.launcher.ui.screens.content.download.DownloadModPackScreen
import com.cmc.launcher.ui.screens.content.download.DownloadModScreen
import com.cmc.launcher.ui.screens.content.download.DownloadResourcePackScreen
import com.cmc.launcher.ui.screens.content.download.DownloadSavesScreen
import com.cmc.launcher.ui.screens.content.download.DownloadShadersScreen
import com.cmc.launcher.ui.screens.content.download.addon.search.SearchIdScreen
import com.cmc.launcher.ui.screens.content.elements.CategoryIcon
import com.cmc.launcher.ui.screens.content.elements.CategoryItem
import com.cmc.launcher.ui.screens.navigateOnce
import com.cmc.launcher.ui.screens.onBack
import com.cmc.launcher.ui.screens.rememberTransitionSpec
import com.cmc.launcher.viewmodel.ErrorViewModel
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.ModpackImportViewModel
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.Radius
import com.cmc.launcher.ui.kit.CmcSection
import com.cmc.launcher.ui.kit.CmcCard

fun ScreenBackStackViewModel.navigateToDownload(targetScreen: TitledNavKey? = null) {
    downloadHubVisible = targetScreen == null
    downloadScreen.clearWith(targetScreen ?: downloadGameScreen)
    mainScreen.removeAndNavigateTo(
        removes = clearBeforeNavKeys,
        screenKey = downloadScreen,
        useClassEquality = true
    )
}

private fun ScreenBackStackViewModel.swapToCategoryAddon(
    platform: Platform,
    classes: PlatformClasses,
    projectId: String,
    iconUrl: String?
) {
    val targetScreen = when (classes) {
        PlatformClasses.MOD -> downloadModScreen
        PlatformClasses.MOD_PACK -> downloadModPackScreen
        PlatformClasses.RESOURCE_PACK -> downloadResourcePackScreen
        PlatformClasses.SAVES -> downloadSavesScreen
        PlatformClasses.SHADERS -> downloadShadersScreen
    }
    navigateToDownload(
        targetScreen = targetScreen.apply {
            navigateTo(
                NormalNavKey.DownloadAddon(
                    platform = platform,
                    projectId = projectId,
                    classes = classes,
                    iconUrl = iconUrl
                )
            )
        }
    )
}

@Composable
fun DownloadScreen(
    key: NestedNavKey.Download,
    backScreenViewModel: ScreenBackStackViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {

    LaunchedEffect(Unit) {
        FavoriteProjectsRepository.ensureLoaded()
    }

    val showHub = backScreenViewModel.downloadHubVisible

    BackHandler(enabled = !showHub) {
        backScreenViewModel.downloadHubVisible = true
    }

    BaseScreen(
        screenKey = key,
        currentKey = backScreenViewModel.mainScreen.currentKey,
        useClassEquality = true
    ) { isVisible: Boolean ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (!showHub) {
                NavigationUI(
                    key = key,
                    backScreenViewModel = backScreenViewModel,
                    eventViewModel = eventViewModel,
                    modpackImportViewModel = modpackImportViewModel,
                    submitError = submitError,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                DownloadHub(
                    isVisible = isVisible,
                    backScreenViewModel = backScreenViewModel,
                    onOpenCategory = { target ->
                        backScreenViewModel.downloadHubVisible = false
                        key.backStack.navigateOnce(target)
                    }
                )
            }
        }
    }
}

private data class HubEntry(
    val target: TitledNavKey,
    val title: String,
    val caption: String,
    @DrawableRes val icon: Int
)

@Composable
private fun DownloadHub(
    isVisible: Boolean,
    backScreenViewModel: ScreenBackStackViewModel,
    onOpenCategory: (TitledNavKey) -> Unit
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)

    val entries = listOf(
        HubEntry(backScreenViewModel.downloadGameScreen, stringResource(R.string.pe_download_game_title), stringResource(R.string.pe_download_game_summary), R.drawable.ic_sports_esports_outlined),
        HubEntry(backScreenViewModel.downloadModPackScreen, stringResource(R.string.pe_download_modpack_title), stringResource(R.string.pe_download_modpack_summary), R.drawable.ic_package_2_outlined),
        HubEntry(backScreenViewModel.downloadModScreen, stringResource(R.string.pe_download_mod_title), stringResource(R.string.pe_download_mod_summary), R.drawable.ic_extension_outlined),
        HubEntry(backScreenViewModel.downloadResourcePackScreen, stringResource(R.string.pe_download_resource_pack_title), stringResource(R.string.pe_download_resource_pack_summary), R.drawable.ic_format_paint_outlined),
        HubEntry(backScreenViewModel.downloadSavesScreen, stringResource(R.string.pe_download_saves_title), stringResource(R.string.pe_download_saves_summary), R.drawable.ic_public),
        HubEntry(backScreenViewModel.downloadShadersScreen, stringResource(R.string.pe_download_shaders_title), stringResource(R.string.pe_download_shaders_summary), R.drawable.ic_lightbulb),
        HubEntry(NormalNavKey.SearchId, stringResource(R.string.pe_download_search_id_title), stringResource(R.string.pe_download_search_id_summary), R.drawable.ic_card),
        HubEntry(backScreenViewModel.downloadFavoritesScreen, stringResource(R.string.pe_download_favorites_title), stringResource(R.string.pe_download_favorites_summary), R.drawable.ic_favorite_outlined)
    )

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .animated(ctx, from = AnimatedDirection.Top),
        contentPadding = PaddingValues(
            start = Space.screenH,
            end = Space.screenH,
            top = Space.sm,
            bottom = Space.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        item {
            Column(modifier = Modifier.padding(start = Space.xs, top = Space.xs, bottom = Space.xs)) {
                Text(
                    text = stringResource(R.string.pe_download_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.pe_download_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { CmcSection(title = stringResource(R.string.pe_download_section), caption = stringResource(R.string.pe_download_section_caption)) }

        items(entries.size) { index ->
            val entry = entries[index]
            CmcCard(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.medium),
                onClick = { onOpenCategory(entry.target) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.small))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(entry.icon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(Space.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = entry.caption,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right_rounded),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationUI(
    key: NestedNavKey.Download,
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val backStack = key.backStack

    if (backStack.isNotEmpty()) {
        NestedNavDisplay(
            ownerKey = key,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NestedNavKey.DownloadGame> { key ->
                    DownloadGameScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreen.currentKey,
                        downloadScreenKey = backScreenViewModel.downloadScreen.currentKey,
                        downloadGameScreenKey = backScreenViewModel.downloadGameScreen.currentKey,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NestedNavKey.DownloadModPack> { key ->
                    DownloadModPackScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreen.currentKey,
                        downloadScreenKey = backScreenViewModel.downloadScreen.currentKey,
                        downloadModPackScreenKey = backScreenViewModel.downloadModPackScreen.currentKey,
                        eventViewModel = eventViewModel,
                        importerViewModel = modpackImportViewModel
                    )
                }
                entry<NestedNavKey.DownloadMod> { key ->
                    DownloadModScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreen.currentKey,
                        downloadScreenKey = backScreenViewModel.downloadScreen.currentKey,
                        downloadModScreenKey = backScreenViewModel.downloadModScreen.currentKey,
                        submitError = submitError,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NestedNavKey.DownloadResourcePack> { key ->
                    DownloadResourcePackScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreen.currentKey,
                        downloadScreenKey = backScreenViewModel.downloadScreen.currentKey,
                        downloadResourcePackScreenKey = backScreenViewModel.downloadResourcePackScreen.currentKey,
                        submitError = submitError,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NestedNavKey.DownloadSaves> { key ->
                    DownloadSavesScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreen.currentKey,
                        downloadScreenKey = backScreenViewModel.downloadScreen.currentKey,
                        downloadSavesScreenKey = backScreenViewModel.downloadSavesScreen.currentKey,
                        submitError = submitError,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NestedNavKey.DownloadShaders> { key ->
                    DownloadShadersScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreen.currentKey,
                        downloadScreenKey = backScreenViewModel.downloadScreen.currentKey,
                        downloadShadersScreenKey = backScreenViewModel.downloadShadersScreen.currentKey,
                        submitError = submitError,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.SearchId> {
                    SearchIdScreen(
                        mainScreenKey = backScreenViewModel.mainScreen.currentKey,
                        downloadScreenKey = backScreenViewModel.downloadScreen.currentKey,
                        swapToDownload = { platform, classes, projectId, iconUrl ->
                            backScreenViewModel.swapToCategoryAddon(platform, classes, projectId, iconUrl)
                        },
                        openLink = { link ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(link))
                        }
                    )
                }
                entry<NestedNavKey.DownloadFavorites> { key ->
                    DownloadFavoritesScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreen.currentKey,
                        downloadScreenKey = backScreenViewModel.downloadScreen.currentKey,
                        downloadFavoritesScreenKey = backScreenViewModel.downloadFavoritesScreen.currentKey,
                        swapToDownload = { platform, classes, projectId, iconUrl ->
                            backScreenViewModel.swapToCategoryAddon(platform, classes, projectId, iconUrl)
                        }
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}
