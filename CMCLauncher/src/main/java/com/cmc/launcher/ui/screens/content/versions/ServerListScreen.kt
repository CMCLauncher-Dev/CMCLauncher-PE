package com.cmc.launcher.ui.screens.content.versions

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import com.cmc.launcher.ui.components.StaticDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.cmc.launcher.ui.components.StaticProgressIndicator
import com.cmc.launcher.ui.components.StaticBusyIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmc.launcher.ui.components.StaticDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cmc.launcher.R
import com.cmc.launcher.context.COPY_LABEL_SERVER_IP
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.multiplayer.AllServers
import com.cmc.launcher.game.version.multiplayer.ServerData
import com.cmc.launcher.game.version.multiplayer.description.ComponentDescription
import com.cmc.launcher.game.version.multiplayer.description.ComponentDescriptionRoot
import com.cmc.launcher.game.version.multiplayer.description.ServerDescription
import com.cmc.launcher.game.version.multiplayer.description.StringDescription
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.components.CardTitleLayout
import com.cmc.launcher.ui.components.EdgeDirection
import com.cmc.launcher.ui.components.IconTextButton
import com.cmc.launcher.ui.components.ImePanContainer
import com.cmc.launcher.ui.components.MarqueeText
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.components.ScalingLabel
import com.cmc.launcher.ui.components.ShimmerBox
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.components.SimpleTextInputField
import com.cmc.launcher.ui.components.SingleLineTextCheck
import com.cmc.launcher.ui.components.animated
import com.cmc.launcher.ui.components.fadeEdge
import com.cmc.launcher.ui.components.rememberAnimatedContext
import com.cmc.launcher.ui.components.rememberDialogMaxHeight
import com.cmc.launcher.ui.components.verticalScrollWithBar
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.home.server.ServerCardManager
import com.cmc.launcher.ui.screens.content.versions.elements.ComponentText
import com.cmc.launcher.ui.screens.content.versions.elements.MinecraftColorText
import com.cmc.launcher.ui.screens.content.versions.elements.MinecraftColorTextNormal
import com.cmc.launcher.ui.screens.content.versions.layouts.VersionChunkBackground
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.cardColor
import com.cmc.launcher.ui.theme.itemColor
import com.cmc.launcher.ui.theme.onCardColor
import com.cmc.launcher.ui.theme.onItemColor
import com.cmc.launcher.utils.animation.getAnimateTween
import com.cmc.launcher.utils.copyText
import com.cmc.launcher.utils.logging.Logger
import com.cmc.launcher.utils.string.isEmptyOrBlank
import com.cmc.launcher.utils.string.stripColorCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "ServerList"

private sealed interface ServerListOperation {

    data object Loading : ServerListOperation

    data object LoadedData : ServerListOperation
}

private sealed interface ServerDataOperation {
    data object None: ServerDataOperation

    data object AddServer : ServerDataOperation

    data class DeleteServer(val data: ServerData) : ServerDataOperation

    data class EditServer(val data: ServerData) : ServerDataOperation
}

