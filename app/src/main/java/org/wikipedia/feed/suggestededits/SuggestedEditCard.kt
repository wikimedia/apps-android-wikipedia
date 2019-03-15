package org.wikipedia.feed.suggestededits

import io.reactivex.annotations.NonNull
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.util.DateUtil

class SuggestedEditCard(wiki: WikiSite) : WikiSiteCard(wiki) {
    var wiki: WikiSite? = null
    var sourcePage: RbPageSummary? = null
    var destinationLanguageCode: String? = null
    var age: Int? = null

    override fun type(): CardType {
        return CardType.SUGGESTED_EDITS
    }

    constructor(@NonNull sourcePage: RbPageSummary, @NonNull destinationLanguageCode: String, @NonNull age: Int, @NonNull wiki: WikiSite) : this(wiki) {
        this.age = age
        this.sourcePage = sourcePage
        this.destinationLanguageCode = destinationLanguageCode
        this.wiki = wiki
    }

    override fun title(): String {
        return WikipediaApp.getInstance().getString(R.string.suggested_edits)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardDateString(0)
    }
}
