package com.cmc.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey

class ScreenBackStackViewModel : ViewModel() {

    val mainScreen = NestedNavKey.Main()

    val settingsScreen = NestedNavKey.Settings()

    val downloadScreen = NestedNavKey.Download()

    var downloadHubVisible by mutableStateOf(true)

    val downloadGameScreen = NestedNavKey.DownloadGame()

    val downloadModPackScreen = NestedNavKey.DownloadModPack()

    val downloadModScreen = NestedNavKey.DownloadMod()

    val downloadResourcePackScreen = NestedNavKey.DownloadResourcePack()

    val downloadSavesScreen = NestedNavKey.DownloadSaves()

    val downloadShadersScreen = NestedNavKey.DownloadShaders()

    val downloadFavoritesScreen = NestedNavKey.DownloadFavorites()

    val clearBeforeNavKeys = listOf(
        settingsScreen::class,
        downloadScreen::class,
        NormalNavKey.Toolbox::class,
        NormalNavKey.Multiplayer::class
    )
}
