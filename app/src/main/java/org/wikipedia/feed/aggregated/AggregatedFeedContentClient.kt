package org.wikipedia.feed.aggregated

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.topread.TopRead
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class AggregatedFeedContentClient {
    private val aggregatedResponses = mutableMapOf<String, AggregatedFeedContent>()
    private var aggregatedResponseAge = -1
    var clientJob: Job? = null

    class OnThisDayFeed(coroutineScope: CoroutineScope, aggregatedClient: AggregatedFeedContentClient) : BaseClient(coroutineScope, aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            for (appLangCode in WikipediaApp.instance.languageState.appLanguageCodes) {
                if (responses.containsKey(appLangCode) && !FeedContentType.ON_THIS_DAY.langCodesDisabled.contains(appLangCode)) {
                    responses[appLangCode]?.onthisday?.let {
                        if (it.isNotEmpty()) {
                            outCards.add(OnThisDayCard(it, WikiSite.forLanguageCode(appLangCode), age))
                        }
                    }
                }
            }
        }
    }

    class InTheNews(coroutineScope: CoroutineScope, aggregatedClient: AggregatedFeedContentClient) : BaseClient(coroutineScope, aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            for (appLangCode in WikipediaApp.instance.languageState.appLanguageCodes) {
                if (responses.containsKey(appLangCode) && !FeedContentType.NEWS.langCodesDisabled.contains(appLangCode)) {
                    responses[appLangCode]?.news?.let {
                        outCards.add(NewsCard(it, age, WikiSite.forLanguageCode(appLangCode)))
                    }
                }
            }
        }
    }

    class FeaturedArticle(coroutineScope: CoroutineScope, aggregatedClient: AggregatedFeedContentClient) : BaseClient(coroutineScope, aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            for (appLangCode in WikipediaApp.instance.languageState.appLanguageCodes) {
                if (responses.containsKey(appLangCode) && !FeedContentType.FEATURED_ARTICLE.langCodesDisabled.contains(appLangCode)) {
                    responses[appLangCode]?.tfa?.let {
                        outCards.add(FeaturedArticleCard(it, age, WikiSite.forLanguageCode(appLangCode)))
                    }
                }
            }
        }
    }

    class TopReadArticles(coroutineScope: CoroutineScope, aggregatedClient: AggregatedFeedContentClient) : BaseClient(coroutineScope, aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            for (appLangCode in WikipediaApp.instance.languageState.appLanguageCodes) {
                if (responses.containsKey(appLangCode) && !FeedContentType.TOP_READ_ARTICLES.langCodesDisabled.contains(appLangCode)) {
                    responses[appLangCode]?.topRead?.let {
                        outCards.add(
                            TopReadListCard(
                                it,
                                WikiSite.forLanguageCode(appLangCode)
                            )
                        )
                    }
                }
            }
        }
    }

    class FeaturedImage(coroutineScope: CoroutineScope, aggregatedClient: AggregatedFeedContentClient) : BaseClient(coroutineScope, aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            if (responses.containsKey(wiki.languageCode)) {
                responses[wiki.languageCode]?.potd?.let {
                    outCards.add(FeaturedImageCard(it, age, wiki))
                }
            }
        }
    }

    fun invalidate() {
        aggregatedResponseAge = -1
    }

    abstract class BaseClient internal constructor(
        private val coroutineScope: CoroutineScope,
        private val aggregatedClient: AggregatedFeedContentClient
    ) : FeedClient {
        private lateinit var cb: FeedClient.Callback
        private lateinit var wiki: WikiSite
        private var age = 0

        abstract fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>, wiki: WikiSite, age: Int, outCards: MutableList<Card>)

        override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
            this.cb = cb
            this.age = age
            this.wiki = wiki
            if (aggregatedClient.aggregatedResponseAge == age && aggregatedClient.aggregatedResponses.containsKey(wiki.languageCode)) {
                val cards = mutableListOf<Card>()
                getCardFromResponse(aggregatedClient.aggregatedResponses, wiki, age, cards)
                cb.success(cards)
            } else {
                requestAggregated()
            }
        }

        override fun cancel() {}

        private fun requestAggregated() {
            aggregatedClient.clientJob?.cancel()
            val date = DateUtil.getUtcRequestDateFor(age)
            aggregatedClient.clientJob = coroutineScope.launch(
                CoroutineExceptionHandler { _, caught ->
                    L.v(caught)
                    cb.error(caught)
                }
            ) {
                val cards = mutableListOf<Card>()
                FeedContentType.aggregatedLanguages.forEach { langCode ->
                    val wikiSite = WikiSite.forLanguageCode(langCode)
                    val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(langCode).isNullOrEmpty()
                    var feedContentResponse = ServiceFactory.getRest(wikiSite).getFeedFeatured(date.year, date.month, date.day)

                    // TODO: This is a temporary fix for T355192
                    if (hasParentLanguageCode) {
                        // TODO: Needs to update tfa and most read
                        feedContentResponse.tfa?.let {
                            val tfaResponse = getPageSummaryForLanguageVariant(it, wikiSite)
                            feedContentResponse = AggregatedFeedContent(
                                tfa = tfaResponse,
                                news = feedContentResponse.news,
                                topRead = feedContentResponse.topRead,
                                potd = feedContentResponse.potd,
                                onthisday = feedContentResponse.onthisday
                            )
                        }
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

                    aggregatedClient.aggregatedResponses[langCode] = feedContentResponse
                    aggregatedClient.aggregatedResponseAge = age
                }
                if (aggregatedClient.aggregatedResponses.containsKey(wiki.languageCode)) {
                    getCardFromResponse(aggregatedClient.aggregatedResponses, wiki, age, cards)
                }
                cb.success(cards)
            }
        }

        // TODO: This is a temporary fix for T355192
        private suspend fun getPageSummaryForLanguageVariant(pageSummary: PageSummary, wikiSite: WikiSite): PageSummary {
            var newPageSummary = pageSummary
            withContext(Dispatchers.IO) {
                // First, get the correct description from Wikidata directly.
                val wikiDataResponse = async {
                    ServiceFactory.get(Constants.wikidataWikiSite)
                        .getWikidataDescription(titles = pageSummary.apiTitle, sites = wikiSite.dbName(), langCode = wikiSite.languageCode)
                }
                // Second, fetch PageSummary endpoint instead of using the one with incorrect language variant (mostly from the feed endpoint).
                val pageSummaryResponse = async {
                    ServiceFactory.getRest(wikiSite).getPageSummary(null, pageSummary.apiTitle)
                }

                newPageSummary = pageSummaryResponse.await().apply {
                    description = wikiDataResponse.await().first?.getDescription(wikiSite.languageCode) ?: description
                }
            }
            return newPageSummary
        }

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
}
