package com.cmc.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import com.cmc.launcher.setting.enums.isLauncherInDarkTheme
import com.cmc.launcher.ui.components.influencedByBackgroundColor

fun ColorScheme.withQuietSurfaces(dark: Boolean = surface.luminance() < 0.5f): ColorScheme {
    fun neutral(darkValue: Long, lightValue: Long): Color =
        lerp(Color(if (dark) darkValue else lightValue), primary, 0.025f)
    return copy(
        background = neutral(0xFF101114, 0xFFF5F6FA),
        surface = neutral(0xFF101114, 0xFFF5F6FA),
        surfaceDim = neutral(0xFF101114, 0xFFE6E8F0),
        surfaceBright = neutral(0xFF383B43, 0xFFFFFFFF),
        surfaceContainerLowest = neutral(0xFF141519, 0xFFFFFFFF),
        surfaceContainerLow = neutral(0xFF1A1C21, 0xFFF0F1F6),
        surfaceContainer = neutral(0xFF22242B, 0xFFEAECF3),
        surfaceContainerHigh = neutral(0xFF2C2F37, 0xFFFFFFFF),
        surfaceContainerHighest = neutral(0xFF363A44, 0xFFE3E6F0),
        surfaceVariant = neutral(0xFF24272E, 0xFFE9ECF4),
        outline = neutral(0xFF858B9B, 0xFF747B8D),
        outlineVariant = neutral(0xFF414550, 0xFFD5DAE6),
        onSurface = neutral(0xFFF1F2F7, 0xFF20232C),
        onSurfaceVariant = neutral(0xFFBDC2D0, 0xFF565F72),
        onBackground = neutral(0xFFF1F2F7, 0xFF20232C)
    )
}

private fun Color.luminance(): Float {
    val c = convert(ColorSpaces.Srgb)
    fun channel(v: Float) = if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * channel(c.red) + 0.7152f * channel(c.green) + 0.0722f * channel(c.blue)
}

@Composable
fun backgroundColor(): Color = MaterialTheme.colorScheme.surface
@Composable
fun onBackgroundColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun cardColor(
    influencedByBackground: Boolean = true
): Color = influencedByBackgroundColor(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    enabled = influencedByBackground
)
@Composable
fun onCardColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun cardTitleColor(
    alpha: Float = 0.5f
): Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)

@Composable
fun itemColor(
    influencedByBackground: Boolean = true,
    isDark: Boolean = isLauncherInDarkTheme()
): Color {
    return influencedByBackgroundColor(
        color = if (isDark) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        },
        enabled = influencedByBackground
    )
}
@Composable
fun onItemColor() = MaterialTheme.colorScheme.onSurface
