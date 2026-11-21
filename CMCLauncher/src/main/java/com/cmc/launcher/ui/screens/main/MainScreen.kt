package com.cmc.launcher.ui.screens.main

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.cmc.launcher.BuildKeys
import com.cmc.launcher.R
import com.cmc.launcher.coroutine.TaskSystem
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.path.PathManager
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.setting.enums.isLauncherInDarkTheme
import com.cmc.launcher.ui.AndroidStringText
import com.cmc.launcher.ui.androidText
import com.cmc.launcher.ui.components.BackgroundCard
import com.cmc.launcher.ui.components.CardTitleLayout
import com.cmc.launcher.ui.screens.BackStackNavKey
import com.cmc.launcher.ui.screens.NavStateHost
import com.cmc.launcher.ui.screens.NestedNavDisplay
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.AccountManageScreen
import com.cmc.launcher.ui.screens.content.DownloadScreen
import com.cmc.launcher.ui.screens.content.FileSelectorScreen
import com.cmc.launcher.ui.screens.content.LauncherScreen
import com.cmc.launcher.ui.screens.content.LogViewScreen
import com.cmc.launcher.ui.screens.content.MultiplayerScreen
import com.cmc.launcher.ui.screens.content.SettingsScreen
import com.cmc.launcher.ui.screens.content.VersionExportScreen
import com.cmc.launcher.ui.screens.content.VersionSettingsScreen
import com.cmc.launcher.ui.screens.content.VersionsManageScreen
import com.cmc.launcher.ui.screens.content.WebViewScreen
import com.cmc.launcher.ui.screens.content.addoninfo.AddonInfoScreen
import com.cmc.launcher.ui.screens.content.navigateToDownload
import com.cmc.launcher.ui.screens.navigateTo
import com.cmc.launcher.ui.screens.onBack
import com.cmc.launcher.ui.screens.rememberTransitionSpec
import com.cmc.launcher.ui.theme.backgroundColor
import com.cmc.launcher.ui.theme.cardColor
import com.cmc.launcher.ui.theme.cmc.cmcBackgroundBrush
import com.cmc.launcher.ui.theme.onBackgroundColor
import com.cmc.launcher.ui.theme.onCardColor
import com.cmc.launcher.utils.file.formatFileSize
import com.cmc.launcher.viewmodel.ErrorViewModel
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.LocalBackgroundViewModel
import com.cmc.launcher.viewmodel.ModifyVersionViewModel
import com.cmc.launcher.viewmodel.ModpackImportViewModel
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import com.cmc.launcher.viewmodel.sendKeepScreen
import com.cmc.launcher.viewmodel.sendToast

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {

    var hasRunningTasks by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        TaskSystem.tasksFlow.collect { tasks ->
            hasRunningTasks = tasks.isNotEmpty()
            eventViewModel.sendKeepScreen(hasRunningTasks)
        }
    }

    val isTaskMenuExpanded = AllSettings.launcherTaskMenuExpanded.state

    fun changeTasksExpandedState() {
        AllSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    val mainScreenKey = screenBackStackModel.mainScreen.currentKey
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain

    val isBackgroundValid = LocalBackgroundViewModel.current?.isValid == true
    val launcherBackgroundOpacity = AllSettings.launcherBackgroundOpacity.state.toFloat() / 100f

    val backgroundColor = if (isBackgroundValid) {
        backgroundColor().copy(alpha = launcherBackgroundOpacity)
    } else backgroundColor()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isBackgroundValid) Modifier
                else Modifier.background(cmcBackgroundBrush(isLauncherInDarkTheme()))
            ),
        color = if (isBackgroundValid) backgroundColor else Color.Transparent,
        contentColor = onBackgroundColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            TopBar(
                modifier = Modifier.fillMaxWidth(),
                mainScreenKey = mainScreenKey,
                inLauncherScreen = inLauncherScreen,
                hasRunningTasks = hasRunningTasks,
                isTasksExpanded = isTaskMenuExpanded,
                onScreenBack = {
                    onBack(screenBackStackModel.mainScreen.backStack)
                },
                toMainScreen = toMainScreen,
                toSettingsScreen = {
                    screenBackStackModel.mainScreen.removeAndNavigateTo(
                        removes = screenBackStackModel.clearBeforeNavKeys,
                        screenKey = screenBackStackModel.settingsScreen
                    )
                },
                toDownloadScreen = {
                    screenBackStackModel.navigateToDownload()
                },
                toMultiplayerScreen = {
                    screenBackStackModel.mainScreen.removeAndNavigateTo(
                        removes = screenBackStackModel.clearBeforeNavKeys,
                        screenKey = NormalNavKey.Multiplayer
                    )
                },
                openFileManager = {
                    eventViewModel.sendEvent(
                        EventViewModel.Event.OpenFileManager(
                            rootPath = PathManager.DIR_FILES_EXTERNAL.absolutePath
                        )
                    )
                },
                changeExpandedState = {
                    changeTasksExpandedState()
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                NavStateHost(rootKey = screenBackStackModel.mainScreen) {
                    NavigationUI(
                        modifier = Modifier.fillMaxSize(),
                        screenBackStackModel = screenBackStackModel,
                        toMainScreen = toMainScreen,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        modifyVersionViewModel = modifyVersionViewModel,
                        submitError = submitError
                    )
                }

                TaskMenu(
                    isExpanded = isTaskMenuExpanded,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 420.dp)
                        .fillMaxWidth(0.92f)
                        .align(Alignment.CenterStart)
                        .padding(all = 6.dp)
                ) {
                    changeTasksExpandedState()
                }
            }
        }
    }
}

