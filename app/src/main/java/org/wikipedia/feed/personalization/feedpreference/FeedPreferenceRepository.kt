package org.wikipedia.feed.personalization.feedpreference

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.personalization.db.dao.InterestArticleDao
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import java.time.LocalDate

class FeedPreferenceRepository(
    private val interestArticleDao: InterestArticleDao,
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

    suspend fun getInterests(): List<FeedPreferenceContent> {
        val articles = interestArticleDao.getArticlesWithTopic(wikiSite.languageCode)

        return articles.map {
            FeedPreferenceContent(
                title = it.article.displayTitle,
                description = it.article.description,
                imageUrl = it.article.thumbUrl,
                tag = it.topic.topicLabel
            )
        }
    }

    fun saveFeedPreferenceSelection(preferenceType: FeedPreferenceType) {
        Prefs.exploreFeedPreferenceSelection = preferenceType
    }
}
