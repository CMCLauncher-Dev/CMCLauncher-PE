package com.cmc.launcher.ui.screens.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmc.launcher.R
import com.cmc.launcher.game.account.Account
import com.cmc.launcher.game.account.AccountsManager
import com.cmc.launcher.game.account.getAccountTypeName
import com.cmc.launcher.game.account.wardrobe.toArmModel
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.setting.enums.ActionMenuSide
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.components.AnimatedDirection
import com.cmc.launcher.ui.components.BackgroundCard
import com.cmc.launcher.ui.components.PetModelView
import com.cmc.launcher.ui.components.ScalingActionButton
import com.cmc.launcher.ui.components.ScreenType
import com.cmc.launcher.ui.components.ScreenTypeLayout
import com.cmc.launcher.ui.components.animated
import com.cmc.launcher.ui.components.rememberAnimatedContext
import com.cmc.launcher.ui.guide.GuideKeys
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.content.elements.CommonVersionInfoLayout
import com.cmc.launcher.ui.screens.content.elements.PlayerFace
import com.cmc.launcher.ui.screens.content.elements.VersionIconImage
import com.cmc.launcher.ui.screens.content.home.HomeGrid
import com.cmc.launcher.ui.screens.content.home.LocalActionMenuDrag
import com.cmc.launcher.ui.screens.content.home.actionMenuDragAnchor
import com.cmc.launcher.ui.screens.content.home.actionMenuDragExclusion
import com.cmc.launcher.ui.screens.content.home.rememberActionMenuDragState
import com.cmc.launcher.ui.screens.content.home.server.LocalServerCardQuickPlay
import com.cmc.launcher.ui.screens.content.home.version.LocalHomeCardLauncher
import com.cmc.launcher.ui.screens.content.home.version.LocalHomeCardVersionSettings
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import com.movtery.cardgrid.state.rememberCardGridState
import com.movtery.guide.GuideSide
import com.movtery.guide.guideNode
import com.movtery.petlib.PetAnimation
import com.movtery.petlib.PetTapReaction
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ActionMenuOuterPadding = 12.dp

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
    onQuickPlayServer: (Version?, String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val dockedSide = AllSettings.launcherActionMenuSide.state
        val dragState = rememberActionMenuDragState(
            onCommit = { side -> AllSettings.launcherActionMenuSide.save(side) }
        )
        dragState.dockedSide = dockedSide
        dragState.isRtl = isRtl

        val toAccountManageScreen: () -> Unit = {
            backStackViewModel.mainScreen.navigateTo(NormalNavKey.AccountManager())
        }
        val toVersionManageScreen: () -> Unit = {
            backStackViewModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NormalNavKey.VersionsManager
            )
        }
        val toVersionSettingsScreen: () -> Unit = {
            VersionsManager.currentVersion.value?.let(navigateToVersions)
        }

        ScreenTypeLayout(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .onGloballyPositioned { coordinates ->
                    dragState.parentOrigin = coordinates.positionInRoot()
                }
        ) { screenType ->
            val hasSidePanel = maxWidth >= 600.dp && maxHeight >= 240.dp
            val menuSpan = (maxWidth * 0.32f).coerceIn(236.dp, 380.dp)
            val panelWidth = menuSpan - ActionMenuOuterPadding
            val panelHeight = maxHeight - ActionMenuOuterPadding * 2

            LaunchedEffect(isVisible, hasSidePanel) {
                if (!isVisible || !hasSidePanel) dragState.onDragCancel()
            }

            with(LocalDensity.current) {
                dragState.parentWidthPx = maxWidth.toPx()
                dragState.outerPaddingPx = ActionMenuOuterPadding.toPx()
                dragState.menuSpanPx = menuSpan.toPx()
            }

            val startInset = if (hasSidePanel && dockedSide == ActionMenuSide.START) menuSpan else 0.dp
            val endInset = if (hasSidePanel && dockedSide == ActionMenuSide.END) menuSpan else 0.dp

            CompositionLocalProvider(
                LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) = onOpenLink(uri)
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            val shift = if (hasSidePanel) dragState.previewShift.value else 0f
                            IntOffset(shift.roundToInt(), 0)
                        }
                        .padding(start = startInset, end = endInset)
                ) {
                    HomeContent(
                        modifier = Modifier
                            .weight(1f)
                            .guideNode(GuideKeys.Main.Step.CardTip, holeRadius = 0.dp),
                        isVisible = isVisible,
                        onLaunchGame = { version -> onLaunchGame(version) },
                        onQuickPlayServer = onQuickPlayServer,
                        onOpenVersionSettings = navigateToVersions,
                        onBrowseVersions = toVersionManageScreen,
                        onDownload = { backStackViewModel.navigateToDownload() }
                    )

                    if (!hasSidePanel) {
                        LaunchPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            isVisible = isVisible,
                            showPlayerModel = false,
                            dockedSide = dockedSide,
                            onLaunchGame = onLaunchGame,
                            swapDirection = AnimatedDirection.Bottom,
                            pickUpScale = { 1f },
                            toAccountManageScreen = toAccountManageScreen,
                            toVersionManageScreen = toVersionManageScreen,
                            toVersionSettingsScreen = toVersionSettingsScreen
                        )
                    }
                }

                if (hasSidePanel) {
                    CompositionLocalProvider(LocalActionMenuDrag provides dragState) {
                        LaunchPanel(
                            modifier = Modifier
                                .offset {
                                    val position = if (dragState.floating) {
                                        dragState.cardPosition
                                    } else {
                                        dragState.landingOf(dragState.previewSide ?: dockedSide) +
                                            dragState.settleOffset.value
                                    }
                                    val x = if (isRtl) {
                                        constraints.maxWidth - panelWidth.toPx() - position.x
                                    } else {
                                        position.x
                                    }
                                    IntOffset(x.roundToInt(), position.y.roundToInt())
                                }
                                .size(panelWidth, panelHeight),
                            isVisible = isVisible,
                            showPlayerModel = screenType == ScreenType.Tablet,
                            dockedSide = dockedSide,
                            onLaunchGame = onLaunchGame,
                            swapDirection = if (dockedSide == ActionMenuSide.END) {
                                AnimatedDirection.Right
                            } else {
                                AnimatedDirection.Left
                            },
                            pickUpScale = { dragState.scale },
                            toAccountManageScreen = toAccountManageScreen,
                            toVersionManageScreen = toVersionManageScreen,
                            toVersionSettingsScreen = toVersionSettingsScreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    isVisible: Boolean,
    onLaunchGame: (Version) -> Unit,
    onQuickPlayServer: (Version?, String) -> Unit,
    onOpenVersionSettings: (Version) -> Unit,
    onBrowseVersions: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)
    val gridState = rememberCardGridState()

    CompositionLocalProvider(
        LocalHomeCardLauncher provides onLaunchGame,
        LocalHomeCardVersionSettings provides onOpenVersionSettings,
        LocalServerCardQuickPlay provides onQuickPlayServer
    ) {
        HomeGrid(
            state = gridState,
            onBrowseVersions = onBrowseVersions,
            onDownload = onDownload,
            isVisible = isVisible,
            modifier = modifier
                .fillMaxSize()
                .animated(ctx)
        )
    }
}

@Composable
private fun AccountSummary(
    modifier: Modifier = Modifier,
    account: Account?,
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (account != null) {
            PlayerFace(
                modifier = Modifier.padding(start = 4.dp),
                account = account,
                avatarSize = 32.dp,
            )
        } else {
            Icon(
                modifier = Modifier.size(32.dp),
                painter = painterResource(R.drawable.ic_person_outlined),
                contentDescription = null
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = account?.username ?: stringResource(R.string.page_title_account_list),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall
            )
            if (account != null) {
                Text(
                    text = getAccountTypeName(account),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Icon(
            painter = painterResource(R.drawable.ic_keyboard_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LaunchVersionControls(
    onLaunchGame: (Version?) -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()

    var showList by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CurrentVersionPicker(
                    isRefreshing = isRefreshing,
                    version = version,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (version == null) toVersionManageScreen() else showList = true
                    }
                )
                DropdownMenu(
                    expanded = showList,
                    onDismissRequest = { showList = false },
                    modifier = Modifier.width(280.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.page_title_version_manage)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_package_2_outlined),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showList = false
                            toVersionManageScreen()
                        }
                    )
                    HorizontalDivider()
                    versions.forEach { version0 ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CommonVersionInfoLayout(
                                        modifier = Modifier.weight(1f),
                                        version = version0,
                                        iconSize = 28.dp
                                    )
                                    IconButton(
                                        onClick = {
                                            onLaunchGame(version0)
                                            showList = false
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_play_arrow_filled),
                                            contentDescription = stringResource(R.string.main_launch_game),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (version != version0) VersionsManager.saveVersion(version0)
                                showList = false
                            }
                        )
                    }
                }
            }
            if (version?.isValid() == true && !isRefreshing) {
                IconButton(onClick = toVersionSettingsScreen) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_filled),
                        contentDescription = stringResource(R.string.versions_manage_settings)
                    )
                }
            }
        }

        ScalingActionButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = MaterialTheme.shapes.small,
            elevation = null,
            onClick = {
                onLaunchGame(null)
            },
            content = {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.main_launch_game))
            }
        )
    }
}

