package org.wikipedia.feed.topread

import androidx.annotation.VisibleForTesting
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.ListCard
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil
import java.util.concurrent.TimeUnit

class TopReadListCard(private val articles: TopRead,
                      wiki: WikiSite) : ListCard<TopReadItemCard>(toItems(articles.articles, wiki), wiki) {

    override fun title(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_top_read_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardDateString(articles.date)
    }

    override fun type(): CardType {
        return CardType.TOP_READ_LIST
    }

    fun footerActionText(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_top_read_card_action)
    }

    override fun dismissHashCode(): Int {
        return TimeUnit.MILLISECONDS.toDays(articles.date.time).toInt() + wikiSite().hashCode()
    }

    companion object {
        @JvmStatic
        @VisibleForTesting
        fun toItems(articles: List<TopReadArticles>, wiki: WikiSite): List<TopReadItemCard> {
            return articles.map { TopReadItemCard(it, wiki) }
        }
    }
}
