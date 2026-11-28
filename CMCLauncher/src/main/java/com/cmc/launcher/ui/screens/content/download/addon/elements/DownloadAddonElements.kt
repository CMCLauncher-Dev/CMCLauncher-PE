package com.cmc.launcher.ui.screens.content.download.addon.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.ui.theme.FieldShape
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.game.download.addon.platform.PlatformDisplayLabel
import com.cmc.launcher.game.download.addon.platform.PlatformProject
import com.cmc.launcher.game.download.addon.platform.PlatformVersion
import com.cmc.launcher.game.version.installed.Version
import com.cmc.launcher.game.version.installed.VersionsManager
import com.cmc.launcher.game.version.mod.InstalledMod
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.ui.AndroidStringText
import com.cmc.launcher.ui.components.IconTextButton
import com.cmc.launcher.ui.components.rememberMaxHeight
import com.cmc.launcher.ui.theme.cardColor
import com.cmc.launcher.ui.theme.onCardColor
import com.cmc.launcher.utils.formatNumberByLocale
import com.cmc.launcher.utils.getTimeAgo
import com.cmc.launcher.utils.isChinese
import com.cmc.launcher.utils.string.isNotEmptyOrBlank
import org.jackhuang.hmcl.addon.AddonProjectInfo
import org.jackhuang.hmcl.util.versioning.GameVersionNumber

sealed interface DownloadAddonState<T> {
    class Getting<T> : DownloadAddonState<T>
    data class Success<T>(val result: T) : DownloadAddonState<T>
    data class Error<T>(val message: AndroidStringText) : DownloadAddonState<T>
}

sealed interface DownloadAddonVersionLoading {
    data object None: DownloadAddonVersionLoading

    data object StartLoadPage: DownloadAddonVersionLoading

    data class LoadingPage(val chunk: Int, val page: Int): DownloadAddonVersionLoading
}

class VersionInfoMap(
    val gameVersion: String,
    val loader: PlatformDisplayLabel?,
    val versions: List<PlatformVersion>,
    val isAdapt: Boolean
)

suspend fun <E: PlatformVersion> List<E>.initAllGeneric(
    currentProjectId: String,
    also: suspend (E) -> Unit = {}
): List<E> {
    return mapNotNull { version ->
        if (!version.initFile(currentProjectId)) return@mapNotNull null
        version.also {
            also(it)
        }
    }.sortedByDescending {

        it.platformDatePublished()
    }
}

suspend fun List<PlatformVersion>.initAll(
    currentProjectId: String,
    also: suspend (PlatformVersion) -> Unit = {}
): List<PlatformVersion> {
    return initAllGeneric<PlatformVersion>(currentProjectId, also)
}

fun List<PlatformVersion>.mapWithVersions(classes: PlatformClasses): List<VersionInfoMap> {
    val grouped = mutableMapOf<Pair<String, PlatformDisplayLabel?>, MutableList<PlatformVersion>>()

    forEach { version ->
        val labels = version.platformLoaders().ifEmpty { listOf(null) }
        version.platformGameVersion().forEach { gameVer ->
            labels.forEach { loaderLabel ->
                grouped.getOrPut(gameVer to loaderLabel) { mutableListOf() } += version
            }
        }
    }

    val currentVersion = VersionsManager.currentVersion.value

    return grouped.map { (key, versions) ->
        VersionInfoMap(
            gameVersion = key.first,
            loader = key.second,
            versions = versions,
            isAdapt = when (classes) {
                PlatformClasses.MOD_PACK -> false
                else -> isVersionAdapt(currentVersion, key.first, key.second)
            }
        )
    }.sortedByVersionAndLoader()
}

private fun List<VersionInfoMap>.sortedByVersionAndLoader(): List<VersionInfoMap> {
    return sortedWith { a, b ->

        val versionCompare = -GameVersionNumber.compare(a.gameVersion, b.gameVersion)
        if (versionCompare != 0) {
            versionCompare
        } else {
            when {
                a.loader == null && b.loader == null -> 0
                a.loader == null -> 1
                b.loader == null -> -1
                else -> a.loader.getDisplayName().compareTo(b.loader.getDisplayName())
            }
        }
    }
}

private fun isVersionAdapt(
    currentVersion: Version?,
    gameVersion: String,
    loader: PlatformDisplayLabel?
): Boolean {
    return if (currentVersion == null) {
        false
    } else {
        if (currentVersion.getVersionInfo()?.minecraftVersion != gameVersion) {
            false
        } else {

            val loaderInfos = currentVersion.getVersionInfo()?.loaderInfos.orEmpty()
            when {
                loader == null -> true
                loaderInfos.isEmpty() -> false
                else -> loaderInfos.any { it.loader.displayName.equals(loader.getDisplayName(), true) }
            }
        }
    }
}

