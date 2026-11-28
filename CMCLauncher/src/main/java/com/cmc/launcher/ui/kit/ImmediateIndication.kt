package com.cmc.launcher.ui.kit

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

object ImmediateIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = ImmediateIndicationNode(interactionSource)
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = javaClass.hashCode()
}

private class ImmediateIndicationNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private val presses = mutableSetOf<PressInteraction.Press>()
    private val hovers = mutableSetOf<HoverInteraction.Enter>()
    private val focus = mutableSetOf<FocusInteraction.Focus>()
    private var alpha = 0f

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses.add(interaction)
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                    is HoverInteraction.Enter -> hovers.add(interaction)
                    is HoverInteraction.Exit -> hovers.remove(interaction.enter)
                    is FocusInteraction.Focus -> focus.add(interaction)
                    is FocusInteraction.Unfocus -> focus.remove(interaction.focus)
                }
                val next = when {
                    presses.isNotEmpty() -> 0.16f
                    focus.isNotEmpty() -> 0.10f
                    hovers.isNotEmpty() -> 0.06f
                    else -> 0f
                }
                if (next != alpha) {
                    alpha = next
                    invalidateDraw()
                }
            }
        }
    }

    override fun onDetach() {
        presses.clear()
        hovers.clear()
        focus.clear()
        alpha = 0f
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (alpha > 0f) drawRect(Color(0xFF8E9BE0), alpha = alpha)
    }
}