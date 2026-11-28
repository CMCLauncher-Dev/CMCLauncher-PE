package com.cmc.launcher.ui.components

import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AnimatedDirection { Auto, Top, Bottom, Left, Right, None }

@Stable
class AnimatedContext(val progress: State<Float>, val alphaProgress: State<Float>, val distanceScale: State<Float>, val defaultDirection: AnimatedDirection)

private object Shown : State<Float> { override val value = 1f }
private val columnContext = AnimatedContext(Shown, Shown, Shown, AnimatedDirection.Top)
private val rowContext = AnimatedContext(Shown, Shown, Shown, AnimatedDirection.Right)

@Composable
fun rememberAnimatedContext(isVisible: Boolean, delayMillis: Int = 0): AnimatedContext = columnContext

fun Modifier.animated(ctx: AnimatedContext, from: AnimatedDirection = AnimatedDirection.Auto, distance: Dp = 40.dp, fade: Boolean = true): Modifier = this

@Composable
fun swapAnimateProgressAsState(isVisible: Boolean, animationSpec: DurationBasedAnimationSpec<Float>): State<Float> = Shown

@Composable
fun swapAnimateAlphaProgressAsState(isVisible: Boolean, animationSpec: DurationBasedAnimationSpec<Float>): State<Float> = Shown

interface AnimatedColumnScope : ColumnScope {
    @Composable fun AnimatedItem(delay: Int? = null, content: @Composable ColumnScope.(ctx: AnimatedContext) -> Unit)
}

private class StaticColumnScope(scope: ColumnScope) : AnimatedColumnScope, ColumnScope by scope {
    @Composable override fun AnimatedItem(delay: Int?, content: @Composable ColumnScope.(ctx: AnimatedContext) -> Unit) { content(columnContext) }
}

@Composable
fun AnimatedColumn(modifier: Modifier = Modifier, isVisible: Boolean, stagger: Boolean = true, verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp), horizontalAlignment: Alignment.Horizontal = Alignment.Start, content: @Composable AnimatedColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement, horizontalAlignment) { StaticColumnScope(this).content() }
}

interface AnimatedRowScope : RowScope {
    @Composable fun AnimatedItem(delay: Int? = null, content: @Composable RowScope.(ctx: AnimatedContext) -> Unit)
}

private class StaticRowScope(scope: RowScope) : AnimatedRowScope, RowScope by scope {
    @Composable override fun AnimatedItem(delay: Int?, content: @Composable RowScope.(ctx: AnimatedContext) -> Unit) { content(rowContext) }
}

@Composable
fun AnimatedRow(modifier: Modifier = Modifier, isVisible: Boolean, stagger: Boolean = true, horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp), content: @Composable AnimatedRowScope.() -> Unit) {
    Row(modifier, horizontalArrangement) { StaticRowScope(this).content() }
}

interface AnimatedLazyListScope {
    fun animatedItem(key: Any? = null, delay: Int? = null, content: @Composable LazyItemScope.(ctx: AnimatedContext) -> Unit)
    fun <T> animatedItems(items: List<T>, delay: Int = 0, key: ((item: T) -> Any)? = null, content: @Composable LazyItemScope.(index: Int, item: T, ctx: AnimatedContext) -> Unit)
}

private class StaticLazyScope(private val list: LazyListScope) : AnimatedLazyListScope {
    override fun animatedItem(key: Any?, delay: Int?, content: @Composable LazyItemScope.(ctx: AnimatedContext) -> Unit) {
        list.item(key = key) { content(columnContext) }
    }
    override fun <T> animatedItems(items: List<T>, delay: Int, key: ((item: T) -> Any)?, content: @Composable LazyItemScope.(index: Int, item: T, ctx: AnimatedContext) -> Unit) {
        list.itemsIndexed(items, key = key?.let { itemKey -> { _, item -> itemKey(item) } }) { index, item -> content(index, item, columnContext) }
    }
}

@Composable
fun AnimatedLazyColumn(modifier: Modifier = Modifier, state: LazyListState = rememberLazyListState(), isVisible: Boolean, stagger: Boolean = true, verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp), contentPadding: PaddingValues = PaddingValues(0.dp), content: AnimatedLazyListScope.() -> Unit) {
    LazyColumn(modifier = modifier, state = state, contentPadding = contentPadding, verticalArrangement = verticalArrangement) { StaticLazyScope(this).content() }
}
