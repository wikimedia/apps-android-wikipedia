package org.wikipedia.feed.model

import org.wikipedia.dataclient.WikiSite

abstract class WikiSiteCard(val wiki: WikiSite) : Card() {

    fun wikiSite(): WikiSite {
        return wiki
    }
}
