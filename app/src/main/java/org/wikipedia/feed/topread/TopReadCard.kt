package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.ListCard
import org.wikipedia.settings.homefeed.CommunityModuleType
import org.wikipedia.util.DateUtil

@Parcelize
class TopReadCard(
    val articles: TopRead,
    val age: Int,
    val site: WikiSite
) : ListCard<TopReadItemCard>(toItems(articles.articles, site), site), Parcelable {

    override fun moduleKey(): String {
        return CommunityModuleType.TOP_READ.name
    }

    // TODO: replace this with a general function for sending title.
    override fun subtitle(): String {
        return DateUtil.getShortDateString(articles.localDate)
    }

    override fun dismissHashCode(): Int {
        return articles.localDate.toEpochDay().toInt() + wikiSite().hashCode()
    }

    companion object {
        fun toItems(articles: List<PageSummary>, wiki: WikiSite): List<TopReadItemCard> {
            return articles.map { TopReadItemCard(it, wiki) }
        }
    }
}
