package com.cmc.launcher.ui.screens.content.download.game

import androidx.compose.foundation.Image
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.mapExceptionToMessage
import com.cmc.launcher.game.versioninfo.MinecraftVersion
import com.cmc.launcher.game.versioninfo.MinecraftVersions
import com.cmc.launcher.game.versioninfo.models.isType
import com.cmc.launcher.ui.AndroidStringText
import com.cmc.launcher.ui.components.CheckChip
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.utils.formatDate
import com.cmc.launcher.viewmodel.EventViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class VersionFilter(
    val release: Boolean = true, val snapshot: Boolean = false,
    val aprilFools: Boolean = false, val old: Boolean = false, val id: String = ""
)

private class VersionsViewModel : ViewModel() {
    var versions by mutableStateOf<List<MinecraftVersion>>(emptyList())
        private set
    var filter by mutableStateOf(VersionFilter())
        private set
    var loading by mutableStateOf(false)
        private set
    var failure by mutableStateOf<AndroidStringText?>(null)
        private set
    private var loaded: List<MinecraftVersion> = emptyList()
    private var job: Job? = null

    fun filterWith(value: VersionFilter) {
        filter = value
        applyFilter()
    }

    private fun applyFilter() {
        versions = loaded.filter {
            it.isType(filter.release, filter.snapshot, filter.aprilFools, filter.old) &&
                (filter.id.isBlank() || it.version.id.contains(filter.id.trim(), ignoreCase = true))
        }
    }

    fun refresh(force: Boolean = false) {
        if (loading) return
        loading = true
        failure = null
        job = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { MinecraftVersions.refreshVersions(force) }
                loaded = MinecraftVersions.allVersions.value
                applyFilter()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failure = mapExceptionToMessage(error)
            } finally {
                loading = false
            }
        }
    }

    init { refresh() }
    override fun onCleared() { job?.cancel(); super.onCleared() }
}

sealed interface SelectGameVersionHost {
    data class Download(val mainScreenKey: TitledNavKey?, val downloadScreenKey: TitledNavKey?,
        val downloadGameScreenKey: TitledNavKey?) : SelectGameVersionHost
    data class VersionSettings(val mainScreenKey: TitledNavKey?, val versionSettingsScreenKey: TitledNavKey?) : SelectGameVersionHost
}

@Composable
fun SelectGameVersionScreen(host: SelectGameVersionHost, eventViewModel: EventViewModel,
    onVersionSelect: (MinecraftVersion) -> Unit = {}) {
    val model: VersionsViewModel = viewModel(key = NormalNavKey.DownloadGame.SelectGameVersion.toString()) { VersionsViewModel() }
    val active = when (host) {
        is SelectGameVersionHost.Download -> host.mainScreenKey is NestedNavKey.Download &&
            host.downloadScreenKey is NestedNavKey.DownloadGame && host.downloadGameScreenKey == NormalNavKey.DownloadGame.SelectGameVersion
        is SelectGameVersionHost.VersionSettings -> host.mainScreenKey is NestedNavKey.VersionSettings &&
            host.versionSettingsScreenKey == NormalNavKey.Versions.ModifyVersion
    }
    if (!active) return
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            OwnOutlinedTextField(model.filter.id, { model.filterWith(model.filter.copy(id = it)) }, Modifier.weight(1f),
                singleLine = true, label = { Text(stringResource(R.string.generic_search)) })
            IconButton(enabled = !model.loading, onClick = { model.refresh(true) }) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.generic_refresh))
            }
        }
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = Space.md),
            horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            CheckChip(selected = model.filter.release, onClick = { model.filterWith(model.filter.copy(release = !model.filter.release)) },
                label = { Text(stringResource(R.string.download_game_type_release)) })
            CheckChip(selected = model.filter.snapshot, onClick = { model.filterWith(model.filter.copy(snapshot = !model.filter.snapshot)) },
                label = { Text(stringResource(R.string.download_game_type_snapshot)) })
            CheckChip(selected = model.filter.aprilFools, onClick = { model.filterWith(model.filter.copy(aprilFools = !model.filter.aprilFools)) },
                label = { Text(stringResource(R.string.download_game_type_april_fools)) })
            CheckChip(selected = model.filter.old, onClick = { model.filterWith(model.filter.copy(old = !model.filter.old)) },
                label = { Text(stringResource(R.string.download_game_type_old)) })
        }
        HorizontalDivider(Modifier.padding(top = Space.sm))
        val error = model.failure
        when {
            model.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.generic_loading)) }
            error != null -> Column(Modifier.weight(1f).fillMaxWidth().padding(Space.lg), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                AndroidStringText(error)
                TextButton(onClick = { model.refresh(true) }) { Text(stringResource(R.string.generic_retry)) }
            }
            model.versions.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth().padding(Space.lg), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.pe_search_empty))
            }
            else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                items(model.versions, key = { it.version.id }) { version ->
                    GameVersionRow(version, { onVersionSelect(version) }) {
                        eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameVersionRow(version: MinecraftVersion, select: () -> Unit, open: (String) -> Unit) {
    val (icon, type, wiki) = when (version.type) {
        MinecraftVersion.Type.Release -> Triple(R.drawable.img_minecraft, R.string.download_game_type_release, R.string.url_wiki_minecraft_game_release)
        MinecraftVersion.Type.Snapshot -> Triple(R.drawable.img_command_block, R.string.download_game_type_snapshot, R.string.url_wiki_minecraft_game_snapshot)
        MinecraftVersion.Type.AprilFools -> Triple(R.drawable.img_diamond_block, R.string.download_game_type_april_fools, R.string.url_wiki_minecraft_game_snapshot)
        MinecraftVersion.Type.OldBeta -> Triple(R.drawable.img_old_cobblestone, R.string.download_game_type_old_beta, null)
        MinecraftVersion.Type.OldAlpha -> Triple(R.drawable.img_old_grass_block, R.string.download_game_type_old_alpha, null)
        MinecraftVersion.Type.Unknown -> Triple(R.drawable.ic_unknown_icon, R.string.generic_unknown, null)
    }
    val link = wiki?.let { stringResource(it, version.urlSuffix ?: version.version.id) }
    Surface(shape = FieldShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clip(FieldShape).clickable(role = Role.Button, onClick = select)
                .heightIn(min = Size.row).padding(Space.md), horizontalArrangement = Arrangement.spacedBy(Space.md),
                verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(icon), null, Modifier.size(Size.iconXl))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(version.version.id, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(type) + " · " + formatDate(version.version.releaseTime, stringResource(R.string.date_format)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    version.summary?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (link != null) IconButton(onClick = { open(link) }) {
                Icon(painterResource(R.drawable.ic_link), stringResource(R.string.download_addon_wiki_link))
            }
        }
    }
}