private class LauncherNavigationAction(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val selected: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun TopBar(
    mainScreenKey: TitledNavKey?,
    inLauncherScreen: Boolean,
    hasRunningTasks: Boolean,
    isTasksExpanded: Boolean,
    modifier: Modifier = Modifier,
    onScreenBack: () -> Unit,
    toMainScreen: () -> Unit,
    toSettingsScreen: () -> Unit,
    toDownloadScreen: () -> Unit,
    toMultiplayerScreen: () -> Unit,
    openFileManager: () -> Unit,
    changeExpandedState: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val download = LauncherNavigationAction(
        icon = R.drawable.ic_download_2_filled,
        label = R.string.generic_download,
        selected = mainScreenKey is NestedNavKey.Download,
        onClick = toDownloadScreen
    )
    val otherActions = listOf(
        LauncherNavigationAction(
            icon = R.drawable.ic_group_filled,
            label = R.string.terracotta_terracotta,
            selected = mainScreenKey is NormalNavKey.Multiplayer,
            onClick = toMultiplayerScreen
        ),
        LauncherNavigationAction(
            icon = R.drawable.ic_folder_filled,
            label = R.string.fm_activity_name,
            onClick = openFileManager
        ),
        LauncherNavigationAction(
            icon = R.drawable.ic_settings_filled,
            label = R.string.generic_setting,
            selected = mainScreenKey is NestedNavKey.Settings,
            onClick = toSettingsScreen
        )
    )

    Surface(
        modifier = modifier,
        color = colors.surfaceContainerLow,
        contentColor = colors.onSurface
    ) {
        Column {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 600.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!inLauncherScreen) {
                        ToolbarIcon(
                            icon = R.drawable.ic_arrow_back,
                            label = R.string.generic_back,
                            onClick = { backDispatcher?.onBackPressed() ?: onScreenBack() }
                        )
                        if (!compact) {
                            ToolbarIcon(
                                icon = R.drawable.ic_home_filled,
                                label = R.string.generic_main_menu,
                                onClick = toMainScreen
                            )
                        }
                    }

                    val parentTitle = mainScreenKey?.title
                    val childTitle = (mainScreenKey as? BackStackNavKey<*>)?.currentKey?.title
                    val titleModifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                    val titleStyle = MaterialTheme.typography.titleMedium
                    if (inLauncherScreen) {
                        Row(
                            modifier = titleModifier,
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(34.dp),
                                painter = painterResource(R.drawable.ic_cmc_logo),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                            Text(
                                text = BuildKeys.LAUNCHER_NAME,
                                style = titleStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        val title = when {
                            parentTitle == null -> androidText(BuildKeys.LAUNCHER_NAME)
                            childTitle == null -> parentTitle
                            else -> androidText(parentTitle, androidText(" · "), childTitle)
                        }
                        AndroidStringText(
                            modifier = titleModifier,
                            text = title,
                            style = titleStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (hasRunningTasks) {
                        ToolbarIcon(
                            icon = R.drawable.ic_assignment_filled,
                            label = R.string.main_task_menu,
                            selected = isTasksExpanded,
                            onClick = changeExpandedState
                        )
                    }
                    val toolbarActions = if (compact) listOf(download) else listOf(download) + otherActions
                    toolbarActions.forEach { action ->
                        ToolbarIcon(
                            icon = action.icon,
                            label = action.label,
                            selected = action.selected,
                            onClick = { if (!action.selected) action.onClick() }
                        )
                    }
                    if (compact) {
                        val actions = if (inLauncherScreen) {
                            otherActions
                        } else {
                            listOf(
                                LauncherNavigationAction(
                                    icon = R.drawable.ic_home_filled,
                                    label = R.string.generic_main_menu,
                                    onClick = toMainScreen
                                )
                            ) + otherActions
                        }
                        ToolbarOverflow(actions)
                    }
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
        }
    }
}

@Composable
private fun ToolbarIcon(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = if (selected) Modifier.semantics { this.selected = true } else Modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) colors.primaryContainer else Color.Transparent,
            contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface
        )
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(icon),
            contentDescription = stringResource(label)
        )
    }
}

