package com.cmc.launcher.ui.screens.content.home.server

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import com.cmc.launcher.ui.components.StaticBusyIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import com.cmc.launcher.ui.components.StaticProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmc.launcher.ui.components.StaticDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.movtery.cardgrid.model.CardSize
import com.movtery.cardgrid.model.CardSizeClass
import com.movtery.cardgrid.model.CardState
import com.movtery.cardgrid.model.CardInteraction
import androidx.compose.runtime.saveable.rememberSaveable
import com.cmc.launcher.ui.screens.content.home.HomeEntry
import com.cmc.launcher.R
import com.cmc.launcher.context.COPY_LABEL_SERVER_IP
import com.cmc.launcher.game.path.getGameHome
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.game.version.multiplayer.Players
import com.cmc.launcher.game.version.multiplayer.description.ComponentDescription
import com.cmc.launcher.game.version.multiplayer.description.ComponentDescriptionRoot
import com.cmc.launcher.game.version.multiplayer.description.ServerDescription
import com.cmc.launcher.game.version.multiplayer.description.StringDescription
import com.cmc.launcher.ui.components.ImePanContainer
import com.cmc.launcher.ui.components.LittleTextLabel
import com.cmc.launcher.ui.components.MarqueeText
import com.cmc.launcher.ui.components.MenuButtonLayout
import com.cmc.launcher.ui.components.MenuSwitchButton
import com.cmc.launcher.ui.components.MenuTextButton
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.components.ShimmerBox
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.components.fadeEdge
import com.cmc.launcher.ui.components.rememberDialogMaxHeight
import com.cmc.launcher.ui.components.verticalScrollWithBar
import com.cmc.launcher.ui.screens.content.elements.DisabledAlpha
import com.cmc.launcher.ui.screens.content.elements.VersionIconImage
import com.cmc.launcher.ui.screens.content.home.CardIconShape
import com.cmc.launcher.ui.screens.content.home.version.VersionCardDir
import com.cmc.launcher.ui.screens.content.home.version.VersionCardStatus
import com.cmc.launcher.ui.screens.content.versions.DescriptionTextRender
import com.cmc.launcher.ui.screens.content.versions.ServerSignalIcon
import com.cmc.launcher.ui.theme.cardColor
import com.cmc.launcher.ui.theme.onCardColor
import com.cmc.launcher.utils.copyText
import com.cmc.launcher.utils.string.stripColorCodes

val LocalServerCardQuickPlay = staticCompositionLocalOf<(Version?, String) -> Unit> { { _, _ -> } }



@Composable
fun CardState.ServerCardContent(cardId: String) {
    val states by ServerCardManager.cards.collectAsStateWithLifecycle()
    val card = states.firstOrNull { it.record.cardId == cardId } ?: return
    val quickPlay = LocalServerCardQuickPlay.current
    var settingsOpen by rememberSaveable(cardId) { mutableStateOf(false) }
    val loaded = card.ping as? ServerCardPingStatus.Loaded
    val undefined = stringResource(R.string.servers_list_undefined)
    val full = stringResource(R.string.servers_list_players_full)
    val detail = when (val ping = card.ping) {
        ServerCardPingStatus.Loading -> stringResource(R.string.generic_loading)
        ServerCardPingStatus.Failed -> stringResource(R.string.servers_list_failed_to_connect)
        is ServerCardPingStatus.Loaded -> "${ping.result.pingMs} ms · ${playersText(ping.result.status.players, undefined, full)}"
    }
    HomeEntry(
        title = card.record.name,
        detail = detail,
        enabled = interaction == CardInteraction.Idle,
        icon = { size -> ServerCardIcon(card = card, iconSize = size) },
        onOpen = { settingsOpen = true },
        onLaunch = if (card.record.quickLaunchEnabled) ({
            val version = when (card.record.versionMode) {
                ServerCardVersionMode.CURRENT -> null
                ServerCardVersionMode.SPECIFIC -> (card.versionStatus as? VersionCardStatus.Available)
                    ?.version?.takeIf { it.isValid() }
            }
            quickPlay(version, card.record.cardId)
        }) else null,
        summary = {
            loaded?.result?.status?.description?.let {
                DescriptionTextRender(description = it, fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    maxLines = 3, softWrap = true)
            }
        }
    )
    if (settingsOpen) ServerCardSettingsHost(cardId, onDismiss = { settingsOpen = false })
}

@Composable
fun ServerCardSettingsHost(
    cardId: String,
    onDismiss: () -> Unit
) {
    val states by ServerCardManager.cards.collectAsStateWithLifecycle()
    val record = remember(states, cardId) {
        states.firstOrNull { it.record.cardId == cardId }?.record
    } ?: run {
        onDismiss()
        return
    }

    ServerCardSettingsDialog(
        record = record,
        onDismissRequest = onDismiss
    )
}

@Composable
private fun ServerCardIcon(
    card: ServerCardState?,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val pxSize = with(density) { iconSize.roundToPx() }
    val icon = card?.record?.icon

    val sizeModifier = modifier
        .size(iconSize)
        .clip(CardIconShape)
    if (icon == null) {
        Image(
            modifier = sizeModifier,
            painter = painterResource(R.drawable.ic_unknown_icon),
            contentDescription = null
        )
        return
    }

    val imageRequest = remember(icon, pxSize) {
        ImageRequest.Builder(context)
            .data(icon)
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
        AsyncImagePainter.State.Empty -> Box(modifier = sizeModifier)
        is AsyncImagePainter.State.Loading -> ShimmerBox(modifier = sizeModifier)
        is AsyncImagePainter.State.Error,
        is AsyncImagePainter.State.Success -> {
            Image(
                painter = painter,
                contentDescription = null,
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                modifier = sizeModifier
            )
        }
    }
}

private fun playersText(players: Players, undefined: String, full: String): String {
    val online = players.online
    val max = players.max
    return when {
        online < 0 -> undefined
        max <= 0 -> online.toString()
        online in 0..max -> "$online/$max"
        else -> full
    }
}
