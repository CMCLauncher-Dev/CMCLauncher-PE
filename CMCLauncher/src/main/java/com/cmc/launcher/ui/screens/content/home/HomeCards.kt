package com.cmc.launcher.ui.screens.content.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.IntOffset
import com.cmc.launcher.ui.screens.content.home.server.ServerCardContent
import com.cmc.launcher.ui.screens.content.home.version.VersionCardContent
import com.movtery.cardgrid.model.CardLimits
import com.movtery.cardgrid.model.CardType

val CardIconShape = RoundedCornerShape(percent = 28)

object HomeCards {

    const val VERSION_CARD_TYPE_ID = "version_card"

    const val SERVER_CARD_TYPE_ID = "server_card"

    private val versionCardType = CardType(
        typeId = VERSION_CARD_TYPE_ID,
        defaultSpan = IntOffset(10, 6),
        limits = CardLimits(
            minWidth = 10,
            minHeight = 4,
            maxHeight = 12
        ),
        content = { cardId ->
            VersionCardContent(cardId)
        }
    )

    private val serverCardType = CardType(
        typeId = SERVER_CARD_TYPE_ID,
        defaultSpan = IntOffset(10, 6),
        limits = CardLimits(
            minWidth = 10,
            minHeight = 4,
            maxHeight = 12
        ),
        content = { cardId ->
            ServerCardContent(cardId)
        }
    )

    fun versionCardType(): CardType = versionCardType

    fun serverCardType(): CardType = serverCardType

    val userCardTypes: List<CardType> = listOf(versionCardType, serverCardType)
}
