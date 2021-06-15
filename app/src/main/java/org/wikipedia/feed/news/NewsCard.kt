package org.wikipedia.feed.news

import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.UtcDate
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.util.L10nUtil
import java.util.concurrent.TimeUnit

class NewsCard(private val news: List<NewsItem>,
               private val age: Int,
               wiki: WikiSite) : WikiSiteCard(wiki) {

    override fun title(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_card_news_title)
    }

    override fun type(): CardType {
        return CardType.NEWS_LIST
    }

    override fun dismissHashCode(): Int {
        return TimeUnit.MILLISECONDS.toDays(date().baseCalendar.time.time).toInt() + wikiSite().hashCode()
    }

    fun date(): UtcDate {
        return UtcDate(age)
    }

    fun news(): List<NewsItem> {
        return news
    }
}
