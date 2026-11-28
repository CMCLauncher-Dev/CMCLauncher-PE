package com.cmc.launcher.ui.screens.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmc.launcher.R
import com.cmc.launcher.game.account.AccountsManager
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.content.home.HomeGrid
import com.cmc.launcher.ui.screens.content.home.LaunchDock
import com.cmc.launcher.ui.screens.content.home.server.LocalServerCardQuickPlay
import com.cmc.launcher.ui.screens.content.home.version.LocalHomeCardLauncher
import com.cmc.launcher.ui.screens.content.home.version.LocalHomeCardVersionSettings
import com.cmc.launcher.ui.theme.CardShape
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.ui.screens.content.elements.backgroundGlass
import com.cmc.launcher.ui.theme.cardColor
import com.cmc.launcher.ui.theme.launcherPanelColor
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import com.movtery.cardgrid.state.rememberCardGridState

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
    onQuickPlayServer: (Version?, String) -> Unit,
    onOpenLink: (String) -> Unit
) {
    if (backStackViewModel.mainScreen.backStack.lastOrNull() != NormalNavKey.LauncherMain) return
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val refreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val grid = rememberCardGridState()
    val toAccounts = {
        backStackViewModel.mainScreen.navigateTo(NormalNavKey.AccountManager(FirstLoginMenu.NONE))
    }
    val toVersions = {
        backStackViewModel.mainScreen.removeAndNavigateTo(
            remove = NestedNavKey.VersionSettings::class, screenKey = NormalNavKey.VersionsManager
        )
    }
    val toDownload = {
        backStackViewModel.downloadGameScreen.clearWith(NormalNavKey.DownloadGame.SelectGameVersion)
        backStackViewModel.navigateToDownload(backStackViewModel.downloadGameScreen)
    }
    CompositionLocalProvider(
        LocalHomeCardLauncher provides { onLaunchGame(it) },
        LocalHomeCardVersionSettings provides navigateToVersions,
        LocalServerCardQuickPlay provides onQuickPlayServer,
        LocalUriHandler provides object : UriHandler {
            override fun openUri(uri: String) = onOpenLink(uri)
        }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = Space.screenH)) {
            val wide = maxWidth >= 560.dp
            val header: @Composable () -> Unit = {
                HomeShortcuts(toVersions, toDownload)
                Row(Modifier.fillMaxWidth().padding(top = Space.lg, bottom = Space.sm),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pe_home_cards), Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                    if (grid.cards.isNotEmpty()) Text(grid.cards.size.toString(), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val dock: @Composable () -> Unit = {
                LaunchDock(account, version, versions, refreshing, toAccounts, toVersions, navigateToVersions,
                    toDownload, { onLaunchGame(version) })
            }
            if (wide) {
                Row(Modifier.fillMaxSize().padding(top = Space.sm, bottom = Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
                    HomeGrid(grid, true, Modifier.weight(1f), header = header, emptyContent = { EmptyHomeCards() })
                    Column(Modifier.width(280.dp).verticalScroll(rememberScrollState())) { dock() }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    HomeGrid(grid, true, Modifier.fillMaxWidth().weight(1f).padding(top = Space.sm),
                        header = header, emptyContent = { EmptyHomeCards() })
                    dock()
                    Box(Modifier.size(0.dp, Space.xxs))
                }
            }
        }
    }
}

@Composable
private fun HomeShortcuts(versions: () -> Unit, download: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        HomeShortcut(stringResource(R.string.page_title_version_manage), R.drawable.ic_package_2_outlined,
            versions, Modifier.weight(1f))
        HomeShortcut(stringResource(R.string.download_category_game), R.drawable.ic_download_2_outlined,
            download, Modifier.weight(1f))
    }
}

@Composable
private fun HomeShortcut(title: String, icon: Int, open: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = CardShape, color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.09f))) {
        Column(Modifier.fillMaxWidth().clip(CardShape)
            .background(launcherPanelColor())
            .backgroundGlass(AllSettings.backgroundBlur.state, cardColor())
            .clickable(role = Role.Button, interactionSource = null, indication = com.cmc.launcher.ui.kit.ImmediateIndication, onClick = open)
            .heightIn(min = 96.dp).padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.size(36.dp).clip(FieldShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, Modifier.size(23.dp), tint = colors.onPrimaryContainer)
            }
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyHomeCards() {
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))) {
        Row(Modifier.fillMaxWidth()
            .background(launcherPanelColor())
            .backgroundGlass(AllSettings.backgroundBlur.state, cardColor())
            .padding(Space.lg), horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_dashboard_outlined), null, Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.pe_home_cards_empty), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}