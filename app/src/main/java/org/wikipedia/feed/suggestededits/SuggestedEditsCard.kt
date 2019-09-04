package org.wikipedia.feed.suggestededits

import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.suggestededits.SuggestedEditsSummary

class SuggestedEditsCard(wiki: WikiSite,
                         val invokeSource: InvokeSource,
                         val sourceSummary: SuggestedEditsSummary?,
                         val targetSummary: SuggestedEditsSummary?,
                         val age: Int) : WikiSiteCard(wiki) {

    override fun type(): CardType {
        return CardType.SUGGESTED_EDITS
    }

    override fun title(): String {
        return WikipediaApp.getInstance().getString(R.string.suggested_edits_feed_card_title)
    }

    fun logImpression() {
        SuggestedEditsFunnel.get(InvokeSource.FEED).impression(invokeSource)
    }
}
