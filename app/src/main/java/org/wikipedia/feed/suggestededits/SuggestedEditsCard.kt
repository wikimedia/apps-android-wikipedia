package org.wikipedia.feed.suggestededits

import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.feed.suggestededits.SuggestedEditsFeedClient.SuggestedEditsType
import org.wikipedia.util.DateUtil

class SuggestedEditsCard(wiki: WikiSite,
                         val suggestedEditsType: SuggestedEditsType,
                         val sourceSummary: RbPageSummary?,
                         val targetSummary: RbPageSummary?) : WikiSiteCard(wiki) {

    override fun type(): CardType {
        return CardType.SUGGESTED_EDITS
    }

    override fun title(): String {
        return WikipediaApp.getInstance().getString(R.string.suggested_edits_feed_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardDateString(0)
    }
}
