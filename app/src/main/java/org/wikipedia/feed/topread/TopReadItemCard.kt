package org.wikipedia.feed.topread

import android.net.Uri
import androidx.core.net.toUri
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

@JsonClass(generateAdapter = true)
class TopReadItemCard internal constructor(private val page: PageSummary,
                                           private val wiki: WikiSite) : Card() {

    override fun title(): String {
        return page.displayTitle
    }

    override fun subtitle(): String? {
        return page.description
    }

    override fun image(): Uri? {
        return page.thumbnailUrl?.ifEmpty { null }?.toUri()
    }

    override fun type(): CardType {
        return CardType.MOST_READ_ITEM
    }

    val pageViews get() = page.views
    val viewHistory get() = page.viewHistory
    val pageTitle get() = page.getPageTitle(wiki)
}
