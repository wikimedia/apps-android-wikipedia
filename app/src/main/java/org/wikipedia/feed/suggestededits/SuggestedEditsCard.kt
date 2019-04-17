package org.wikipedia.feed.suggestededits

import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil

class SuggestedEditsCard(wiki: WikiSite) : WikiSiteCard(wiki) {
    var wiki: WikiSite? = null
    var age: Int? = null
    var isTranslation: Boolean = false
    var sourceDescription: String = ""
    var summary: RbPageSummary? = null
    var targetPageTitle: PageTitle? = null

    override fun type(): CardType {
        return CardType.SUGGESTED_EDITS
    }

    constructor(wiki: WikiSite, translation: Boolean, summary: RbPageSummary?, sourceDescription: String, targetPageTitle: PageTitle?) : this(wiki) {
        this.wiki = wiki
        this.isTranslation = translation
        this.summary = summary
        this.sourceDescription = sourceDescription
        this.targetPageTitle = targetPageTitle
    }

    override fun title(): String {
        return WikipediaApp.getInstance().getString(R.string.suggested_edits_feed_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardDateString(0)
    }
}
