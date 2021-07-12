package org.wikipedia.feed.aggregated

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.util.DateUtil
import org.wikipedia.util.log.L

class AggregatedFeedContentClient {
    private val aggregatedResponses = mutableMapOf<String, AggregatedFeedContent>()
    private var aggregatedResponseAge = -1
    private val disposables = CompositeDisposable()

    class OnThisDayFeed(aggregatedClient: AggregatedFeedContentClient) :
        BaseClient(aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            for (appLangCode in WikipediaApp.getInstance().language().appLanguageCodes) {
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

    class InTheNews(aggregatedClient: AggregatedFeedContentClient) : BaseClient(aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            for (appLangCode in WikipediaApp.getInstance().language().appLanguageCodes) {
                if (responses.containsKey(appLangCode) && !FeedContentType.NEWS.langCodesDisabled.contains(appLangCode)) {
                    responses[appLangCode]?.news?.let {
                        outCards.add(NewsCard(it, age, WikiSite.forLanguageCode(appLangCode)))
                    }
                }
            }
        }
    }

    class FeaturedArticle(aggregatedClient: AggregatedFeedContentClient) : BaseClient(aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            for (appLangCode in WikipediaApp.getInstance().language().appLanguageCodes) {
                if (responses.containsKey(appLangCode) && !FeedContentType.FEATURED_ARTICLE.langCodesDisabled.contains(appLangCode)) {
                    responses[appLangCode]?.tfa?.let {
                        outCards.add(FeaturedArticleCard(it, age, WikiSite.forLanguageCode(appLangCode)))
                    }
                }
            }
        }
    }

    class TopReadArticles(aggregatedClient: AggregatedFeedContentClient) : BaseClient(aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            for (appLangCode in WikipediaApp.getInstance().language().appLanguageCodes) {
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

    class FeaturedImage(aggregatedClient: AggregatedFeedContentClient) : BaseClient(aggregatedClient) {
        override fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>,
                                         wiki: WikiSite,
                                         age: Int,
                                         outCards: MutableList<Card>) {
            if (responses.containsKey(wiki.languageCode())) {
                responses[wiki.languageCode()]?.potd?.let {
                    outCards.add(FeaturedImageCard(it, age, wiki))
                }
            }
        }
    }

    fun invalidate() {
        aggregatedResponseAge = -1
    }

    fun cancel() {
        disposables.clear()
    }

    abstract class BaseClient internal constructor(private val aggregatedClient: AggregatedFeedContentClient) : FeedClient {
        private lateinit var cb: FeedClient.Callback
        private lateinit var wiki: WikiSite
        private var age = 0

        abstract fun getCardFromResponse(responses: Map<String, AggregatedFeedContent>, wiki: WikiSite, age: Int, outCards: MutableList<Card>)

        override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
            this.cb = cb
            this.age = age
            this.wiki = wiki
            if (aggregatedClient.aggregatedResponseAge == age && aggregatedClient.aggregatedResponses.containsKey(wiki.languageCode())) {
                val cards = mutableListOf<Card>()
                getCardFromResponse(aggregatedClient.aggregatedResponses, wiki, age, cards)
                FeedCoordinator.postCardsToCallback(cb, cards)
            } else {
                requestAggregated()
            }
        }

        override fun cancel() {}

        private fun requestAggregated() {
            aggregatedClient.cancel()
            val date = DateUtil.getUtcRequestDateFor(age)
            aggregatedClient.disposables.add(Observable.fromIterable(FeedContentType.aggregatedLanguages)
                .flatMap({ lang ->
                        ServiceFactory.getRest(WikiSite.forLanguageCode(lang))
                            .getAggregatedFeed(date.year, date.month, date.day)
                            .subscribeOn(Schedulers.io())
                         }, { first, second -> Pair(first, second) })
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pairList ->
                    val cards = mutableListOf<Card>()
                    for (pair in pairList) {
                        val content = pair.second ?: continue
                        aggregatedClient.aggregatedResponses[WikiSite.forLanguageCode(pair.first).languageCode()] = content
                        aggregatedClient.aggregatedResponseAge = age
                    }
                    if (aggregatedClient.aggregatedResponses.containsKey(wiki.languageCode())) {
                        getCardFromResponse(aggregatedClient.aggregatedResponses, wiki, age, cards)
                    }
                    FeedCoordinator.postCardsToCallback(cb, cards)
                }) { caught ->
                    L.v(caught)
                    cb.error(caught)
                })
        }
    }
}
