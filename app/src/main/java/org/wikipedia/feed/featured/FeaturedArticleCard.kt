package org.wikipedia.feed.featured

import android.net.Uri
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.model.WikiSiteCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil

open class FeaturedArticleCard(private val page: PageSummary,
                               private val age: Int, wiki: WikiSite) : WikiSiteCard(wiki) {

    override fun title(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode, R.string.view_featured_article_card_title)
    }

    override fun subtitle(): String {
        return DateUtil.getFeedCardDateString(age)
    }

    override fun image(): Uri? {
        return if (page.thumbnailUrl.isNullOrEmpty()) null else Uri.parse(page.thumbnailUrl)
    }

    override fun extract(): String? {
        return page.extract
    }

    override fun type(): CardType {
        return CardType.FEATURED_ARTICLE
    }

    override fun dismissHashCode(): Int {
        return page.apiTitle.hashCode()
    }

    open fun historyEntrySource(): Int {
        return HistoryEntry.SOURCE_FEED_FEATURED
    }

    open fun footerActionText(): String {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode, R.string.view_main_page_card_title)
    }

    fun articleTitle(): String {
        return page.displayTitle
    }

    fun articleSubtitle(): String? {
        return page.description
    }

    fun historyEntry(): HistoryEntry {
        return HistoryEntry(page.getPageTitle(wikiSite()), historyEntrySource())
    }
}
