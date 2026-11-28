package com.cmc.launcher.ui.screens.content.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class LaunchSelectionViewModel : ViewModel() {
    var selecting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun clearError() { error = null }

    fun select(version: Version) {
        if (selecting || version == VersionsManager.currentVersion.value) return
        selecting = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { VersionsManager.saveVersion(version) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.localizedMessage ?: failure.toString()
            } finally {
                selecting = false
            }
        }
    }
}
