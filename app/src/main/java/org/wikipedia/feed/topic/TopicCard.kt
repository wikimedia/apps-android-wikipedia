package org.wikipedia.feed.topic

import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.model.CardType
import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.L10nUtil

class TopicCard(page: PageSummary, age: Int, wiki: WikiSite, val topic: String) :
    FeaturedArticleCard(page, age, wiki) {

    override fun title(): String {
        return "Featured Topic"
    }

    fun topic(): String {
        return topic
    }

    override fun footerActionText(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode, R.string.view_random_article_card_action)
    }

    override fun type(): CardType {
        return CardType.FEATURED_TOPIC
    }

    override fun historyEntrySource(): Int {
        return HistoryEntry.SOURCE_FEED_RANDOM
    }
}