@Composable
private fun ToolbarOverflow(actions: List<LauncherNavigationAction>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ToolbarIcon(
            icon = R.drawable.ic_more_vert,
            label = R.string.generic_more,
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(action.label)) },
                    leadingIcon = {
                        Icon(painter = painterResource(action.icon), contentDescription = null)
                    },
                    trailingIcon = if (action.selected) {
                        { Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null) }
                    } else {
                        null
                    },
                    modifier = if (action.selected) {
                        Modifier.semantics { selected = true }
                    } else {
                        Modifier
                    },
                    onClick = {
                        expanded = false
                        if (!action.selected) action.onClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {

        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }

        val navigateToExport: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NestedNavKey.VersionExport(version),
                useClassEquality = true
            )
        }

        NestedNavDisplay(
            ownerKey = screenBackStackModel.mainScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    LauncherScreen(
                        backStackViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        onLaunchGame = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.Game(version)
                            )
                        },
                        onQuickPlayServer = { version, address ->
                            val target = (version ?: VersionsManager.currentVersion.value)?.takeIf { it.isValid() }
                            if (target != null) {
                                eventViewModel.sendEvent(
                                    EventViewModel.Event.Launch.PlayServer(target, address)
                                )
                            } else {
                                eventViewModel.sendToast(androidText(R.string.game_launch_no_version))
                                screenBackStackModel.mainScreen.navigateTo(NormalNavKey.VersionsManager)
                            }
                        },
                        onOpenLink = {
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it))
                        },
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.AccountManager> { key ->
                    AccountManageScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        openLink = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(
                        backScreenViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        navigateToExport = navigateToExport,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    VersionSettingsScreen(
                        key = key,
                        modifyViewModel = modifyVersionViewModel,
                        backScreenViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        onExportModpack = {
                            navigateToExport(key.version)
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.VersionExport> { key ->
                    VersionExportScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        backToMainScreen = toMainScreen
                    )
                }
                entry<NestedNavKey.Download> { key ->
                    DownloadScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.AddonInfo> { key ->
                    AddonInfoScreen(
                        key = key,
                        mainScreenKey = screenBackStackModel.mainScreen.currentKey,
                        addonInfoScreenKey = key.currentKey,
                        eventViewModel = eventViewModel,
                        submitError = submitError,
                    )
                }
                entry<NormalNavKey.Multiplayer> {
                    MultiplayerScreen(
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.LogView> { key ->
                    LogViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val show = isExpanded && tasks.isNotEmpty()
    if (show) {
        BackgroundCard(
            modifier = modifier
                .fillMaxSize()
                .padding(all = 6.dp),
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor(),
                contentColor = onBackgroundColor()
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                CardTitleLayout(blur = 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                            onClick = changeExpandedState
                        ) {
                            Icon(
                                modifier = Modifier.size(28.dp),
                                painter = painterResource(R.drawable.ic_arrow_left_rounded),
                                contentDescription = stringResource(R.string.generic_collapse)
                            )
                        }

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val taskProgress by task.progress.collectAsStateWithLifecycle()
                        val taskMessage by task.message.collectAsStateWithLifecycle()
                        val rateBytesPerSec by task.rateBytesPerSec.collectAsStateWithLifecycle()

                        TaskItem(
                            taskProgress = taskProgress,
                            taskMessage = taskMessage,
                            rateBytesPerSec = rateBytesPerSec,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {

                            TaskSystem.cancelTask(task.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    taskProgress: Float,
    taskMessage: AndroidStringText?,
    rateBytesPerSec: Long?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                taskMessage?.let { message ->
                    AndroidStringText(
                        text = message,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (taskProgress < 0) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { taskProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    taskProgress.takeIf { it >= 0f }?.let { progress ->
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    rateBytesPerSec?.let { bytes ->
                        val text = remember(bytes) { "${formatFileSize(bytes)}/s" }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
