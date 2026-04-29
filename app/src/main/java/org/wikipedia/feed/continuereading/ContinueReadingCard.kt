package org.wikipedia.feed.continuereading

import android.net.Uri
import androidx.core.net.toUri
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.history.HistoryEntry

class ContinueReadingCard(
    val entry: HistoryEntry
) : Card() {
    override fun image(): Uri? {
        return entry.title.thumbUrl?.toUri()
    }

    override fun extract(): String {
        return entry.title.description.orEmpty()
    }

    override fun type(): CardType {
        return CardType.BECAUSE_YOU_READ_LIST
    }

    override fun dismissHashCode(): Int {
        return entry.title.hashCode()
    }
}
