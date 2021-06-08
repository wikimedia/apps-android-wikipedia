package org.wikipedia.feed.mainpage

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard

class MainPageCard(wiki: WikiSite) : WikiSiteCard(wiki) {

    override fun type(): CardType {
        return CardType.MAIN_PAGE
    }
}
