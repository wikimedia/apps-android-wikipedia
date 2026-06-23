package org.wikipedia.feed.model

import kotlinx.serialization.Serializable
import org.wikipedia.feed.wikigames.OnThisDayCardGameState
import org.wikipedia.settings.homefeed.ForYouModuleType
import java.time.LocalDate

@Serializable
class OnThisDayGameCard(
    val state: OnThisDayCardGameState,
    val date: String = LocalDate.now().toString()
) : ForYouCard() {
    override fun dismissHashCode(): Int = ForYouModuleType.GAMES.ordinal + date.hashCode()
}