private class ServerListViewModel(
    val gamePath: File,
    val serverData: File
) : ViewModel() {
    var operation by mutableStateOf<ServerListOperation>(ServerListOperation.Loading)

    private val _servers = MutableStateFlow<List<ServerData>?>(emptyList())
    val servers = _servers.asStateFlow()

    private val allServers = AllServers()

    var searchName by mutableStateOf("")

    private val allLoadingServer = mutableMapOf<ServerData, Job>()

    private var refreshJob: Job? = null
    private var saveJobQueue = mutableListOf<Job>()
    private val dataMutex = Mutex()

    private var searchJob: Job? = null

    fun loadServer() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            dataMutex.withLock {
                loadServerSuspend()
            }
        }
    }

    private suspend fun loadServerSuspend() {
        withContext(Dispatchers.Main) {
            operation = ServerListOperation.Loading
            _servers.update { emptyList() }
        }

        allLoadingServer.forEach { (_, job) ->
            job.cancel()
        }
        allLoadingServer.clear()

        allServers.loadServers(serverData)

        withContext(Dispatchers.Main) {
            operation = ServerListOperation.LoadedData
            reloadServerList()
        }
    }

    private suspend fun reloadServerList() {
        withContext(Dispatchers.Main) {
            _servers.update {
                if (allServers.serverList.isEmpty()) {
                    null
                } else {
                    filteredServers()
                }
            }
        }
    }

    var saving by mutableStateOf(false)
        private set

    var dataOperation by mutableStateOf<ServerDataOperation>(ServerDataOperation.None)

    fun addServer(
        serverName: String,
        serverAddress: String
    ) {
        saveServers(
            reason = "added server",
            beforeSave = {
                val data = ServerData(name = serverName, originIp = serverAddress)
                allServers.addServer(data)
            }
        )
    }

    fun deleteServer(
        data: ServerData
    ) {
        saveServers(
            reason = "deleted server",
            beforeSave = {
                allServers.removeServer(data)
                allLoadingServer[data]?.cancel()
                allLoadingServer.remove(data)
            }
        )
    }

    fun editedServer(
        data: ServerData
    ) {
        saveServers(
            reason = "edited server",
            beforeSave = {
                allLoadingServer[data]?.cancel()
                allLoadingServer.remove(data)
            },
            afterSave = {

                loadServer(data, true)
            }
        )
    }

    private fun saveServers(
        reason: String? = null,
        reload: Boolean = true,
        beforeSave: suspend () -> Unit = {},
        afterSave: suspend () -> Unit = {}
    ) {
        val job = viewModelScope.launch(Dispatchers.IO) {
            dataMutex.withLock {
                Logger.info(TAG, "Saving server list, reason = $reason, reload UI? = $reload")

                withContext(Dispatchers.Main) { saving = true }
                beforeSave()
                allServers.save(gamePath)
                if (reload) reloadServerList()
                afterSave()
                withContext(Dispatchers.Main) { saving = false }
            }
        }
        saveJobQueue.add(job)
    }

    private fun filteredServers(): List<ServerData> {
        return allServers.serverList.filter { server ->
            server.name.contains(searchName)
        }
    }

    fun filterServers() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            val servers = filteredServers()
            if (!isActive) return@launch
            _servers.update { servers }
        }
    }

    fun loadServer(
        server: ServerData,
        isRefresh: Boolean = false
    ) {
        if (isRefresh) {
            allLoadingServer[server]?.cancel()
            allLoadingServer.remove(server)
        }

        if (server in allLoadingServer) return

        allLoadingServer[server] = viewModelScope.launch {
            server.load { reason ->
                saveServers(
                    reason = reason,
                    reload = false
                )
            }
        }
    }

    fun copy(
        context: Context,
        ip: String
    ) {
        copyText(label = COPY_LABEL_SERVER_IP, text = ip, context = context)
    }

    init {
        loadServer()
    }

    override fun onCleared() {
        refreshJob?.cancel()
        refreshJob = null
        searchJob?.cancel()
        searchJob = null
        saveJobQueue.forEach { it.cancel() }
        saveJobQueue.clear()
    }
}

@Composable
private fun rememberServerListViewModel(
    gamePath: File,
    serverData: File,
    version: Version
): ServerListViewModel {
    return viewModel(
        key = version.toString() + "_" + "ServerList"
    ) {
        ServerListViewModel(
            gamePath = gamePath,
            serverData = serverData
        )
    }
}

