package org.wikipedia.feed.suggestededits

import io.reactivex.annotations.NonNull
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.util.DateUtil

class SuggestedEditCard(wiki: WikiSite) : WikiSiteCard(wiki) {
    var wiki: WikiSite? = null
    var age: Int? = null
    var translation: Boolean = false

    override fun type(): CardType {
        return CardType.SUGGESTED_EDITS
    }

    constructor(@NonNull wiki: WikiSite, @NonNull translation: Boolean) : this(wiki) {
        this.wiki = wiki
        this.translation = translation
    }

    override fun title(): String {
        return WikipediaApp.getInstance().getString(R.string.suggested_edits)
    }

    fun isTranslation(): Boolean {
        return translation
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardDateString(0)
    }
}
