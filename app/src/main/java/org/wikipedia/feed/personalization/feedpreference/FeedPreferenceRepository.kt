package org.wikipedia.feed.personalization.feedpreference

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.personalization.interest.OnboardingTopic
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import java.time.LocalDate

class FeedPreferenceRepository(
    private val historyEntryWithImageDao: HistoryEntryWithImageDao,
    private val wikiSite: WikiSite
) {
    suspend fun getCommunityContent(): List<FeedPreferenceContent> {
        val currentDate = LocalDate.now()

        val response = ServiceFactory.getRest(wikiSite).getFeedFeatured(
            year = currentDate.year.toString(),
            month = "%02d".format(currentDate.monthValue),
            day = "%02d".format(currentDate.dayOfMonth),
            lang = wikiSite.languageCode
        )

        val featuredArticle = response.tfa?.let { article ->
            FeedPreferenceContent(
                title = article.displayTitle,
                description = article.description,
                imageUrl = article.thumbnailUrl,
                tag = "Featured Article" // TODO: use localized string resource
            )
        }

        val pictureOfTheDay = response.potd?.let { potd ->
            FeedPreferenceContent(
                title = null,
                description = potd.description.text,
                imageUrl = potd.thumbnailUrl,
                tag = "Picture of the Day" // TODO: use localized string resource
            )
        }

        val inTheNewsArticle = response.news?.first().let { newsItem ->
            FeedPreferenceContent(
                title = null,
                description = StringUtil.removeHTMLTags(newsItem?.story),
                imageUrl = newsItem?.thumbUrl(),
                tag = "In the News" // TODO: use localized string resource
            )
        }

        return listOfNotNull(
            featuredArticle,
            pictureOfTheDay,
            inTheNewsArticle
        )
    }

    suspend fun getInterests(
        selectedTopics: List<OnboardingTopic>,
        selectedArticles: Set<PageTitle>
    ): List<FeedPreferenceContent> {
        // TODO: Confirm behavior with product
        if (selectedTopics.isNotEmpty()) {
            val content = selectedTopics.take(3).flatMap { topic ->
                val response = ServiceFactory.get(wikiSite).getArticlesByTopic(articleTopics = topic.queryTopicId, limit = 1)
                response.query?.pages?.map { page ->
                    FeedPreferenceContent(
                        title = page.title,
                        description = page.description,
                        imageUrl = page.thumbUrl(),
                        tag = topic.displayTitle
                    )
                } ?: emptyList()
            }
            return content
        }

        if (selectedArticles.isNotEmpty()) {
            val moreLikeSearchTerm = "morelike:${selectedArticles.take(3).joinToString("|") { it.prefixedText }}"
            val response = ServiceFactory.get(wikiSite).searchMoreLike(searchTerm = moreLikeSearchTerm, gsrLimit = 3, piLimit = 3)
            val content = response.query?.pages?.map { page ->
                FeedPreferenceContent(
                    title = page.title,
                    description = page.description,
                    imageUrl = page.thumbUrl(),
                    tag = null
                )
            } ?: emptyList()
            return content
        }

        val readingHistory = historyEntryWithImageDao.getMostRecentEntriesWithImage(3)
        if (readingHistory.size >= 3) {
            val moreLikeSearchTerm = "morelike:${readingHistory.take(3).joinToString("|") { it.apiTitle }}"
            val response = ServiceFactory.get(wikiSite).searchMoreLike(searchTerm = moreLikeSearchTerm, gsrLimit = 3, piLimit = 3)
            val content = response.query?.pages?.map { page ->
                FeedPreferenceContent(
                    title = page.title,
                    description = page.description,
                    imageUrl = page.thumbUrl(),
                    tag = null
                )
            } ?: emptyList()
            return content
        }

        return listOf()
    }

    fun saveFeedPreferenceSelection(preferenceType: FeedPreferenceType) {
        Prefs.exploreFeedPreferenceSelection = preferenceType
    }
}