@Composable
private fun ServerDataOperation(
    operation: ServerDataOperation,
    onChange: (ServerDataOperation) -> Unit,
    onAddServer: (name: String, ip: String) -> Unit,
    onEditedServer: (ServerData) -> Unit,
    onDeleteServer: (ServerData) -> Unit
) {
    when (operation) {
        is ServerDataOperation.None -> {}
        is ServerDataOperation.AddServer -> {
            ServerEditDialog(
                title = stringResource(R.string.servers_list_add_server),
                name = null,
                address = "",
                onDismissRequest = {
                    onChange(ServerDataOperation.None)
                },
                onApply = onAddServer
            )
        }
        is ServerDataOperation.DeleteServer -> {
            val serverName = remember(operation) {
                operation.data.name.stripColorCodes()
            }

            SimpleAlertDialog(
                title = stringResource(R.string.servers_list_delete_server),
                text = stringResource(R.string.servers_list_delete_server_text, serverName),
                onDismiss = {
                    onChange(ServerDataOperation.None)
                },
                onConfirm = {
                    onDeleteServer(operation.data)
                    onChange(ServerDataOperation.None)
                }
            )
        }
        is ServerDataOperation.EditServer -> {
            val data = operation.data
            ServerEditDialog(
                title = stringResource(R.string.servers_list_edit_server),
                name = data.name,
                address = data.originIp,
                onDismissRequest = {
                    onChange(ServerDataOperation.None)
                },
                onApply = { name, ip ->
                    data.name = name
                    data.originIp = ip
                    onEditedServer(data)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServerListScreen(
    mainScreenKey: TitledNavKey?,
    versionsScreenKey: TitledNavKey?,
    version: Version,
    onQuickPlay: (Version, String) -> Unit,
    backToMainScreen: () -> Unit,
) {
    if (!version.isValid()) {
        backToMainScreen()
        return
    }

    val dataFile = remember(version) {
        File(version.getGameDir(), "servers.dat")
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.ServerList, versionsScreenKey, false),
    ) { isVisible ->
        val context = LocalContext.current

        val viewModel = rememberServerListViewModel(
            gamePath = version.getGameDir(),
            serverData = dataFile,
            version = version
        )

        val ctx = rememberAnimatedContext(isVisible = isVisible)

        ServerDataOperation(
            operation = viewModel.dataOperation,
            onChange = { viewModel.dataOperation = it },
            onAddServer = { name, ip ->
                viewModel.addServer(
                    serverName = name,
                    serverAddress = ip
                )
            },
            onEditedServer = { data ->
                viewModel.editedServer(
                    data = data
                )
            },
            onDeleteServer = { server ->
                viewModel.deleteServer(
                    data = server
                )
            }
        )

        VersionChunkBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
                .animated(ctx),
            paddingValues = PaddingValues()
        ) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = false,
                enabled = viewModel.operation !is ServerListOperation.Loading,
                onRefresh = viewModel::loadServer,
            ) {
                when (viewModel.operation) {
                    is ServerListOperation.Loading -> {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            StaticBusyIndicator()
                        }
                    }
                    is ServerListOperation.LoadedData -> {
                        val servers by viewModel.servers.collectAsStateWithLifecycle()

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ServerListHeader(
                                searchName = viewModel.searchName,
                                onSearchNameChange = {
                                    viewModel.searchName = it
                                    viewModel.filterServers()
                                },
                                onAddServer = {
                                    viewModel.dataOperation = ServerDataOperation.AddServer
                                },
                                refreshServers = {
                                    viewModel.loadServer()
                                }
                            )

                            ServerListBody(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                servers = servers,
                                isSavingServer = viewModel.saving,
                                onLoad = { viewModel.loadServer(it) },
                                onRefresh = { viewModel.loadServer(it, true) },
                                onCopy = { viewModel.copy(context, it) },
                                onPlay = { address ->
                                    onQuickPlay(version, address)
                                },
                                onEdit = { data ->
                                    viewModel.dataOperation = ServerDataOperation.EditServer(data)
                                },
                                onDelete = { data ->
                                    viewModel.dataOperation = ServerDataOperation.DeleteServer(data)
                                },
                                onAddToHome = { server ->
                                    ServerCardManager.addCard(
                                        name = server.name,
                                        serverIp = server.originIp,
                                        icon = server.icon
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerListHeader(
    searchName: String,
    onSearchNameChange: (String) -> Unit,
    onAddServer: () -> Unit,
    refreshServers: () -> Unit,
    modifier: Modifier = Modifier,
    inputFieldColor: Color = itemColor(),
    inputFieldContentColor: Color = onItemColor()
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchName,
                onValueChange = onSearchNameChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.generic_search)) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = FieldShape,
                singleLine = true
            )
            IconButton(onClick = refreshServers) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.generic_refresh))
            }
        }
        TextButton(onClick = onAddServer, modifier = Modifier.padding(horizontal = Space.sm)) {
            Icon(painterResource(R.drawable.ic_add), null, Modifier.size(Size.iconSm))
            Spacer(Modifier.width(Space.sm))
            Text(stringResource(R.string.servers_list_add_server))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ServerListBody(
    servers: List<ServerData>?,
    isSavingServer: Boolean,
    onLoad: (ServerData) -> Unit,
    onRefresh: (ServerData) -> Unit,
    onCopy: (String) -> Unit,
    onPlay: (String) -> Unit,
    onEdit: (ServerData) -> Unit,
    onDelete: (ServerData) -> Unit,
    onAddToHome: (ServerData) -> Unit,
    modifier: Modifier = Modifier,
) {
    servers?.let { list ->
        if (list.isNotEmpty()) {
            val scrollState = rememberLazyListState()
            LazyColumn(
                modifier = modifier.nonInteractiveScrollbar(
                    state = scrollState.scrollIndicatorState!!,
                    orientation = Orientation.Vertical,
                ),
                contentPadding = PaddingValues(all = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                state = scrollState,
            ) {
                items(list) { server ->
                    ServerItem(
                        modifier = Modifier.fillMaxWidth(),
                        item = server,
                        isSavingServer = isSavingServer,
                        onLoad = { onLoad(server) },
                        onRefresh = { onRefresh(server) },
                        onCopy = { onCopy(server.originIp) },
                        onPlay = { onPlay(server.originIp) },
                        onEdit = { onEdit(server) },
                        onDelete = { onDelete(server) },
                        onAddToHome = { onAddToHome(server) }
                    )
                }
            }
        } else {

            Box(modifier = Modifier.fillMaxSize()) {
                ScalingLabel(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.generic_no_matching_items)
                )
            }
        }
    } ?: run {

        Box(modifier = Modifier.fillMaxSize()) {
            ScalingLabel(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.servers_list_no_servers)
            )
        }
    }
}

@Composable
private fun ServerItem(
    item: ServerData,
    isSavingServer: Boolean,
    onLoad: () -> Unit,
    onRefresh: () -> Unit,
    onCopy: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = item.operation
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember(item.originIp) { mutableStateOf(false) }
    val cards by ServerCardManager.cards.collectAsStateWithLifecycle()
    val pinned = cards.any { it.record.cardId == item.originIp }
    LaunchedEffect(item) { onLoad() }
    Surface(modifier, shape = FieldShape, color = itemColor(), contentColor = onItemColor()) {
        Column(Modifier.fillMaxWidth().padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                ServerIcon(server = item, size = 40.dp, modifier = Modifier.clip(FieldShape))
                Column(Modifier.weight(1f)) {
                    MinecraftColorTextNormal(inputText = item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text(item.originIp, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.generic_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.generic_refresh)) },
                            enabled = status !is ServerData.Operation.Loading, onClick = { menuOpen = false; onRefresh() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.servers_list_copy_server_address)) },
                            onClick = { menuOpen = false; onCopy() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.home_add_version_card)) },
                            enabled = !pinned, onClick = { menuOpen = false; onAddToHome() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.servers_list_edit_server)) },
                            enabled = !isSavingServer, onClick = { menuOpen = false; onEdit() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.servers_list_delete_server), color = colors.error) },
                            enabled = !isSavingServer, onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
            when (status) {
                ServerData.Operation.Loading -> Text(stringResource(R.string.generic_loading),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                ServerData.Operation.Failed -> Text(stringResource(R.string.servers_list_failed_to_connect),
                    style = MaterialTheme.typography.bodySmall, color = colors.error)
                is ServerData.Operation.Loaded -> status.result.status.description?.let {
                    DescriptionTextRender(description = it, fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        maxLines = 3, softWrap = true)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (status is ServerData.Operation.Loaded) {
                        val players = status.result.status.players
                        val playerText = when {
                            players.online < 0 -> stringResource(R.string.servers_list_undefined)
                            players.max <= 0 -> players.online.toString()
                            else -> "${players.online}/${players.max}"
                        }
                        Text("${status.result.pingMs} ms · $playerText", style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onPlay) {
                    Icon(painterResource(R.drawable.ic_play_arrow_filled), null, Modifier.size(Size.iconSm))
                    Spacer(Modifier.width(Space.xs))
                    Text(stringResource(R.string.main_launch_game))
                }
            }
        }
    }
}

