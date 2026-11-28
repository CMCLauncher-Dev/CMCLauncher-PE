package com.movtery.guide

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun GuideOverlay(
    controller: GuideController,
    state: GuideState.Active,
    registry: GuideRegistry,
    arbiter: GuideEventArbiter,
    fadeAlpha: Float,
    nextTip: @Composable (NextTip) -> Unit,
    animations: GuideAnimations
) {
    val entry = state.entry

    val version = registry.version
    val nodes = remember(entry.key, version) { registry.nodesFor(entry.key) }
    val anchors = nodes.map(GuideNode::bounds)

    val contentShape = nodes.firstNotNullOfOrNull { it.shape } ?: controller.shape

    LaunchedEffect(state.index, anchors) {

        if (!entry.isIntro) controller.updateAnchors(state.index, anchors)
    }

    LaunchedEffect(state.index) {
        if (entry.isIntro) return@LaunchedEffect
        var handled = 0
        while (registry.rectsFor(entry.key).isEmpty() && handled < 6) {
            if (registry.requestScroll(entry.key)) handled++
            delay(600.milliseconds)
        }
    }

    var armedForNext by remember(state.index) { mutableStateOf(false) }
    val gateActive = arbiter.gateActive(controller, state)
    val nextNodesReady = gateActive && arbiter.nextAnchorReady(controller, state)

    LaunchedEffect(armedForNext, nextNodesReady) {
        if (armedForNext && nextNodesReady) controller.next()
    }

    var contentRect by remember { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current
    val gapPx = with(density) { GuideDefaults.contentGap.toPx() }
    val paddingPx = with(density) { GuideDefaults.screenPadding.toPx() }
    val layoutDirection = LocalLayoutDirection.current

    val holes = nodes.map { node ->
        HoleTarget(
            rect = node.bounds,
            style = HoleStyle(
                radiusPx = with(density) { (node.holeRadius ?: controller.holeRadius).toPx() },
                borderWidthPx = with(density) { (node.holeBorderWidth ?: controller.holeBorderWidth).toPx() },
                borderColor = node.holeBorderColor ?: GuideDefaults.holeBorder
            )
        )
    }

    val ready = entry.isIntro || anchors.isNotEmpty()
    if (!ready && contentRect != null) contentRect = null

    CompositionLocalProvider(LocalContentColor provides controller.colors.content) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sharePointerInputWithSiblings()
        ) {
            Scrim(
                controller = controller,
                holes = holes,
                ready = ready,
                fadeAlpha = fadeAlpha,
                contentRect = contentRect,
                animations = animations,
                arbiter = arbiter,
                onAdvance = controller::next,
                onGateArm = { armedForNext = true }
            )
        }

        GuideCardContainer(
            controller = controller,
            state = state,
            registry = registry,
            anchors = anchors,
            placement = entry.placement,
            visible = ready,
            fadeAlpha = fadeAlpha,
            animations = animations,
            layoutDirection = layoutDirection,
            gapPx = gapPx,
            paddingPx = paddingPx,
            shape = contentShape,
            contentRect = contentRect,
            arbiter = arbiter,
            onGateArm = { armedForNext = true },
            onPlaced = { rect -> if (contentRect != rect) contentRect = rect }
        )

        val tip: NextTip? = when {
            !entry.showNextTip -> null
            entry.nodeClick == NodeClickMode.PassThrough -> null
            state.index == controller.entries.lastIndex -> NextTip.Finish
            entry.advanceOnScrimClick -> NextTip.TapBlank
            else -> null
        }
        NextTipSlot(
            tip = tip,
            anchors = anchors,
            contentRect = contentRect,
            fadeAlpha = fadeAlpha,
            animations = animations,
            content = nextTip
        )
    }
}

@Composable
private fun Scrim(
    controller: GuideController,
    holes: List<HoleTarget>,
    ready: Boolean,
    fadeAlpha: Float,
    contentRect: Rect?,
    animations: GuideAnimations,
    arbiter: GuideEventArbiter,
    onAdvance: () -> Unit,
    onGateArm: () -> Unit
) {
    val currentReady by rememberUpdatedState(ready)
    val currentContentRect by rememberUpdatedState(contentRect)
    val currentController by rememberUpdatedState(controller)
    val currentArbiter by rememberUpdatedState(arbiter)
    val currentOnAdvance by rememberUpdatedState(onAdvance)
    val currentOnGateArm by rememberUpdatedState(onGateArm)
    val currentColors by rememberUpdatedState(controller.colors)

    val currentHoles by rememberUpdatedState(holes)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                val scrim = currentColors.scrim
                drawRect(scrim.copy(alpha = scrim.alpha * fadeAlpha))
                if (currentReady) {
                    currentHoles.forEach { hole ->
                        val style = hole.style
                        drawRoundRect(
                            color = Color.Black,
                            topLeft = hole.rect.topLeft,
                            size = hole.rect.size,
                            cornerRadius = CornerRadius(style.radiusPx),
                            blendMode = BlendMode.Clear
                        )
                        if (style.borderWidthPx > 0f) {

                            val border = hole.rect.inflate(style.borderWidthPx / 2f)
                            drawRoundRect(
                                color = style.borderColor,
                                topLeft = border.topLeft,
                                size = border.size,
                                cornerRadius = CornerRadius(style.radiusPx + style.borderWidthPx / 2f),
                                style = Stroke(width = style.borderWidthPx)
                            )
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    val startPos = down.position

                    val observeOnly = currentArbiter.passThroughAt(startPos)
                    if (!observeOnly) down.consume()

                    var pressed = true
                    var isTap = true
                    val slop = viewConfiguration.touchSlop

                    while (pressed) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        var up = false
                        event.changes.forEach { change ->
                            if (change.id == down.id) {
                                if (!change.pressed && change.previousPressed) {
                                    up = true
                                } else if (abs(change.position.x - startPos.x) > slop ||
                                    abs(change.position.y - startPos.y) > slop
                                ) {
                                    isTap = false
                                }
                            }
                            if (!observeOnly) change.consume()
                        }
                        if (up) pressed = false
                    }

                    if (isTap) {
                        when (currentArbiter.resolveTap(currentController, currentContentRect, startPos)) {
                            TapAction.Advance -> currentOnAdvance()
                            TapAction.GateArm -> currentOnGateArm()
                            TapAction.None -> Unit
                        }
                    }
                }
            }
    )
}

