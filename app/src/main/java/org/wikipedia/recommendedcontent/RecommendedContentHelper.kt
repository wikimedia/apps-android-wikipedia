package org.wikipedia.recommendedcontent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.aggregated.AggregatedFeedContent
import org.wikipedia.feed.topread.TopRead
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil

object RecommendedContentHelper {

    suspend fun loadHistoryItems(): List<PageTitle> {
        return withContext(Dispatchers.IO) {
            AppDatabase.instance.historyEntryWithImageDao().filterHistoryItemsWithoutTime("").map {
                it.title
            }
        }
    }

    suspend fun loadRecentSearches(): List<PageTitle> {
        return withContext(Dispatchers.IO) {
            AppDatabase.instance.recentSearchDao().getRecentSearches().map {
                PageTitle(it.text, WikipediaApp.instance.wikiSite)
            }
        }
    }

    suspend fun loadTopRead(): List<PageTitle> {
        return withContext(Dispatchers.IO) {
            val wikiSite = WikipediaApp.instance.wikiSite
            val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(wikiSite.languageCode).isNullOrEmpty()
            val date = DateUtil.getUtcRequestDateFor(0)
            var feedContentResponse = ServiceFactory.getRest(wikiSite).getFeedFeatured(date.year, date.month, date.day)
            if (hasParentLanguageCode) {
                feedContentResponse.topRead?.let {
                    val topReadResponse = getPagesForLanguageVariant(it.articles, wikiSite)
                    feedContentResponse = AggregatedFeedContent(
                        tfa = feedContentResponse.tfa,
                        news = feedContentResponse.news,
                        topRead = TopRead(it.date, topReadResponse),
                        potd = feedContentResponse.potd,
                        onthisday = feedContentResponse.onthisday
                    )
                }
            }
            feedContentResponse.topRead?.articles?.map { it.getPageTitle(wikiSite) } ?: emptyList()
        }
    }

    // TODO: borrowed from FeedClient. Refactor this method to be more generic.
    private suspend fun getPagesForLanguageVariant(list: List<PageSummary>, wikiSite: WikiSite): List<PageSummary> {
        val newList = mutableListOf<PageSummary>()
        withContext(Dispatchers.IO) {
            val titles = list.joinToString(separator = "|") { it.apiTitle }
            // First, get the correct description from Wikidata directly.
            val wikiDataResponse = async {
                ServiceFactory.get(Constants.wikidataWikiSite)
                    .getWikidataDescription(titles = titles, sites = wikiSite.dbName(), langCode = wikiSite.languageCode)
            }
            // Second, fetch varianttitles from prop=info endpoint.
            val mwQueryResponse = async {
                ServiceFactory.get(wikiSite).getVariantTitlesByTitles(titles)
            }

            list.forEach { pageSummary ->
                // Find the correct display title from the varianttitles map, and insert the new page summary to the list.
                val displayTitle = mwQueryResponse.await().query?.pages?.find { StringUtil.addUnderscores(it.title) == pageSummary.apiTitle }?.varianttitles?.get(wikiSite.languageCode)
                val newPageSummary = pageSummary.apply {
                    val newDisplayTitle = displayTitle ?: pageSummary.displayTitle
                    this.titles = PageSummary.Titles(
                        canonical = pageSummary.apiTitle,
                        display = newDisplayTitle
                    )
                    this.description = wikiDataResponse.await().entities.values.firstOrNull {
                        it.labels[wikiSite.languageCode]?.value == newDisplayTitle
                    }?.descriptions?.get(wikiSite.languageCode)?.value ?: pageSummary.description
                }
                newList.add(newPageSummary)
            }
        }
        return newList
    }
}
