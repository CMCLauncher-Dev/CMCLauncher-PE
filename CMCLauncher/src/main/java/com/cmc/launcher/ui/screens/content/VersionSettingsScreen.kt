package com.cmc.launcher.ui.screens.content

import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.notification.NotificationManager
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.base.VersionViewModelStoreScope
import com.cmc.launcher.ui.components.AnimatedDirection
import com.cmc.launcher.ui.components.animated
import com.cmc.launcher.ui.components.fadeEdge
import com.cmc.launcher.ui.components.rememberAnimatedContext
import com.cmc.launcher.ui.screens.NestedNavDisplay
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.elements.CategoryIcon
import com.cmc.launcher.ui.screens.content.elements.CategoryItem
import com.cmc.launcher.ui.screens.content.versions.ModifyVersionScreen
import com.cmc.launcher.ui.screens.content.versions.ModsManagerScreen
import com.cmc.launcher.ui.screens.content.versions.ResourcePackManageScreen
import com.cmc.launcher.ui.screens.content.versions.SavesManagerScreen
import com.cmc.launcher.ui.screens.content.versions.ScreenshotsManagerScreen
import com.cmc.launcher.ui.screens.content.versions.ServerListScreen
import com.cmc.launcher.ui.screens.content.versions.ShadersManagerScreen
import com.cmc.launcher.ui.screens.content.versions.VersionConfigScreen
import com.cmc.launcher.ui.screens.content.versions.VersionOverViewScreen
import com.cmc.launcher.ui.screens.content.versions.layouts.VersionWorkspace
import com.cmc.launcher.ui.screens.navigateOnce
import com.cmc.launcher.ui.screens.onBack
import com.cmc.launcher.ui.screens.rememberTransitionSpec
import com.cmc.launcher.viewmodel.ErrorViewModel
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.ModifyOperation
import com.cmc.launcher.viewmodel.ModifyPayload
import com.cmc.launcher.viewmodel.ModifyVersionViewModel
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import com.cmc.launcher.viewmodel.sendToast

@Composable
fun VersionSettingsScreen(
    key: NestedNavKey.VersionSettings,
    modifyViewModel: ModifyVersionViewModel,
    backScreenViewModel: ScreenBackStackViewModel,
    backToMainScreen: () -> Unit,
    onExportModpack: () -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current

    val cBackToMainScreen by rememberUpdatedState(backToMainScreen)
    DisposableEffect(key) {
        val listener = object : suspend () -> Unit {
            override suspend fun invoke() {
                cBackToMainScreen()
            }
        }
        VersionsManager.registerListener(listener)
        onDispose {
            VersionsManager.unregisterListener(listener)
        }
    }

    VersionViewModelStoreScope(version = key.version) {
        BaseScreen(
            screenKey = key,
            currentKey = backScreenViewModel.mainScreen.currentKey
        ) {
            VersionWorkspace(
                name = key.version.getVersionName(),
                page = key.backStack.lastOrNull(),
                onSelect = { key.navigateOnce(it) }
            ) {
                NavigationUI(
                    modifier = Modifier.fillMaxSize(),
                    key = key,
                    modifyViewModel = modifyViewModel,
                    backScreenViewModel = backScreenViewModel,
                    versionsScreenKey = key.currentKey,
                    backToMainScreen = backToMainScreen,
                    onExport = onExportModpack,
                    version = key.version,
                    onModify = { payload ->
                        if (modifyViewModel.installOperation !is ModifyOperation.None) {

                            return@NavigationUI
                        }
                        if (!NotificationManager.checkNotificationEnabled(context)) {

                            modifyViewModel.installOperation = ModifyOperation.WarningForNotification(payload)
                        } else {
                            modifyViewModel.installOperation = ModifyOperation.Confirm(payload)
                        }
                    },
                    eventViewModel = eventViewModel,
                    submitError = submitError
                )
            }
        }
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    key: NestedNavKey.VersionSettings,
    modifyViewModel: ModifyVersionViewModel,
    backScreenViewModel: ScreenBackStackViewModel,
    versionsScreenKey: TitledNavKey?,
    backToMainScreen: () -> Unit,
    onExport: () -> Unit,
    version: Version,
    onModify: (ModifyPayload) -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val mainScreenKey = backScreenViewModel.mainScreen.currentKey

    val backStack = key.backStack

    if (backStack.isNotEmpty()) {
        NestedNavDisplay(
            ownerKey = key,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backScreenViewModel.mainScreen.backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.Versions.OverView> {
                    VersionOverViewScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        backToMainScreen = backToMainScreen,
                        onExport = onExport,
                        version = version,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.Config> {
                    VersionConfigScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        onCheckVulkan = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.VulkanCheck(version)
                            )
                        },
                        showToast = { text ->
                            eventViewModel.sendToast(text)
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ModifyVersion> {
                    ModifyVersionScreen(
                        viewModel = modifyViewModel,
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        eventViewModel = eventViewModel,
                        onModify = onModify
                    )
                }
                entry(NormalNavKey.Versions.ModsManager) {
                    ModsManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload = {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadModScreen
                            )
                        },
                        onSwapMoreInfo = { projectId, platform ->
                            backScreenViewModel.mainScreen.removeAndNavigateTo(
                                NestedNavKey.AddonInfo::class,
                                NestedNavKey.AddonInfo(platform, projectId, PlatformClasses.MOD)
                            )
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.SavesManager> {
                    SavesManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload = {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadSavesScreen
                            )
                        },
                        onQuickPlay = { version, saveName ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.PlaySave(
                                    version = version,
                                    saveName = saveName
                                )
                            )
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ResourcePackManager> {
                    ResourcePackManageScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload =  {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadResourcePackScreen
                            )
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ShadersManager> {
                    ShadersManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        swapToDownload = {
                            backScreenViewModel.navigateToDownload(
                                targetScreen = backScreenViewModel.downloadShadersScreen
                            )
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ScreenshotsManager> {
                    ScreenshotsManagerScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Versions.ServerList> {
                    ServerListScreen(
                        mainScreenKey = mainScreenKey,
                        versionsScreenKey = versionsScreenKey,
                        version = version,
                        onQuickPlay = { version, address ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.PlayServer(
                                    version = version,
                                    address = address
                                )
                            )
                        },
                        backToMainScreen = backToMainScreen,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}
