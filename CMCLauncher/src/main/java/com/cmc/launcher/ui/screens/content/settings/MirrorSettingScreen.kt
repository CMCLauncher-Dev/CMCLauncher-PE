package com.cmc.launcher.ui.screens.content.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cmc.launcher.R
import com.cmc.launcher.setting.AllSettings
import com.cmc.launcher.setting.enums.MirrorSourceType
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.kit.CmcButton
import com.cmc.launcher.ui.kit.CmcCard
import com.cmc.launcher.ui.kit.CmcDivider
import com.cmc.launcher.ui.kit.CmcPanel
import com.cmc.launcher.ui.kit.CmcProgressBar
import com.cmc.launcher.ui.kit.CmcRadioRow
import com.cmc.launcher.ui.kit.CmcRow
import com.cmc.launcher.ui.kit.CmcSection
import com.cmc.launcher.ui.kit.CmcSliderRow
import com.cmc.launcher.ui.kit.CmcSwitchRow
import com.cmc.launcher.ui.kit.CmcSwatch
import com.cmc.launcher.ui.kit.CmcTag
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.theme.Alpha
import com.cmc.launcher.ui.theme.ColorThemeType
import com.cmc.launcher.ui.theme.Radius
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.viewmodel.LocalBackgroundViewModel
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.launch

@Composable
fun MirrorSettingScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Mirror, settingsScreenKey, false)
    ) { isVisible ->
        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = Space.screenH,
                end = Space.screenH,
                top = Space.sm,
                bottom = Space.xxxl
            ),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item { MirrorSection() }
            item { DownloadSection() }
            item { PaletteSection() }
            item { BackgroundSection() }
            item { BlurSection() }
            item { FooterSection() }
        }
    }
}

@Composable
private fun MirrorSection() {
    val current = AllSettings.gameDownloadSource.state
    CmcSection(
        title = stringResource(R.string.pe_mirror_section_title),
        caption = stringResource(R.string.pe_mirror_section_caption)
    )
    CmcPanel {
        MirrorItem(
            title = stringResource(R.string.pe_mirror_auto_title),
            caption = stringResource(R.string.pe_mirror_auto_caption),
            iconRes = R.drawable.ic_autorenew,
            selected = current == MirrorSourceType.AUTO
        ) {
            AllSettings.gameDownloadSource.save(MirrorSourceType.AUTO)
        }
        CmcDivider(modifier = Modifier.padding(horizontal = Space.xs))
        MirrorItem(
            title = stringResource(R.string.pe_mirror_mirror_title),
            caption = stringResource(R.string.pe_mirror_mirror_caption),
            iconRes = R.drawable.ic_rocket_launch_filled,
            selected = current == MirrorSourceType.MIRROR
        ) {
            AllSettings.gameDownloadSource.save(MirrorSourceType.MIRROR)
        }
        CmcDivider(modifier = Modifier.padding(horizontal = Space.xs))
        MirrorItem(
            title = stringResource(R.string.pe_mirror_official_title),
            caption = stringResource(R.string.pe_mirror_official_caption),
            iconRes = R.drawable.ic_public,
            selected = current == MirrorSourceType.OFFICIAL
        ) {
            AllSettings.gameDownloadSource.save(MirrorSourceType.OFFICIAL)
        }
    }
}

