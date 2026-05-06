package org.wikipedia.feed.becauseyouread

import android.net.Uri
import androidx.core.net.toUri
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.history.HistoryEntry

class BecauseYouReadCard(
    val entry: HistoryEntry,
    val sourceDisplayTitle: String
) : Card() {
    override fun image(): Uri? {
        return entry.title.thumbUrl?.toUri()
    }

    override fun extract(): String {
        return entry.title.description.orEmpty()
    }

    override fun type(): CardType {
        return CardType.RANDOM // TODO: remove, since this is no longer used
    }

    override fun dismissHashCode(): Int {
        return entry.title.hashCode()
    }
}
