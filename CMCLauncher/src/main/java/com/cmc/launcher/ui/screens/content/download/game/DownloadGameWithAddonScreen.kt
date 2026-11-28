package com.cmc.launcher.ui.screens.content.download.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.launcher.R
import com.cmc.launcher.game.addons.modloader.ModLoader
import com.cmc.launcher.game.addons.modloader.fabriclike.fabric.FabricAPIVersions
import com.cmc.launcher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.cmc.launcher.game.addons.modloader.fabriclike.legacyfabric.LegacyFabricAPIVersions
import com.cmc.launcher.game.addons.modloader.fabriclike.legacyfabric.LegacyFabricVersions
import com.cmc.launcher.game.addons.modloader.fabriclike.quilt.QuiltAPIVersions
import com.cmc.launcher.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.cmc.launcher.game.addons.modloader.forgelike.forge.ForgeVersion
import com.cmc.launcher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.cmc.launcher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.cmc.launcher.game.addons.modloader.optifine.OptiFineVersion
import com.cmc.launcher.game.addons.modloader.optifine.OptiFineVersions
import com.cmc.launcher.game.download.game.GameDownloadInfo
import com.cmc.launcher.game.version.installed.VersionsManager.isVersionExists
import com.cmc.launcher.game.versioninfo.MinecraftVersion
import com.cmc.launcher.ui.components.verticalScrollWithBar
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.elements.getGameVersionIconRes
import com.cmc.launcher.ui.screens.content.elements.isFilenameInvalid
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private class AddonsViewModel(
    private val gameVersion: String,
    loaderSupports: LoaderVerSupports
) : ViewModel() {
    val addonList = AddonList()
    val currentAddon = CurrentAddon()
    var refreshIcon by mutableStateOf(false)
        private set

    fun refreshIcon() {
        refreshIcon = !refreshIcon
    }

    fun reloadOptiFine() = launchAddonReload(
        { currentAddon.optifineState = it },
        { OptiFineVersions.fetchOptiFineList(gameVersion = gameVersion) },
        { addonList.optifineList = it }
    )

    fun reloadForge() = launchAddonReload(
        { currentAddon.forgeState = it },
        { ForgeVersions.fetchForgeList(gameVersion) },
        { addonList.forgeList = it }
    )

    fun reloadNeoForge() = launchAddonReload(
        { currentAddon.neoforgeState = it },
        { NeoForgeVersions.fetchNeoForgeList(gameVersion = gameVersion) },
        { addonList.neoforgeList = it }
    )

    fun reloadFabric() = launchAddonReload(
        { currentAddon.fabricState = it },
        { FabricVersions.fetchFabricLoaderList(gameVersion) },
        { addonList.fabricList = it }
    )

    fun reloadFabricAPI() = launchAddonReload(
        { currentAddon.fabricAPIState = it },
        { FabricAPIVersions.fetchVersionList(gameVersion) },
        {
            addonList.fabricAPIList = it

            if (currentAddon.fabricVersion.value != null) {

                currentAddon.fabricAPIVersion.value = it?.firstOrNull()
            }
        }
    )

    fun reloadLegacyFabric() = launchAddonReload(
        { currentAddon.legacyFabricState = it },
        { LegacyFabricVersions.fetchFabricLoaderList(gameVersion) },
        { addonList.legacyFabricList = it }
    )

    fun reloadLegacyFabricAPI() = launchAddonReload(
        { currentAddon.legacyFabricAPIState = it },
        { LegacyFabricAPIVersions.fetchVersionList(gameVersion) },
        {
            addonList.legacyFabricAPIList = it

            if (currentAddon.legacyFabricVersion.value != null) {

                currentAddon.legacyFabricAPIVersion.value = it?.firstOrNull()
            }
        }
    )

    fun reloadQuilt() = launchAddonReload(
        { currentAddon.quiltState = it },
        { QuiltVersions.fetchQuiltLoaderList(gameVersion) },
        { addonList.quiltList = it }
    )

    fun reloadQuiltAPI() = launchAddonReload(
        { currentAddon.quiltAPIState = it },
        { QuiltAPIVersions.fetchVersionList(gameVersion) },
        {
            addonList.quiltAPIList = it

            if (currentAddon.quiltVersion.value != null) {

                currentAddon.quiltAPIVersion.value = it?.firstOrNull()
            }
        }
    )

    private fun <T> launchAddonReload(
        updateState: (AddonState) -> Unit,
        fetch: suspend () -> T?,
        onSuccess: (T?) -> Unit
    ) {
        viewModelScope.launch {
            runWithState(updateState, fetch).also(onSuccess)
        }
    }

    init {
        reloadOptiFine()
        reloadForge()
        if (loaderSupports.isNeoForgeSupports) {
            reloadNeoForge()
        }
        if (loaderSupports.isFabricSupports) {
            reloadFabric()
            reloadFabricAPI()
        }
        if (loaderSupports.isLegacyFabricSupports) {
            reloadLegacyFabric()
            reloadLegacyFabricAPI()
        }
        if (loaderSupports.isQuiltSupports) {
            reloadQuilt()
            reloadQuiltAPI()
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

@Composable
fun DownloadGameWithAddonScreen(
    mainScreenKey: TitledNavKey?, downloadScreenKey: TitledNavKey?, downloadGameScreenKey: TitledNavKey?,
    key: NormalNavKey.DownloadGame.Addons, refreshErrorCheck: Any? = null, onInstall: (GameDownloadInfo) -> Unit = {}
) {
    val support = rememberLoaderVerSupports(key.gameVersion)
    val model: AddonsViewModel = viewModel(key = key.toString() + "_" + support) { AddonsViewModel(key.gameVersion, support) }
    if (mainScreenKey !is NestedNavKey.Download || downloadScreenKey !is NestedNavKey.DownloadGame || downloadGameScreenKey != key) return
    val selected = model.currentAddon
    val choices = model.addonList
    var name by rememberSaveable(key.gameVersion) { mutableStateOf(key.gameVersion) }
    var customName by rememberSaveable(key.gameVersion) { mutableStateOf(false) }
    AutoChangeVersionName(key.gameVersion, selected, customName) { name = it }
    val invalidName = isFilenameInvalid(name)
    val exists = remember(name, refreshErrorCheck) { isVersionExists(name, true) }
    val error = when {
        name.isBlank() -> stringResource(R.string.generic_cannot_empty)
        invalidName != null -> invalidName
        exists -> stringResource(R.string.versions_manage_install_exists)
        else -> null
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(Space.lg), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            VersionIconPreview(Modifier.size(Size.iconXl), selected, key.type, model.refreshIcon)
            Column(Modifier.weight(1f)) {
                Text(key.gameVersion, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.pe_install_components), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
        Column(Modifier.weight(1f).fillMaxWidth().verticalScrollWithBar(rememberScrollState()).padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                OwnOutlinedTextField(value = name, onValueChange = { name = it; customName = true }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.download_game_version_name)) },
                    isError = error != null, supportingText = if (error == null) null else ({ Text(error) }))
                if (customName) TextButton(onClick = { customName = false; name = key.gameVersion }) {
                    Text(stringResource(R.string.pe_install_auto_name))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                OptiFineList(currentAddon = selected, addonList = choices, onValueChanged = { model.refreshIcon() }, onReload = model::reloadOptiFine)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                ForgeList(currentAddon = selected, addonList = choices, onValueChanged = { model.refreshIcon() }, onReload = model::reloadForge)
            }
            if (support.isNeoForgeSupports) Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                NeoForgeList(currentAddon = selected, addonList = choices, onValueChanged = { model.refreshIcon() }, onReload = model::reloadNeoForge)
            }
            if (support.isFabricSupports) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    if (selected.fabricVersion.value != null && selected.fabricAPIState == AddonState.None &&
                        !choices.fabricAPIList.isNullOrEmpty() && selected.fabricAPIVersion.value == null) {
                        AddonWarningItem(text = stringResource(R.string.download_game_addon_warning_api, ModLoader.FABRIC_API.displayName))
                    }
                    FabricList(currentAddon = selected, addonList = choices, onValueChanged = { loader ->
                        model.refreshIcon()
                        if (loader != null && selected.fabricAPIVersion.value == null) selected.fabricAPIVersion.value = choices.fabricAPIList?.firstOrNull()
                    }, onReload = model::reloadFabric)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    FabricAPIList(currentAddon = selected, addonList = choices, onValueChanged = { model.refreshIcon() }, onReload = model::reloadFabricAPI)
                }
            }
            if (support.isLegacyFabricSupports) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    if (selected.legacyFabricVersion.value != null && selected.legacyFabricAPIState == AddonState.None &&
                        !choices.legacyFabricAPIList.isNullOrEmpty() && selected.legacyFabricAPIVersion.value == null) {
                        AddonWarningItem(text = stringResource(R.string.download_game_addon_warning_api, ModLoader.LEGACY_FABRIC_API.displayName))
                    }
                    LegacyFabricList(currentAddon = selected, addonList = choices, onValueChanged = { loader ->
                        model.refreshIcon()
                        if (loader != null && selected.legacyFabricAPIVersion.value == null) selected.legacyFabricAPIVersion.value = choices.legacyFabricAPIList?.firstOrNull()
                    }, onReload = model::reloadLegacyFabric)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    LegacyFabricAPIList(currentAddon = selected, addonList = choices, onValueChanged = { model.refreshIcon() }, onReload = model::reloadLegacyFabricAPI)
                }
            }
            if (support.isQuiltSupports) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    if (selected.quiltVersion.value != null && selected.quiltAPIState == AddonState.None &&
                        !choices.quiltAPIList.isNullOrEmpty() && selected.quiltAPIVersion.value == null) {
                        AddonWarningItem(text = stringResource(R.string.download_game_addon_warning_api, ModLoader.QUILT_API.displayName))
                    }
                    QuiltList(currentAddon = selected, addonList = choices, onValueChanged = { loader ->
                        model.refreshIcon()
                        if (loader != null && selected.quiltAPIVersion.value == null) selected.quiltAPIVersion.value = choices.quiltAPIList?.firstOrNull()
                    }, onReload = model::reloadQuilt)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    QuiltAPIList(currentAddon = selected, addonList = choices, onValueChanged = { model.refreshIcon() }, onReload = model::reloadQuiltAPI)
                }
            }
        }
        HorizontalDivider()
        Button(enabled = error == null, modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm)
            .heightIn(min = Size.touch), onClick = {
            if (error == null) onInstall(GameDownloadInfo(
                gameVersion = key.gameVersion, customVersionName = name,
                optifine = selected.optifineVersion.value, forge = selected.forgeVersion.value,
                neoforge = selected.neoforgeVersion.value.takeIf { support.isNeoForgeSupports },
                fabric = selected.fabricVersion.value.takeIf { support.isFabricSupports },
                fabricAPI = selected.fabricAPIVersion.value.takeIf { support.isFabricSupports && selected.fabricVersion.value != null },
                legacyFabric = selected.legacyFabricVersion.value.takeIf { support.isLegacyFabricSupports },
                legacyFabricAPI = selected.legacyFabricAPIVersion.value.takeIf { support.isLegacyFabricSupports && selected.legacyFabricVersion.value != null },
                quilt = selected.quiltVersion.value.takeIf { support.isQuiltSupports },
                quiltAPI = selected.quiltAPIVersion.value.takeIf { support.isQuiltSupports && selected.quiltVersion.value != null }
            ))
        }) { Text(stringResource(R.string.download_install)) }
    }
}

