package org.wikipedia.feed.aggregated

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.topread.TopRead
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.util.DateUtil
import org.wikipedia.util.L10nUtil
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
                    responses[appLangCode]?.randomOnThisDayEvent?.let {
                        outCards.add(OnThisDayCard(it, WikiSite.forLanguageCode(appLangCode), age))
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
                val deferredResponses = WikipediaApp.instance.languageState.appLanguageCodes.map { langCode ->
                    async {
                        val wikiSite = WikiSite.forLanguageCode(langCode)
                        val hasParentLanguageCode = !WikipediaApp.instance.languageState.getDefaultLanguageCode(langCode).isNullOrEmpty()
                        var feedContentResponse = ServiceFactory.getRest(wikiSite).getFeedFeatured(date.year, date.month, date.day)

                        feedContentResponse.randomOnThisDayEvent = feedContentResponse.onthisday?.random()

                        // TODO: This is a temporary fix for T355192
                        feedContentResponse = handleLanguageVariant(feedContentResponse, wikiSite, hasParentLanguageCode)

                        aggregatedClient.aggregatedResponses[langCode] = feedContentResponse
                        aggregatedClient.aggregatedResponseAge = age
                    }
                }

                deferredResponses.awaitAll()

                if (aggregatedClient.aggregatedResponses.containsKey(wiki.languageCode)) {
                    getCardFromResponse(aggregatedClient.aggregatedResponses, wiki, age, cards)
                }
                cb.success(cards)
            }
        }

        private suspend fun handleLanguageVariant(feedContentResponse: AggregatedFeedContent,
                                                  wikiSite: WikiSite,
                                                  hasParentLanguageCode: Boolean): AggregatedFeedContent {
            return withContext(Dispatchers.IO) {
                var response = feedContentResponse
                if (hasParentLanguageCode) {
                    val tfaDeferred = feedContentResponse.tfa?.let {
                        async { L10nUtil.getPagesForLanguageVariant(listOf(it), wikiSite, shouldUpdateExtracts = true).first() }
                    }

                    val topReadDeferred = feedContentResponse.topRead?.let {
                        async { L10nUtil.getPagesForLanguageVariant(it.articles, wikiSite) }
                    }

                    // Same logic in OnThisDayCardView and we only need to send one page to the event class.
                    val randomOnThisDayPageDeferred = feedContentResponse.randomOnThisDayEvent?.pages?.find { it.thumbnailUrl != null }?.let {
                        async { L10nUtil.getPagesForLanguageVariant(listOf(it), wikiSite) }
                    }

                    val tfaResponse = tfaDeferred?.await()
                    val topReadResponse = topReadDeferred?.await()
                    val randomOnThisDayPageResponse = randomOnThisDayPageDeferred?.await()

                    response = AggregatedFeedContent(
                        tfa = tfaResponse ?: feedContentResponse.tfa,
                        news = feedContentResponse.news,
                        topRead = topReadResponse?.let {
                            TopRead(
                                feedContentResponse.topRead.date,
                                it
                            )
                        } ?: feedContentResponse.topRead,
                        potd = feedContentResponse.potd,
                        onthisday = feedContentResponse.onthisday
                    ).apply {
                        randomOnThisDayEvent = OnThisDay.Event(
                            pages = randomOnThisDayPageResponse ?: feedContentResponse.randomOnThisDayEvent?.pages ?: emptyList(),
                            text = feedContentResponse.randomOnThisDayEvent?.text ?: "",
                            year = feedContentResponse.randomOnThisDayEvent?.year ?: 0
                        )
                    }
                }
                response
            }
        }
    }
}
