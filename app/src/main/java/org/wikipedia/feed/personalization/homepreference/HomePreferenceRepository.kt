package org.wikipedia.feed.personalization.homepreference

import android.content.Context
import org.wikipedia.R
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import java.time.LocalDate

class HomePreferenceRepository(
    private val context: Context,
    private val historyEntryWithImageDao: HistoryEntryWithImageDao,
    private val wikiSite: WikiSite
) {
    suspend fun getCommunityPreviewContent(): List<HomePreferenceContent> {
        val currentDate = LocalDate.now()

        val response = ServiceFactory.getRest(wikiSite).getFeedFeatured(
            year = currentDate.year.toString(),
            month = "%02d".format(currentDate.monthValue),
            day = "%02d".format(currentDate.dayOfMonth),
            lang = wikiSite.languageCode
        )

        val featuredArticle = response.tfa?.let { article ->
            HomePreferenceContent(
                title = article.displayTitle,
                description = article.description,
                imageUrl = article.thumbnailUrl,
                tag = context.getString(wikiSite.languageCode, R.string.view_featured_article_card_title)
            )
        }

        val pictureOfTheDay = response.potd?.let { potd ->
            HomePreferenceContent(
                title = null,
                description = potd.description.text,
                imageUrl = potd.thumbnailUrl,
                tag = context.getString(wikiSite.languageCode, R.string.view_featured_image_card_title)
            )
        }

        val topNewsItem = response.news?.firstOrNull()?.let { newsItem ->
            HomePreferenceContent(
                title = null,
                description = StringUtil.removeHTMLTags(newsItem.story),
                imageUrl = newsItem.thumbUrl(),
                tag = context.getString(wikiSite.languageCode, R.string.view_card_news_title)
            )
        }

        return listOfNotNull(
            featuredArticle,
            pictureOfTheDay,
            topNewsItem
        )
    }

    suspend fun getPersonalizedPreviewContent(
        selectedArticles: Set<PageTitle>,
        contentByTopic: Map<String, List<HomePreferenceContent>>,
    ): List<HomePreferenceContent> {
        if (contentByTopic.isNotEmpty()) {
            return sampleAcrossTopics(contentByTopic = contentByTopic)
        }

        if (selectedArticles.isNotEmpty()) {
            return fetchMoreLike(seeds = selectedArticles.map { it.prefixedText })
        }

        val recentHistoryEntries = historyEntryWithImageDao.getMostRecentEntriesWithImage(3)
        if (recentHistoryEntries.size >= 3) {
            return fetchMoreLike(seeds = recentHistoryEntries.map { it.apiTitle })
        }

        return listOf()
    }

    // has count logic for cases where user has selected less than 3 topics
    // as we need 3 articles to show in the preview, we need to distribute them across the selected topics
    private fun sampleAcrossTopics(
        contentByTopic: Map<String, List<HomePreferenceContent>>,
        totalCount: Int = 3,
    ): List<HomePreferenceContent> {
        val topicIds = contentByTopic.keys.toList().reversed()

        val baseLimit = totalCount / topicIds.size
        val remainder = totalCount % topicIds.size

        return topicIds.flatMapIndexed { index, topic ->
            val count = baseLimit + if (index < remainder) 1 else 0
            contentByTopic[topic].orEmpty().take(count)
        }
    }

    private suspend fun fetchMoreLike(seeds: List<String>): List<HomePreferenceContent> {
        if (seeds.isEmpty()) return emptyList()
        val moreLikeSearchTerm = "morelike:${seeds.take(3).joinToString("|")}"
        val response = ServiceFactory.get(wikiSite).searchMoreLike(searchTerm = moreLikeSearchTerm, gsrLimit = 3, piLimit = 3)
        return response.query?.pages?.map { page ->
            HomePreferenceContent(
                title = page.displayTitle(wikiSite.languageCode),
                description = page.description,
                imageUrl = page.thumbUrl(),
                tag = null
            )
        } ?: emptyList()
    }

    fun savePreference(preferenceType: HomePreferenceType) {
        Prefs.homePreferenceSelection = preferenceType
    }
}
