package com.cmc.launcher.ui.screens.content.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

@Composable
internal fun HomeEntry(
    title: String,
    detail: String,
    enabled: Boolean,
    icon: @Composable (Dp) -> Unit,
    onOpen: (() -> Unit)? = null,
    onLaunch: (() -> Unit)? = null,
    summary: (@Composable () -> Unit)? = null
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(Space.md)) {
        val tall = maxHeight >= 120.dp
        val roomy = maxWidth >= 220.dp
        val iconSize = if (tall) 40.dp else 28.dp
        val compact = maxHeight < 64.dp
        val showLaunchText = roomy && maxHeight >= 48.dp
        val launchHeight = maxHeight.coerceAtMost(Size.touch)
        if (tall) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                EntryIdentity(title, detail, iconSize, icon, onOpen, enabled, Modifier.fillMaxWidth())
                Box(Modifier.weight(1f).fillMaxWidth()) { summary?.invoke() }
                if (onLaunch != null) EntryLaunch(enabled, true, onLaunch, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically) {
                EntryIdentity(title, if (compact) "" else detail, iconSize, icon, onOpen, enabled, Modifier.weight(1f), if (compact) 1 else 2)
                if (onLaunch != null) EntryLaunch(enabled, showLaunchText, onLaunch, minimumHeight = launchHeight)
            }
        }
    }
}

@Composable
private fun EntryIdentity(
    title: String, detail: String, iconSize: Dp, icon: @Composable (Dp) -> Unit,
    onOpen: (() -> Unit)?, enabled: Boolean, modifier: Modifier, titleLines: Int = 2
) {
    val currentOpen by rememberUpdatedState(onOpen)
    val currentEnabled by rememberUpdatedState(enabled)
    val openLabel = stringResource(R.string.generic_setting)
    Row(
        modifier.then(if (onOpen != null) Modifier
            .semantics(mergeDescendants = true) {
                onClick(label = openLabel) {
                    if (currentEnabled) currentOpen?.invoke()
                    currentEnabled
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.isConsumed) return@awaitEachGesture
                    val start = down.position
                    var tapped = false
                    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed || (change.position - start).getDistance() > viewConfiguration.touchSlop) break
                            if (!change.pressed) { tapped = true; break }
                        }
                    }
                    if (tapped && currentEnabled) currentOpen?.invoke()
                }
            } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically
    ) {
        icon(iconSize)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = titleLines, overflow = TextOverflow.Ellipsis)
            if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EntryLaunch(enabled: Boolean, showText: Boolean, launch: () -> Unit, modifier: Modifier = Modifier, minimumHeight: Dp = Size.touch) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.clip(FieldShape).background(if (enabled) colors.primary else colors.surfaceContainerHighest)
            .clickable(enabled = enabled, interactionSource = null, indication = null, role = Role.Button, onClick = launch)
            .heightIn(min = minimumHeight).padding(horizontal = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_play_arrow_filled),
            if (showText) null else stringResource(R.string.main_launch_game), Modifier.size(Size.iconSm),
            tint = if (enabled) colors.onPrimary else colors.onSurfaceVariant)
        if (showText) Text(stringResource(R.string.main_launch_game), style = MaterialTheme.typography.labelLarge,
            maxLines = 1, color = if (enabled) colors.onPrimary else colors.onSurfaceVariant)
    }
}