@Composable
fun ServerSignalIcon(
    modifier: Modifier = Modifier,
    signalStrength: Int,
    mainColor: Color = MaterialTheme.colorScheme.primary,
    otherColor: Color = MaterialTheme.colorScheme.background,
    contentPadding: PaddingValues = PaddingValues(vertical = 2.dp)
) {
    require(signalStrength in 1..5) {
        "signalStrength must be between 1 and 5"
    }

    Canvas(modifier = modifier.padding(contentPadding)) {
        val barCount = 5
        val barWidth = size.width / (barCount * 2 - 1)
        val spacing = barWidth

        for (i in 0 until barCount) {
            val barHeight = size.height * (i + 1) / barCount
            val xOffset = i * (barWidth + spacing)
            val yOffset = size.height - barHeight

            drawRect(
                color = if (i < signalStrength) mainColor else otherColor,
                topLeft = Offset(xOffset, yOffset),
                size = GeometrySize(barWidth, barHeight)
            )
        }
    }
}

@Composable
private fun ServerIcon(
    server: ServerData,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val sizeModifier = modifier.size(size)

    if (server.icon == null) {
        Image(
            modifier = sizeModifier,
            painter = painterResource(R.drawable.ic_unknown_icon),
            contentDescription = null
        )
        return
    }

    val context = LocalContext.current

    val density = LocalDensity.current
    val pxSize = with(density) { size.roundToPx() }

    val imageRequest = remember(server, server.refreshUI, pxSize) {
        ImageRequest.Builder(context)
            .data(server.icon)
            .size(pxSize)
            .crossfade(false)
            .build()
    }

    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        placeholder = null,
        error = painterResource(R.drawable.ic_unknown_icon)
    )

    val state by painter.state.collectAsStateWithLifecycle()

    when (state) {
        AsyncImagePainter.State.Empty -> {
            Box(modifier = sizeModifier)
        }
        is AsyncImagePainter.State.Loading -> {
            ShimmerBox(
                modifier = sizeModifier
            )
        }
        is AsyncImagePainter.State.Error,
        is AsyncImagePainter.State.Success -> {
            Image(
                painter = painter,
                contentDescription = null,
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                modifier = sizeModifier,
            )
        }
    }
}

