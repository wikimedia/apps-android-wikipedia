package org.wikipedia.feed.suggestededits

import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.DateUtils

class SuggestedEditsCard(wiki: WikiSite,
                         val isTranslation: Boolean,
                         val sourceSummary: SuggestedEditsSummary?,
                         val targetSummary: SuggestedEditsSummary?) : WikiSiteCard(wiki) {

    override fun type(): CardType {
        return CardType.SUGGESTED_EDITS
    }

    override fun title(): String {
        return WikipediaApp.getInstance().getString(R.string.suggested_edits_feed_card_title)
    }

    override fun subtitle(): String {
        return DateUtils.getFeedCardDateString(0)
    }
}