@Composable
private fun VersionIconPreview(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    gameType: MinecraftVersion.Type,
    refreshIcon: Any? = null
) {
    val iconRes = remember(refreshIcon, gameType) {
        when {
            currentAddon.optifineVersion.value != null && currentAddon.forgeVersion.value != null -> R.drawable.img_anvil
            currentAddon.optifineVersion.value != null -> R.drawable.img_loader_optifine
            currentAddon.forgeVersion.value != null -> R.drawable.img_anvil
            currentAddon.neoforgeVersion.value != null -> R.drawable.img_loader_neoforge
            currentAddon.fabricVersion.value != null -> R.drawable.img_loader_fabric
            currentAddon.legacyFabricVersion.value != null -> R.drawable.img_loader_legacy_fabric
            currentAddon.quiltVersion.value != null -> R.drawable.img_loader_quilt
            else -> getGameVersionIconRes(gameType)
        }
    }

    Image(
        modifier = modifier,
        painter = painterResource(id = iconRes),
        contentDescription = null
    )
}

@Composable
private fun AutoChangeVersionName(
    gameVersion: String,
    currentAddon: CurrentAddon,
    editedByUser: Boolean,
    changeValue: (String) -> Unit = {}
) {
    LaunchedEffect(
        gameVersion,
        editedByUser,
        currentAddon.optifineVersion.value,
        currentAddon.forgeVersion.value,
        currentAddon.neoforgeVersion.value,
        currentAddon.fabricVersion.value,
        currentAddon.legacyFabricVersion.value,
        currentAddon.quiltVersion.value,
    ) {
        if (editedByUser) return@LaunchedEffect

        fun formatModloader(name: String, version: String) = "$name $version"
        fun formatOptiFine(optifine: OptiFineVersion) = formatModloader(ModLoader.OPTIFINE.displayName, optifine.realVersion)
        fun formatForge(forge: ForgeVersion) = formatModloader(ModLoader.FORGE.displayName, forge.versionName)

        val modloaderValue = buildString {
            with(currentAddon) {
                when {
                    optifineVersion.value != null && forgeVersion.value != null -> {
                        append(formatForge(forgeVersion.value!!))
                        append('-')
                        append(formatOptiFine(optifineVersion.value!!))
                    }
                    optifineVersion.value != null -> append(formatOptiFine(optifineVersion.value!!))
                    forgeVersion.value != null -> append(formatForge(forgeVersion.value!!))
                    neoforgeVersion.value != null -> append(formatModloader(ModLoader.NEOFORGE.displayName, neoforgeVersion.value!!.versionName))
                    fabricVersion.value != null -> append(formatModloader(ModLoader.FABRIC.displayName, fabricVersion.value!!.version))
                    legacyFabricVersion.value != null -> append(formatModloader(ModLoader.LEGACY_FABRIC.displayName, legacyFabricVersion.value!!.version))
                    quiltVersion.value != null -> append(formatModloader(ModLoader.QUILT.displayName, quiltVersion.value!!.version))
                    else -> {
                        changeValue(gameVersion)
                        return@LaunchedEffect
                    }
                }
            }
        }

        changeValue("$gameVersion $modloaderValue")
    }
}
