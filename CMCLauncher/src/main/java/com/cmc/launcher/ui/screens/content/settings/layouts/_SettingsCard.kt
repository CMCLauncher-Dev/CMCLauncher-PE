package com.cmc.launcher.ui.screens.content.settings.layouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmc.launcher.ui.components.TitleAndSummary
import com.cmc.launcher.ui.theme.Alpha
import com.cmc.launcher.ui.theme.Size
import com.cmc.launcher.ui.theme.Space

enum class CardPosition { Top, TopStart, TopEnd, Middle, Bottom, BottomStart, BottomEnd, Single }

@Composable
fun rememberSettingsCardShape(position: CardPosition, outerShape: Dp = 20.dp, innerShape: Dp = 4.dp): Shape =
    remember(position, outerShape, innerShape) {
        when (position) {
            CardPosition.Single -> RoundedCornerShape(outerShape)
            CardPosition.Top -> RoundedCornerShape(outerShape, outerShape, innerShape, innerShape)
            CardPosition.Bottom -> RoundedCornerShape(innerShape, innerShape, outerShape, outerShape)
            CardPosition.TopStart -> RoundedCornerShape(outerShape, innerShape, innerShape, innerShape)
            CardPosition.TopEnd -> RoundedCornerShape(innerShape, outerShape, innerShape, innerShape)
            CardPosition.BottomStart -> RoundedCornerShape(innerShape, innerShape, innerShape, outerShape)
            CardPosition.BottomEnd -> RoundedCornerShape(innerShape, innerShape, outerShape, innerShape)
            CardPosition.Middle -> RoundedCornerShape(innerShape)
        }
    }

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    position: CardPosition,
    outerShape: Dp = 20.dp,
    innerShape: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier.fillMaxWidth(), shape = rememberSettingsCardShape(position, outerShape, innerShape),
        color = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(content = content)
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    position: CardPosition,
    outerShape: Dp = 20.dp,
    innerShape: Dp = 4.dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = rememberSettingsCardShape(position, outerShape, innerShape)
    Surface(
        modifier.fillMaxWidth().clip(shape).clickable(enabled = enabled, role = Role.Button,
            interactionSource = null, indication = null, onClick = onClick),
        shape = shape, color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else Alpha.medium)
    ) {
        Column(Modifier.heightIn(min = Size.touch), content = content)
    }
}

@Composable
fun SettingsCard(
    position: CardPosition,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    summaryStyle: TextStyle = MaterialTheme.typography.bodySmall,
    outerShape: Dp = 20.dp,
    innerShape: Dp = 4.dp,
    innerPadding: PaddingValues = PaddingValues(Space.lg),
    onClick: () -> Unit,
    trailingIcon: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true
) {
    SettingsCard(modifier, position, outerShape, innerShape, onClick, enabled) {
        Row(Modifier.fillMaxWidth().padding(innerPadding),
            horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
            TitleAndSummary(title = title, summary = summary, modifier = Modifier.weight(1f),
                titleStyle = titleStyle, summaryStyle = summaryStyle)
            trailingIcon?.invoke(this)
        }
    }
}

@Composable
fun SettingsCardColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
}
