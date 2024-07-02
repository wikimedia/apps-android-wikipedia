package org.wikipedia.feed.becauseyouread

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class BecauseYouReadClient(
    private val coroutineScope: CoroutineScope
) : FeedClient {
    private var clientJob: Job? = null
    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        clientJob = coroutineScope.launch(
            CoroutineExceptionHandler { _, caught ->
                L.v(caught)
                cb.success(emptyList())
            }
        ) {
            val entries = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(age, context.resources.getInteger(R.integer.article_engagement_threshold_sec))
            if (entries.size <= age) {
                cb.success(emptyList())
            } else {
                val entry = entries[age]
                val langCode = entry.title.wikiSite.languageCode
                // If the language code has a parent language code, it means set "Accept-Language" will slow down the loading time of /page/related
                // TODO: remove when https://phabricator.wikimedia.org/T271145 is resolved.
                val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(langCode).isNullOrEmpty()
                val searchTerm = StringUtil.removeUnderscores(entry.title.prefixedText)
                val relatedPages = mutableListOf<PageSummary>()

                val moreLikeResponse = ServiceFactory.get(entry.title.wikiSite).searchMoreLike("morelike:$searchTerm",
                    Constants.SUGGESTION_REQUEST_ITEMS, Constants.SUGGESTION_REQUEST_ITEMS)

                val headerPage = PageSummary(entry.title.displayText, entry.title.prefixedText, entry.title.description,
                    entry.title.extract, entry.title.thumbUrl, langCode)

                moreLikeResponse.query?.pages?.forEach {
                    if (it.title != searchTerm) {
                        if (hasParentLanguageCode) {
                            val pageSummary = ServiceFactory.getRest(entry.title.wikiSite).getPageSummary(entry.referrer, it.title)
                            relatedPages.add(pageSummary)
                        } else {
                            relatedPages.add(PageSummary(it.displayTitle(langCode), it.title, it.description,
                                it.extract, it.thumbUrl(), langCode))
                        }
                    }
                }

                cb.success(
                    if (relatedPages.isEmpty()) emptyList()
                    else listOf(toBecauseYouReadCard(relatedPages, headerPage, entry.title.wikiSite))
                )
            }
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }

    private fun toBecauseYouReadCard(results: List<PageSummary>, pageSummary: PageSummary, wikiSite: WikiSite): BecauseYouReadCard {
        val itemCards = results.map { BecauseYouReadItemCard(it.getPageTitle(wikiSite)) }
        return BecauseYouReadCard(pageSummary.getPageTitle(wikiSite), itemCards)
    }
}