@Composable
private fun LaunchPanel(
    isVisible: Boolean,
    showPlayerModel: Boolean,
    dockedSide: ActionMenuSide,
    onLaunchGame: (Version?) -> Unit,
    swapDirection: AnimatedDirection,
    pickUpScale: () -> Float,
    modifier: Modifier = Modifier,
    toAccountManageScreen: () -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val isSidePanel = LocalActionMenuDrag.current != null
    val compactGuide = if (isSidePanel) Modifier else Modifier.guideNode(GuideKeys.Main.Step.CardDrag)

    BackgroundCard(
        modifier = modifier
            .graphicsLayer {
                val scale = pickUpScale()
                scaleX = scale
                scaleY = scale
            }
            .animated(ctx, from = swapDirection)
            .then(compactGuide),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .actionMenuDragAnchor()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSidePanel) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 16.dp)
                        .guideNode(GuideKeys.Main.Step.CardDrag),
                    painter = painterResource(R.drawable.ic_drag_handle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            if (showPlayerModel) {
                PanelPlayerModel(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    account = account,
                    dockedSide = dockedSide
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .guideNode(
                        key = GuideKeys.Main.Step.Account,
                        preferSide = if (showPlayerModel) GuideSide.Above else GuideSide.Below
                    ),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = toAccountManageScreen
            ) {
                AccountSummary(account = account)
            }
            LaunchVersionControls(
                modifier = Modifier.fillMaxWidth(),
                onLaunchGame = onLaunchGame,
                toVersionManageScreen = toVersionManageScreen,
                toVersionSettingsScreen = toVersionSettingsScreen
            )
        }
    }
}

