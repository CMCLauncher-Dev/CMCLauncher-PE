package com.cmc.launcher.ui.screens.content.download.addon.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.favorites.FavoriteEntry
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.ui.screens.content.download.addon.elements.AddonIcon
import com.cmc.launcher.ui.screens.content.download.addon.elements.ClassesIdentifier
import com.cmc.launcher.ui.screens.content.download.addon.elements.PlatformIdentifier
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.theme.cardColor
import com.cmc.launcher.ui.theme.onCardColor
import com.cmc.launcher.utils.formatNumberByLocale

@Composable
fun FavoriteProjectLayout(
    entry: FavoriteEntry, onClick: () -> Unit, onUnfavorite: () -> Unit,
    modifier: Modifier = Modifier, shape: Shape = FieldShape, influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground), contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state
) {
    val remote = entry.remote
    val context = LocalContext.current
    val title = remote?.platformTitle() ?: entry.project.title
    val description = remote?.platformSummary() ?: entry.project.description
    val authors = remote?.platformAuthors() ?: entry.project.authors
    Surface(modifier.fillMaxWidth(), shape = shape, color = color, contentColor = contentColor) {
        Column {
            Column(Modifier.fillMaxWidth().clip(shape).clickable(enabled = !entry.invalid, role = Role.Button, onClick = onClick)
                .padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    AddonIcon(modifier = Modifier.clip(FieldShape), size = Size.touch, iconUrl = remote?.platformIconUrl() ?: entry.project.iconUrl)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!authors.isNullOrEmpty()) Text(authors.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    PlatformIdentifier(platform = entry.platform)
                    ClassesIdentifier(entry.project.classes)
                    remote?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                            Icon(painterResource(R.drawable.ic_download_2_filled), null, Modifier.size(Size.iconXs))
                            Text(formatNumberByLocale(context, it.platformDownloadCount()), style = MaterialTheme.typography.labelSmall)
                        }
                        it.platformFollows()?.let { count ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                                Icon(painterResource(R.drawable.ic_star_filled), null, Modifier.size(Size.iconXs))
                                Text(formatNumberByLocale(context, count), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                val tags = remote?.platformModLoaders().orEmpty().map { it.getDisplayName() } +
                    remote?.platformCategories(entry.project.classes).orEmpty().sortedBy { it.index() }.map { stringResource(it.getDisplayName()) }
                if (tags.isNotEmpty()) Text(tags.distinct().joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.invalid) Text(stringResource(R.string.favorites_item_unavailable),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onUnfavorite, modifier = Modifier.align(Alignment.End).padding(horizontal = Space.sm)) {
                Icon(painterResource(R.drawable.ic_favorite_filled), null, Modifier.size(Size.iconSm))
                Text(stringResource(R.string.pe_favorite_remove), Modifier.padding(start = Space.xs))
            }
        }
    }
}
