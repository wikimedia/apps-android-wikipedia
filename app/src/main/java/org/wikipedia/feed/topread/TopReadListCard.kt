package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.ListCard
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil

@Parcelize
class TopReadListCard(private val articles: TopRead, val site: WikiSite) :
    ListCard<TopReadItemCard>(toItems(articles.articles, site), site), Parcelable {

    override fun title(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode, R.string.view_top_read_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getShortDateString(articles.localDate)
    }

    override fun type(): CardType {
        return CardType.TOP_READ_LIST
    }

    fun footerActionText(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode, R.string.view_top_read_card_action)
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
