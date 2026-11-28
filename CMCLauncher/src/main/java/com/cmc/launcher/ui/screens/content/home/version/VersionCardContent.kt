package com.cmc.launcher.ui.screens.content.home.version

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmc.launcher.R
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.ui.screens.content.elements.VersionIconImage
import com.cmc.launcher.ui.screens.content.home.CardIconShape
import com.cmc.launcher.ui.screens.content.home.HomeEntry
import com.movtery.cardgrid.model.CardInteraction
import com.movtery.cardgrid.model.CardState

val LocalHomeCardLauncher = staticCompositionLocalOf<(Version) -> Unit> { {} }
val LocalHomeCardVersionSettings = staticCompositionLocalOf<(Version) -> Unit> { {} }

@Composable
fun CardState.VersionCardContent(cardId: String) {
    val cards by VersionCardManager.cards.collectAsStateWithLifecycle()
    val card = cards.firstOrNull { it.record.cardId == cardId } ?: return
    val version = (card.status as? VersionCardStatus.Available)?.version
    val open = LocalHomeCardVersionSettings.current
    val launch = LocalHomeCardLauncher.current
    val detail = when (val status = card.status) {
        VersionCardStatus.Loading -> stringResource(R.string.generic_loading)
        VersionCardStatus.Deleted -> stringResource(R.string.home_version_card_deleted)
        VersionCardStatus.Inaccessible -> stringResource(R.string.home_version_card_inaccessible)
        is VersionCardStatus.Available -> status.version.getVersionInfo()?.let { info ->
            (listOf(info.minecraftVersion) + info.loaderInfos.map { it.loader.displayName }).joinToString(" · ")
        }.orEmpty()
    }
    HomeEntry(
        title = card.record.versionName,
        detail = detail,
        enabled = interaction == CardInteraction.Idle && version?.isValid() == true,
        icon = { size -> VersionIconImage(version, Modifier.size(size).clip(CardIconShape)) },
        onOpen = version?.let { { open(it) } },
        onLaunch = version?.let { { launch(it) } },
        summary = {
            version?.getVersionSummary()?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    )
}
