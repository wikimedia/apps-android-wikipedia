package org.wikipedia.feed.model

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import org.wikipedia.page.PageTitle

@Serializable
class BecauseYouReadCard(
    val title: PageTitle,
    val sourceDisplayTitle: String
) : ForYouCard() {
    override fun image(): Uri? {
        return title.thumbUrl?.toUri()
    }

    override fun extract(): String {
        return title.description.orEmpty()
    }

    override fun type(): CardType {
        return CardType.RANDOM // TODO: remove, since this is no longer used
    }

    override fun dismissHashCode(): Int {
        return title.hashCode()
    }
}
