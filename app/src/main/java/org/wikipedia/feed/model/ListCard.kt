package org.wikipedia.feed.model

import org.wikipedia.dataclient.WikiSite
import java.util.*

abstract class ListCard<T : Card>(private val items: List<T>,
                                  wiki: WikiSite) : WikiSiteCard(wiki) {

    fun items(): List<T> {
        return Collections.unmodifiableList(items)
    }
}
