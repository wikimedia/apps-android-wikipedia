package org.wikipedia.feed.suggestededits

import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.log.L

class SuggestedEditsCard(
        val wiki: WikiSite,
        val action: Action,
        val sourceSummaryForEdit: PageSummaryForEdit?,
        val targetSummaryForEdit: PageSummaryForEdit?,
        val page: MwQueryPage?,
        val age: Int
) : WikiSiteCard(wiki) {

    override fun type(): CardType {
        return CardType.SUGGESTED_EDITS
    }

    override fun title(): String {
        return L10nUtil.getStringForArticleLanguage(targetSummaryForEdit?.lang
                ?: wiki.languageCode(), R.string.suggested_edits_feed_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardDateString(age)
    }

    fun logImpression() {
        SuggestedEditsFunnel.get(InvokeSource.FEED).impression(action)
    }
}