@Composable
private fun PanelPlayerModel(
    modifier: Modifier,
    account: Account?,
    dockedSide: ActionMenuSide
) {
    val refreshWardrobe by AccountsManager.refreshWardrobe.collectAsStateWithLifecycle()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val yaw = Math.toRadians(
        ((if (dockedSide == ActionMenuSide.START) 35.0 else -(35.0)) * (if (isRtl) -1 else 1))
    ).toFloat()

    val armModel = account?.skinModelType?.toArmModel()
    val skinStamp = account?.getSkinFile()?.lastModified()
    val capeStamp = account?.getCapeFile()?.lastModified()
    val skinBytes by produceState<ByteArray?>(null, account, refreshWardrobe, skinStamp) {
        value = withContext(Dispatchers.IO) {
            account?.getSkinFile()?.takeIf { it.exists() }
                ?.inputStream()?.use { it.readBytes() }
        }
    }
    val capeBytes by produceState<ByteArray?>(null, account, refreshWardrobe, capeStamp) {
        value = withContext(Dispatchers.IO) {
            account?.getCapeFile()?.takeIf { it.exists() }
                ?.inputStream()?.use { it.readBytes() }
        }
    }

    PetModelView(
        modifier = modifier,
        skin = skinBytes,
        cape = capeBytes,
        arm = armModel,
        yaw = yaw,
        animation = PetAnimation.Idle,
        tapReaction = PetTapReaction.Interact,
        dragYaw = false
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CurrentVersionPicker(
    isRefreshing: Boolean,
    version: Version?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .actionMenuDragExclusion()
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .guideNode(
                key = GuideKeys.Main.Step.VersionList,
                preferSide = GuideSide.Above,
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRefreshing) {
            Box(modifier = Modifier.fillMaxWidth()) {
                LoadingIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
        } else {
            VersionIconImage(
                version = version,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (version == null) {
                Text(
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                    overflow = TextOverflow.Ellipsis,
                    text = stringResource(R.string.versions_manage_no_versions),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        overflow = TextOverflow.Ellipsis,
                        text = version.getVersionName(),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                    if (version.isValid()) {
                        Text(
                            overflow = TextOverflow.Ellipsis,
                            text = version.getVersionSummary(),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
