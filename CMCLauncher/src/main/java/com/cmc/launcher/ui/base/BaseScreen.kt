package com.cmc.launcher.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.cmc.launcher.ui.screens.TitledNavKey

@Composable
fun BaseScreen(
    screenKey: TitledNavKey,
    currentKey: TitledNavKey?,
    useClassEquality: Boolean = false,
    content: @Composable (isVisible: Boolean) -> Unit
) {
    ScreenContent(isTagVisible(screenKey, currentKey, useClassEquality), content)
}

@Composable
fun BaseScreen(
    vararg levels: Triple<TitledNavKey, TitledNavKey?, Boolean>,
    content: @Composable (isVisible: Boolean) -> Unit
) {
    ScreenContent(levels.all { (key, current, byClass) -> isTagVisible(key, current, byClass) }, content)
}

@Composable
fun BaseScreen(
    levels1: List<Pair<Class<out TitledNavKey>, TitledNavKey?>>,
    vararg levels2: Triple<TitledNavKey, TitledNavKey?, Boolean>,
    content: @Composable (isVisible: Boolean) -> Unit
) {
    val visible = levels1.all { (type, current) -> type.isInstance(current) } &&
        levels2.all { (key, current, byClass) -> isTagVisible(key, current, byClass) }
    ScreenContent(visible, content)
}

@Composable
private fun ScreenContent(visible: Boolean, content: @Composable (Boolean) -> Unit) {
    if (visible) Box(Modifier.fillMaxSize().clipToBounds()) { content(true) }
}

private fun isTagVisible(key: TitledNavKey, current: TitledNavKey?, byClass: Boolean): Boolean = when {
    current == null -> false
    byClass -> key::class == current::class
    else -> key == current
}