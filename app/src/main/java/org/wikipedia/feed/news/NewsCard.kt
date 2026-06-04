package org.wikipedia.feed.news

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.UtcDate
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.settings.homefeed.CommunityModuleType
import java.util.concurrent.TimeUnit

class NewsCard(
    val news: List<NewsItem>,
    val age: Int,
    wiki: WikiSite
) : WikiSiteCard(wiki) {

    override fun moduleKey(): String {
        return CommunityModuleType.NEWS.name
    }

    override fun dismissHashCode(): Int {
        return TimeUnit.MILLISECONDS.toDays(date().baseCalendar.time.time).toInt() + wikiSite().hashCode() + moduleKey().hashCode()
    }

    fun date(): UtcDate {
        return UtcDate(age)
    }
}