@Composable
fun DescriptionTextRender(
    description: ServerDescription,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = false,
) {
    when(description) {
        is ComponentDescriptionRoot -> {
            ComponentText(
                modifier = modifier,
                descriptions = description.values,
                fontSize = fontSize,
                maxLines = maxLines,
                softWrap = softWrap
            )
        }
        is ComponentDescription -> {
            ComponentText(
                modifier = modifier,
                descriptions = listOf(description),
                fontSize = fontSize,
                maxLines = maxLines,
                softWrap = softWrap
            )
        }
        is StringDescription -> {
            val value = remember(description) { description.value }
            MinecraftColorText(
                modifier = modifier,
                inputText = value,
                fontSize = fontSize,
                maxLines = maxLines,
                softWrap = softWrap
            )
        }
    }
}

@Composable
private fun ServerEditDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onApply: (name: String, ip: String) -> Unit,
    name: String? = null,
    address: String = "",
) {

    val defaultName = stringResource(R.string.servers_list_add_server_default_name)
    var name by remember { mutableStateOf(name?.takeIf { it.isNotEmpty() } ?: defaultName) }
    var ip by remember { mutableStateOf(address) }

    val isIpEmpty = remember(ip) {
        ip.isEmptyOrBlank()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        ImePanContainer(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.size(16.dp))

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .weight(1f, fill = false)
                            .verticalScrollWithBar(state = scrollState)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OwnOutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = name,
                            onValueChange = {
                                name = it
                            },
                            label = { Text(text = stringResource(R.string.servers_list_add_server_name)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )

                        Spacer(modifier = Modifier.size(8.dp))

                        SingleLineTextCheck(
                            text = ip,
                            onSingleLined = { ip = it }
                        )

                        OwnOutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = ip,
                            onValueChange = {
                                ip = it
                            },
                            isError = isIpEmpty,
                            label = { Text(text = stringResource(R.string.servers_list_add_server_ip)) },
                            supportingText = {
                                if (isIpEmpty) {
                                    Text(text = stringResource(R.string.generic_cannot_empty))
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismissRequest
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!isIpEmpty) {
                                    val name0 = name.ifEmpty { defaultName }

                                    onApply(name0, ip)
                                    onDismissRequest()
                                }
                            }
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_confirm))
                        }
                    }
                }
            }
        }
    }
}
