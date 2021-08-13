package org.wikipedia.feed.becauseyouread

import android.net.Uri
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.page.PageTitle

class BecauseYouReadItemCard(private val title: PageTitle) : Card() {

    fun pageTitle(): PageTitle {
        return title
    }

    override fun title(): String {
        return title.displayTextValue
    }

    override fun subtitle(): String? {
        return title.description
    }

    override fun image(): Uri? {
        return if (title.thumbUrl.isNullOrEmpty()) null else Uri.parse(title.thumbUrl)
    }

    override fun type(): CardType {
        return CardType.BECAUSE_YOU_READ_ITEM
    }
}