@Composable
fun AddonVersionItemLayout(
    modifier: Modifier = Modifier,
    infoMap: VersionInfoMap,
    defaultExpanded: Boolean = infoMap.isAdapt,
    maxListHeight: Dp = rememberMaxHeight(),
    shape: Shape = MaterialTheme.shapes.large,
    influencedByBackground: Boolean = true,
    color: Color = cardColor(influencedByBackground),
    contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state,
    installedChecker: ((PlatformVersion) -> InstalledMod?)? = null,
    onItemClicked: (PlatformVersion) -> Unit = {}
) {
    var expanded by remember(infoMap.gameVersion, infoMap.loader?.getDisplayName()) { mutableStateOf(defaultExpanded) }
    Surface(modifier.fillMaxWidth(), shape = shape, color = color, contentColor = contentColor) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(enabled = infoMap.versions.isNotEmpty(), role = Role.Button) { expanded = !expanded }
                .heightIn(min = Size.touch).padding(Space.md),
                horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(infoMap.gameVersion, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    infoMap.loader?.let { Text(it.getDisplayName(), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (infoMap.isAdapt) Icon(painterResource(R.drawable.ic_star_filled), null, Modifier.size(Size.iconSm))
                if (installedChecker != null && infoMap.versions.any { installedChecker(it) != null }) InstalledModBadge()
                Icon(painterResource(if (expanded) R.drawable.ic_arrow_drop_up_rounded else R.drawable.ic_arrow_drop_down_rounded),
                    stringResource(if (expanded) R.string.generic_collapse else R.string.generic_expand), Modifier.size(Size.iconSm))
            }
            if (expanded && infoMap.versions.isNotEmpty()) {
                HorizontalDivider()
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxListHeight.coerceIn(Size.touch, 480.dp)),
                    contentPadding = PaddingValues(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    items(infoMap.versions) { version ->
                        AddonVersionListItem(version = version, installed = installedChecker?.invoke(version),
                            onClick = { onItemClicked(version) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonVersionListItem(
    modifier: Modifier = Modifier,
    version: PlatformVersion,
    installed: InstalledMod? = null,
    onClick: () -> Unit = {}
) {
    val release = version.platformReleaseType()
    val context = LocalContext.current
    Row(modifier.fillMaxWidth().clip(FieldShape)
        .background(if (installed != null) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent)
        .clickable(role = Role.Button, onClick = onClick).heightIn(min = Size.touch).padding(Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(FieldShape).background(release.color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Text(release.name.take(1), color = release.color, style = MaterialTheme.typography.labelLarge)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(version.platformDisplayName(), style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(stringResource(release.textRes), style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_download_2_outlined), null, Modifier.size(Size.iconXs))
                    Text(formatNumberByLocale(context, version.platformDownloadCount()), style = MaterialTheme.typography.labelSmall)
                }
                Text(getTimeAgo(context, version.platformDatePublished()), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (installed != null) InstalledModBadge() else Icon(painterResource(R.drawable.ic_download), null, Modifier.size(Size.iconSm))
    }
}

@Composable
fun ProjectUrlsContent(
    platform: Platform,
    urls: PlatformProject.Urls,
    mcmod: AddonProjectInfo?,
    openLink: (String) -> Unit,
) {
    urls.projectUrl?.takeIf { it.isNotEmptyOrBlank() }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = when (platform) {
                Platform.CURSEFORGE -> painterResource(R.drawable.img_platform_curseforge)
                Platform.MODRINTH -> painterResource(R.drawable.img_platform_modrinth)
            },
            text = stringResource(R.string.download_addon_project_link)
        )
    }
    urls.sourceUrl?.takeIf { it.isNotEmptyOrBlank() }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = painterResource(R.drawable.ic_code),
            text = stringResource(R.string.download_addon_source_link)
        )
    }
    urls.issuesUrl?.takeIf { it.isNotEmptyOrBlank() }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = painterResource(R.drawable.ic_chat_info),
            text = stringResource(R.string.download_addon_issues_link)
        )
    }
    urls.wikiUrl?.takeIf { it.isNotEmptyOrBlank() }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = painterResource(R.drawable.ic_import_contacts_outlined),
            text = stringResource(R.string.download_addon_wiki_link)
        )
    }
    val context = LocalContext.current
    mcmod?.takeIf {
        isChinese(context)
    }?.mcmodUrl()?.takeIf {
        it.isNotEmptyOrBlank()
    }?.let { url ->
        IconTextButton(
            onClick = { openLink(url) },
            iconSize = 18.dp,
            painter = painterResource(R.drawable.ic_link),
            text = stringResource(R.string.pe_mcmod_wiki)
        )
    }
}

@Composable
fun ScreenshotItemLayout(
    screenshot: PlatformProject.Screenshot,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape
) {
    val context = LocalContext.current
    var attempt by remember(screenshot.imageUrl) { mutableStateOf(0) }
    val request = remember(context, screenshot.imageUrl, attempt) {
        screenshot.imageUrl.takeIf { it.isNotBlank() }?.let {
            ImageRequest.Builder(context).data(it).crossfade(false).build()
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        androidx.compose.runtime.key(attempt) {
            val painter = rememberAsyncImagePainter(model = request)
            val state by painter.state.collectAsStateWithLifecycle()
            when (state) {
                is AsyncImagePainter.State.Success -> {
                    val intrinsic = painter.intrinsicSize
                    val ratio = if (intrinsic.width.isFinite() && intrinsic.height.isFinite() &&
                        intrinsic.width > 0f && intrinsic.height > 0f) (intrinsic.width / intrinsic.height).coerceIn(0.4f, 4f) else 16f / 9f
                    Image(painter, screenshot.title, Modifier.fillMaxWidth().aspectRatio(ratio).clip(shape),
                        contentScale = ContentScale.Fit)
                }
                is AsyncImagePainter.State.Error -> {
                    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        Column(Modifier.fillMaxWidth().padding(Space.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.pe_image_failed), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.generic_retry)) }
                        }
                    }
                }
                else -> Text(stringResource(if (request == null) R.string.pe_image_failed else R.string.generic_loading),
                    Modifier.fillMaxWidth().padding(Space.md), style = MaterialTheme.typography.bodySmall)
            }
        }
        screenshot.title?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
        screenshot.description?.takeIf { it.isNotBlank() && it != screenshot.title }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}