private data class HoleStyle(
    val radiusPx: Float,
    val borderWidthPx: Float,
    val borderColor: Color
)


private data class HoleTarget(val rect: Rect, val style: HoleStyle)

@Composable
private fun NextTipSlot(
    tip: NextTip?,
    anchors: List<Rect>,
    contentRect: Rect?,
    fadeAlpha: Float,
    animations: GuideAnimations,
    content: @Composable (NextTip) -> Unit
) {
    if (tip == null) return
    val density = LocalDensity.current
    val paddingPx = with(density) { GuideDefaults.nextTipPadding.toPx() }
    val marginPx = with(density) { GuideDefaults.nextTipMargin.toPx() }

     BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fadeAlpha }
    ) {
        val containerSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        Layout(
            content = { content(tip) }
        ) { measurables, constraints ->
            val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
            val placeable = measurables.firstOrNull()?.measure(loose)
            val tipSize = IntSize(placeable?.width ?: 0, placeable?.height ?: 0)
            val solved = run {
                solveNextTip(
                    tipSize = tipSize,
                    containerSize = containerSize,
                    paddingPx = paddingPx,
                    marginPx = marginPx,
                    obstacles = anchors + listOfNotNull(contentRect)
                )
            }
            val offset = solved ?: IntOffset.Zero
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable?.place(offset.x, offset.y)
            }
        }
    }
}

@Composable
private fun GuideCardContainer(
    controller: GuideController,
    state: GuideState.Active,
    registry: GuideRegistry,
    anchors: List<Rect>,
    placement: GuidePlacement,
    visible: Boolean,
    fadeAlpha: Float,
    animations: GuideAnimations,
    layoutDirection: LayoutDirection,
    gapPx: Float,
    paddingPx: Float,
    shape: Shape,
    contentRect: Rect?,
    arbiter: GuideEventArbiter,
    onGateArm: () -> Unit,
    onPlaced: (Rect) -> Unit
) {
    val entry = state.entry
    val sideState = remember { mutableStateOf<GuideSide?>(null) }


    val currentArbiter by rememberUpdatedState(arbiter)
    val currentContentRect by rememberUpdatedState(contentRect)
    val currentController by rememberUpdatedState(controller)
    val currentOnGateArm by rememberUpdatedState(onGateArm)

    if (!visible) return

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fadeAlpha }
    ) {
        val containerSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val anchorsVisible = anchorsVisibleIn(anchors, containerSize)

        LaunchedEffect(state.index, anchorsVisible) {
            if (!entry.isIntro && anchors.isNotEmpty() && !anchorsVisible) {
                registry.bringIntoView(entry.key)
            }
        }

        val sideHint = registry.sideHintFor(entry.key)
        val effectivePlacement = when {
            entry.isIntro || !anchorsVisible -> GuidePlacement.Fixed(Alignment.Center)
            else -> resolvePlacement(placement, sideHint, anchors)
        }

        val scope = remember(anchors, containerSize, controller) {
            GuideScope(anchors, containerSize, sideState, controller)
        }
        Layout(
            content = {
                key(entry) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .clip(shape)
                            .pointerInput(Unit) {
                                detectTapGestures { position ->
                                    val rect = currentContentRect
                                    val rootPosition = position + (rect?.topLeft ?: Offset.Zero)
                                    when (currentArbiter.resolveTap(currentController, rect, rootPosition)) {
                                        TapAction.Advance -> currentController.next()
                                        TapAction.GateArm -> currentOnGateArm()
                                        TapAction.None -> Unit
                                    }
                                }
                            }
                            .verticalScroll(scrollState)
                    ) {
                        entry.content(scope)
                    }
                }
            }
        ) { measurables, constraints ->
            val loose = Constraints(
                maxWidth = constraints.maxWidth,
                maxHeight = (constraints.maxHeight - paddingPx * 2).roundToInt().coerceAtLeast(0)
            )
            val placeables = measurables.map { it.measure(loose) }
            val contentSize = IntSize(
                placeables.maxOf { it.width },
                placeables.maxOf { it.height }
            )

            val solved = if (entry.isIntro || anchors.isNotEmpty()) {
                solvePlacement(
                    placement = effectivePlacement,
                    anchors = anchors,
                    contentSize = contentSize,
                    containerSize = containerSize,
                    layoutDirection = layoutDirection,
                    gapPx = gapPx,
                    paddingPx = paddingPx
                ).also {
                    if (sideState.value != it.side) sideState.value = it.side
                    onPlaced(
                        Rect(
                            it.offset.x.toFloat(),
                            it.offset.y.toFloat(),
                            (it.offset.x + contentSize.width).toFloat(),
                            (it.offset.y + contentSize.height).toFloat()
                        )
                    )
                }
            } else {
                null
            }

            val offset = solved?.offset ?: IntOffset.Zero
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach { it.place(offset.x, offset.y) }
            }
        }
    }
}
