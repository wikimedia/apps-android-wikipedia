package org.wikipedia.feed

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.aggregated.AggregatedFeedContent
import org.wikipedia.feed.dayheader.DayHeaderCard
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.mainpage.MainPageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.searchbar.SearchCard
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.util.DateUtil

class FeedPagingSource : PagingSource<Int, Card>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Card> {
        val age = params.key ?: 0

        return try {
            val cards = mutableListOf<Card>()
            val date = DateUtil.getUtcRequestDateFor(age)
            val appLangCodes = WikipediaApp.instance.languageState.appLanguageCodes
            val defaultFeaturedWiki = WikiSite.forLanguageCode(appLangCodes.firstOrNull() ?: "en")

            if (age == 0) {
                cards.add(SearchCard())
            }

            cards.add(DayHeaderCard(age))

            if (age == 0) {
//                AnnouncementCard.getActiveCard()?.let { cards.add(it) }
            }

            // Fetch all languages concurrently and collect responses into a map.
            val responses: Map<String, AggregatedFeedContent> = coroutineScope {
                val deferred = appLangCodes.map { langCode ->
                    async {
                        val wikiSite = WikiSite.forLanguageCode(langCode)
                        val resp = ServiceFactory.getRest(wikiSite)
                            .getFeedFeatured(date.year, date.month, date.day, langCode)
                        // set a random on-this-day event like the previous client did
                        resp.randomOnThisDayEvent = resp.onthisday?.random()
                        langCode to resp
                    }
                }
                deferred.awaitAll().toMap()
            }

            // Build cards following previous AggregatedFeedContentClient rules:
            // - For each app language, add OnThisDay, News, FeaturedArticle, TopRead if enabled.
            // - For FeaturedImage, add POTD for the default wiki language.
            for (appLangCode in appLangCodes) {
                val resp = responses[appLangCode] ?: continue

                if (!FeedContentType.ON_THIS_DAY.langCodesDisabled.contains(appLangCode)) {
                    resp.randomOnThisDayEvent?.let {
                        cards.add(OnThisDayCard(it, WikiSite.forLanguageCode(appLangCode), age))
                    }
                }

                if (!FeedContentType.NEWS.langCodesDisabled.contains(appLangCode)) {
                    resp.news?.let {
                        cards.add(NewsCard(it, age, WikiSite.forLanguageCode(appLangCode)))
                    }
                }

                if (!FeedContentType.FEATURED_ARTICLE.langCodesDisabled.contains(appLangCode)) {
                    resp.tfa?.let {
                        cards.add(FeaturedArticleCard(it, age, WikiSite.forLanguageCode(appLangCode)))
                    }
                }

                if (!FeedContentType.TOP_READ_ARTICLES.langCodesDisabled.contains(appLangCode)) {
                    resp.topRead?.let {
                        cards.add(TopReadListCard(it, WikiSite.forLanguageCode(appLangCode)))
                    }
                }

                // Add main page card for each language
                if (!FeedContentType.MAIN_PAGE.langCodesDisabled.contains(appLangCode)) {
                    cards.add(MainPageCard(WikiSite.forLanguageCode(appLangCode)))
                }
            }

            // Featured image: follow previous behavior of using the provided wiki; here use defaultFeaturedWiki.
            responses[defaultFeaturedWiki.languageCode]?.potd?.let {
                cards.add(FeaturedImageCard(it, age, defaultFeaturedWiki))
            }

            // Add random card
//            if (Prefs.feedCardsEnabled.contains(FeedContentType.RANDOM)) {
//                cards.add(RandomCard(defaultFeaturedWiki))
//            }

            LoadResult.Page(
                data = cards,
                prevKey = if (age > 0) age - 1 else null,
                nextKey = age + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Card>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
