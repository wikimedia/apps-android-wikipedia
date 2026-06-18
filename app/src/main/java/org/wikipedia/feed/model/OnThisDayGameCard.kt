package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.feed.wikigames.OnThisDayCardGameState
import org.wikipedia.settings.homefeed.ForYouModuleType

@Serializable
class OnThisDayGameCard(
    val state: OnThisDayCardGameState
) : ForYouCard() {
    override fun dismissHashCode(): Int = ForYouModuleType.GAMES.ordinal
}
