package com.cmc.launcher.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.cmc.launcher.ui.theme.Alpha
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

@Composable
fun CmcListAction(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes endIconRes: Int? = null,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val foreground = when {
        !enabled -> colors.onSurface.copy(alpha = Alpha.medium)
        destructive -> colors.error
        else -> colors.onSurface
    }
    val iconColor = if (enabled && !destructive) colors.onSurfaceVariant else foreground
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = com.cmc.launcher.ui.kit.ImmediateIndication,
                role = Role.Button,
                onClick = onClick
            )
            .heightIn(min = Size.touch)
            .padding(horizontal = Space.md, vertical = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(iconRes), null, Modifier.size(Size.iconSm), tint = iconColor)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(title, color = foreground, style = MaterialTheme.typography.bodyLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (summary != null) {
                Text(summary, color = iconColor, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (endIconRes != null) {
            Icon(painterResource(endIconRes), null, Modifier.size(Size.iconXs), tint = iconColor)
        }
    }
}