@Composable
private fun MirrorItem(
    title: String,
    caption: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    CmcCard(shape = RoundedCornerShape(Radius.medium), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(Size.iconSm),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    modifier = Modifier.padding(top = Space.xxs),
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(Space.sm))
            if (selected) {
                CmcTag(text = stringResource(R.string.pe_mirror_in_use))
            } else {
                Text(
                    text = stringResource(R.string.pe_mirror_switch),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PaletteSection() {
    val current = AllSettings.launcherColorTheme.state
    val customArgb = AllSettings.launcherCustomColor.state
    val customColor = Color(customArgb)
    val customStyle = AllSettings.launcherCustomPaletteStyle.state

    CmcSection(
        title = stringResource(R.string.pe_palette_section_title),
        caption = stringResource(R.string.pe_palette_section_caption),
        trailing = {
            CmcTag(
                text = themeName(current),
                container = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.soft)
            )
        }
    )

    CmcPanel {
        Text(
            text = stringResource(R.string.pe_palette_builtin),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(Space.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PaletteSwatch(ColorThemeType.DYNAMIC, current, Color(0xFF6C7C94))
            PaletteSwatch(ColorThemeType.EMBERMIRE, current, Color(0xFFD4652F))
            PaletteSwatch(ColorThemeType.VELVET_ROSE, current, Color(0xFFC2537A))
            PaletteSwatch(ColorThemeType.MISTWAVE, current, Color(0xFF4F8DA8))
            PaletteSwatch(ColorThemeType.GLACIER, current, Color(0xFF5B9BD5))
        }
        Spacer(Modifier.size(Space.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PaletteSwatch(ColorThemeType.VERDANTFIELD, current, Color(0xFF4E8A5C))
            PaletteSwatch(ColorThemeType.URBAN_ASH, current, Color(0xFF6E7076))
            PaletteSwatch(ColorThemeType.VERDANT_DAWN, current, Color(0xFF69A97B))
            PaletteSwatch(ColorThemeType.CUSTOM, current, customColor)
            Spacer(Modifier.width(Size.swatch))
        }
    }

    if (current == ColorThemeType.CUSTOM) {
        CmcPanel(modifier = Modifier.padding(top = Space.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.pe_palette_seed),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                CmcSwatch(color = customColor, selected = true, onClick = {})
            }
            Spacer(Modifier.size(Space.xs))
            RgbChannel(
                label = stringResource(R.string.pe_rgb_red),
                value = (customArgb shr 16) and 0xFF
            ) { v ->
                AllSettings.launcherCustomColor.updateState(
                    (0xFF shl 24) or (v shl 16) or (customArgb and 0x0000FFFF)
                )
            }
            RgbChannel(
                label = stringResource(R.string.pe_rgb_green),
                value = (customArgb shr 8) and 0xFF
            ) { v ->
                AllSettings.launcherCustomColor.updateState(
                    (0xFF shl 24) or (customArgb and 0x00FF0000) or (v shl 8) or (customArgb and 0xFF)
                )
            }
            RgbChannel(
                label = stringResource(R.string.pe_rgb_blue),
                value = customArgb and 0xFF
            ) { v ->
                AllSettings.launcherCustomColor.updateState(
                    (0xFF shl 24) or (customArgb and 0x00FFFF00) or v
                )
            }
            CmcDivider(modifier = Modifier.padding(vertical = Space.sm))
            Text(
                text = stringResource(R.string.pe_palette_style),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(Space.xs))
            PaletteStyle.entries.forEach { style ->
                CmcRadioRow(
                    text = paletteStyleName(style),
                    selected = customStyle == style,
                    onClick = {
                        AllSettings.launcherCustomPaletteStyle.updateState(style)
                        AllSettings.launcherCustomPaletteStyle.save(style)
                    }
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    type: ColorThemeType,
    current: ColorThemeType,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CmcSwatch(
            color = color,
            selected = current == type,
            onClick = {
                AllSettings.launcherColorTheme.updateState(type)
                AllSettings.launcherColorTheme.save(type)
            }
        )
        Text(
            modifier = Modifier.padding(top = Space.xs),
            text = themeShortName(type),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (current == type) FontWeight.SemiBold else FontWeight.Normal,
            color = if (current == type) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RgbChannel(label: String, value: Int, onValue: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.width(20.dp),
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Space.xs),
            value = value / 255f,
            onValueChange = { onValue((it * 255f).toInt().coerceIn(0, 255)) },
            onValueChangeFinished = {
                AllSettings.launcherCustomColor.save(AllSettings.launcherCustomColor.state)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
        Text(
            modifier = Modifier.width(30.dp),
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BackgroundSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backgroundViewModel = LocalBackgroundViewModel.current
    val backgroundValid = backgroundViewModel?.isValid == true
    val isVideo = backgroundViewModel?.isVideo == true
    var isBusy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && backgroundViewModel != null) {
            isBusy = true
            scope.launch {
                val imported = runCatching { backgroundViewModel.import(context, uri) }.getOrDefault(false)
                if (imported && AllSettings.launcherBackgroundOpacity.state >= 100) {
                    AllSettings.launcherBackgroundOpacity.save(80)
                }
                if (!imported) {
                    android.widget.Toast.makeText(
                        context,
                        R.string.pe_background_import_failed,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                isBusy = false
            }
        }
    }

    CmcSection(
        title = stringResource(R.string.pe_background_section_title),
        caption = stringResource(R.string.pe_background_section_caption)
    )
    CmcPanel {
        CmcRow(
            icon = painterResource(R.drawable.ic_image_outlined),
            title = stringResource(R.string.pe_background_custom_title),
            summary = when {
                isBusy -> stringResource(R.string.pe_background_importing)
                backgroundValid && isVideo -> stringResource(R.string.pe_background_video_active)
                backgroundValid -> stringResource(R.string.pe_background_image_active)
                else -> stringResource(R.string.pe_background_none)
            },
            trailing = {
                if (backgroundValid) {
                    CmcTag(
                        text = stringResource(
                            if (isVideo) R.string.pe_background_tag_video
                            else R.string.pe_background_tag_image
                        ),
                        container = MaterialTheme.colorScheme.secondary.copy(alpha = Alpha.soft)
                    )
                }
            }
        )
        Spacer(Modifier.size(Space.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            CmcButton(
                modifier = Modifier.weight(1f),
                text = stringResource(
                    if (backgroundValid) R.string.pe_background_replace
                    else R.string.pe_background_pick
                ),
                enabled = !isBusy,
                onClick = {
                    picker.launch(
                        arrayOf("image/*", "video/*", "application/octet-stream")
                    )
                }
            )
            if (backgroundValid) {
                CmcButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.pe_background_remove),
                    filled = false,
                    accent = MaterialTheme.colorScheme.error,
                    onClick = { scope.launch { backgroundViewModel?.delete() } }
                )
            }
        }
    }
}

@Composable
private fun DownloadSection() {
    val threads = AllSettings.downloadThreads.state
    val connections = AllSettings.downloadConnections.state
    val wifiOnly = AllSettings.downloadWifiOnly.state
    val speedLimit = AllSettings.downloadSpeedLimitKbps.state
    CmcSection(
        title = stringResource(R.string.pe_download_concurrency_title),
        caption = stringResource(R.string.pe_download_concurrency_caption)
    )
    CmcPanel {
        CmcSliderRow(
            title = stringResource(R.string.pe_download_threads_title),
            summary = stringResource(R.string.pe_download_threads_summary),
            value = threads.toFloat(),
            onValueChange = { AllSettings.downloadThreads.updateState(it.toInt()) },
            valueRange = 1f..32f,
            valueLabel = threads.toString(),
            steps = 30,
            onValueChangeFinished = {
                AllSettings.downloadThreads.save(AllSettings.downloadThreads.state)
            }
        )
        CmcDivider(modifier = Modifier.padding(horizontal = Space.xs))
        CmcSliderRow(
            title = stringResource(R.string.pe_download_connections_title),
            summary = stringResource(R.string.pe_download_connections_summary),
            value = connections.toFloat(),
            onValueChange = { AllSettings.downloadConnections.updateState(it.toInt()) },
            valueRange = 1f..64f,
            valueLabel = connections.toString(),
            steps = 62,
            onValueChangeFinished = {
                AllSettings.downloadConnections.save(AllSettings.downloadConnections.state)
            }
        )
        CmcDivider(modifier = Modifier.padding(horizontal = Space.xs))
        CmcSwitchRow(
            title = stringResource(R.string.pe_download_wifi_only_title),
            summary = stringResource(R.string.pe_download_wifi_only_summary),
            checked = wifiOnly,
            onCheckedChange = { AllSettings.downloadWifiOnly.save(it) }
        )
        CmcDivider(modifier = Modifier.padding(horizontal = Space.xs))
        CmcSliderRow(
            title = stringResource(R.string.pe_download_speed_title),
            summary = stringResource(R.string.pe_download_speed_summary),
            value = speedLimit.toFloat(),
            onValueChange = { AllSettings.downloadSpeedLimitKbps.updateState(it.toInt()) },
            valueRange = 0f..10240f,
            valueLabel = if (speedLimit == 0) stringResource(R.string.pe_download_speed_unlimited) else "$speedLimit KB/s",
            onValueChangeFinished = {
                AllSettings.downloadSpeedLimitKbps.save(AllSettings.downloadSpeedLimitKbps.state)
            }
        )
        CmcDivider(modifier = Modifier.padding(horizontal = Space.xs))
        CmcSwitchRow(
            title = stringResource(R.string.pe_download_cleanup_title),
            summary = stringResource(R.string.pe_download_cleanup_summary),
            checked = AllSettings.downloadCleanupAfterInstall.state,
            onCheckedChange = { AllSettings.downloadCleanupAfterInstall.save(it) }
        )
    }
}

@Composable
private fun BlurSection() {
    val blur = AllSettings.backgroundBlur.state
    val opacity = AllSettings.launcherBackgroundOpacity.state

    CmcSection(
        title = stringResource(R.string.pe_appearance_section_title),
        caption = stringResource(R.string.pe_appearance_section_caption)
    )
    CmcPanel {
        CmcSliderRow(
            title = stringResource(R.string.pe_blur_strength_title),
            summary = stringResource(R.string.pe_blur_strength_summary),
            value = blur.toFloat(),
            onValueChange = { AllSettings.backgroundBlur.updateState(it.toInt()) },
            valueRange = 0f..40f,
            valueLabel = blur.toString(),
            steps = 39,
            onValueChangeFinished = {
                AllSettings.backgroundBlur.save(AllSettings.backgroundBlur.state)
            }
        )
        CmcDivider(modifier = Modifier.padding(horizontal = Space.xs))
        CmcSliderRow(
            title = stringResource(R.string.pe_bg_opacity_title),
            summary = stringResource(R.string.pe_bg_opacity_summary),
            value = opacity.toFloat(),
            onValueChange = { AllSettings.launcherBackgroundOpacity.updateState(it.toInt()) },
            valueRange = 20f..100f,
            valueLabel = "$opacity%",
            onValueChangeFinished = {
                AllSettings.launcherBackgroundOpacity.save(
                    AllSettings.launcherBackgroundOpacity.state
                )
            }
        )
        Spacer(Modifier.size(Space.xs))
        CmcProgressBar(progress = opacity / 100f)
    }
}

@Composable
private fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.xl, bottom = Space.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.pe_settings_applied),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.strong)
        )
        Spacer(Modifier.size(Space.xxs))
        Box(modifier = Modifier.width(40.dp).size(width = 40.dp, height = 2.dp))
    }
}

@Composable
private fun themeName(type: ColorThemeType): String = when (type) {
    ColorThemeType.DYNAMIC -> stringResource(R.string.theme_color_dynamic)
    ColorThemeType.EMBERMIRE -> stringResource(R.string.theme_color_embermire)
    ColorThemeType.VELVET_ROSE -> stringResource(R.string.theme_color_velvet_rose)
    ColorThemeType.MISTWAVE -> stringResource(R.string.theme_color_mistwave)
    ColorThemeType.GLACIER -> stringResource(R.string.theme_color_glacier)
    ColorThemeType.VERDANTFIELD -> stringResource(R.string.theme_color_verdant_field)
    ColorThemeType.URBAN_ASH -> stringResource(R.string.theme_color_urban_ash)
    ColorThemeType.VERDANT_DAWN -> stringResource(R.string.theme_color_verdant_dawn)
    ColorThemeType.CUSTOM -> stringResource(R.string.generic_custom)
}

@Composable
private fun themeShortName(type: ColorThemeType): String = when (type) {
    ColorThemeType.DYNAMIC -> stringResource(R.string.pe_theme_short_dynamic)
    ColorThemeType.EMBERMIRE -> stringResource(R.string.pe_theme_short_embermire)
    ColorThemeType.VELVET_ROSE -> stringResource(R.string.pe_theme_short_velvet_rose)
    ColorThemeType.MISTWAVE -> stringResource(R.string.pe_theme_short_mistwave)
    ColorThemeType.GLACIER -> stringResource(R.string.pe_theme_short_glacier)
    ColorThemeType.VERDANTFIELD -> stringResource(R.string.pe_theme_short_verdant_field)
    ColorThemeType.URBAN_ASH -> stringResource(R.string.pe_theme_short_urban_ash)
    ColorThemeType.VERDANT_DAWN -> stringResource(R.string.pe_theme_short_verdant_dawn)
    ColorThemeType.CUSTOM -> stringResource(R.string.pe_theme_short_custom)
}

@Composable
private fun paletteStyleName(style: PaletteStyle): String = when (style) {
    PaletteStyle.TonalSpot -> stringResource(R.string.pe_palette_style_tonal_spot)
    PaletteStyle.Neutral -> stringResource(R.string.pe_palette_style_neutral)
    PaletteStyle.Vibrant -> stringResource(R.string.pe_palette_style_vibrant)
    PaletteStyle.Expressive -> stringResource(R.string.pe_palette_style_expressive)
    PaletteStyle.Content -> stringResource(R.string.pe_palette_style_content)
    PaletteStyle.Fidelity -> stringResource(R.string.pe_palette_style_fidelity)
    PaletteStyle.Monochrome -> stringResource(R.string.pe_palette_style_monochrome)
    PaletteStyle.Rainbow -> stringResource(R.string.pe_palette_style_rainbow)
    PaletteStyle.FruitSalad -> stringResource(R.string.pe_palette_style_fruit_salad)
    else -> style.name
}