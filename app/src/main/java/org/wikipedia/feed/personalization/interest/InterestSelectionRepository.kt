package org.wikipedia.feed.personalization.interest

import androidx.room.Transaction
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.personalization.db.dao.InterestArticleDao
import org.wikipedia.feed.personalization.db.dao.InterestTopicDao
import org.wikipedia.feed.personalization.db.entity.InterestArticle
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.feed.random.RandomClient
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.db.ReadingListPageDao
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.StringUtil

class InterestSelectionRepository(
    private val interestTopicDao: InterestTopicDao,
    private val interestArticleDao: InterestArticleDao,
    private val historyEntryWithImageDao: HistoryEntryWithImageDao,
    private val readingListPageDao: ReadingListPageDao,
    val wikiSite: WikiSite
) {

    suspend fun getArticlesByTopic(topic: String): List<PageTitle> {
        val response = ServiceFactory.get(wikiSite).getArticlesByTopic(
            "articletopic:$topic^95",
            limit = 20,
            profile = "classic_noboostlinks",
            sort = "random"
        )
        val pageList = response.query?.pages
            ?.filter { it.pageProps?.disambiguation == null } // Filter out disambiguation pages
            ?.sortedBy { it.index } // Sort by index, as reported by the API
            ?.sortedBy { it.thumbUrl().isNullOrEmpty() } // Sort by whether it has a thumbnail
            ?.map { page ->
                PageTitle(
                    text = page.title,
                    wiki = wikiSite,
                    thumbUrl = page.thumbUrl(),
                    description = page.description,
                    displayText = page.displayTitle(wikiSite.languageCode)
                )
            } ?: emptyList()
        return pageList
    }

    suspend fun loadInitialArticles(): List<PageTitle> {
        val maxItems = 20
        val results = mutableListOf<PageTitle>()

        // get most recent history entries
        val historyTitles = historyEntryWithImageDao.findEntryForReadMore(maxItems, 0, wikiSite.languageCode)
            .map { it.title }
        // and the most recent reading list pages
        val readingListTitles = readingListPageDao.getMostRecentSavedPagesByLang(wikiSite.languageCode, maxItems)
            .map { ReadingListPage.toPageTitle(it) }
        // take the two lists and interleave them
        for (i in 0 until maxItems) {
            if (i < historyTitles.size && !results.contains(historyTitles[i])) results.add(historyTitles[i])
            if (i < readingListTitles.size && !results.contains(readingListTitles[i])) results.add(readingListTitles[i])
        }
        // remove non-main namespace articles, or Main page
        results.removeAll { it.isMainPage || it.namespace() != Namespace.MAIN }

        // Hydrate titles, if necessary
        val itemsNeedingCall = results
            .filter { it.description.isNullOrEmpty() || it.thumbUrl.isNullOrEmpty() }
            .groupBy { it.wikiSite }
        itemsNeedingCall.keys.forEach { site ->
            val pageList = ServiceFactory.get(site).getInfoByPageIdsOrTitles(titles = itemsNeedingCall[site]?.joinToString("|") { it.prefixedText })
                .query?.pages.orEmpty()
            pageList.forEach { page ->
                results.find { it.prefixedText == StringUtil.addUnderscores(page.title) }?.let { title ->
                    title.displayText = page.displayTitle(site.languageCode)
                    title.description = page.description
                    title.thumbUrl = page.thumbUrl()
                }
            }
        }

        // If there are still VERY few items, include a few random articles.
        val maxRandomItems = 6
        if (results.size < maxRandomItems) {
            results.addAll(RandomClient.getRandomPages(wikiSite, maxRandomItems))
        }

        return results.distinctBy { it.prefixedText }
    }

    suspend fun getPersistedTopics(): List<OnboardingTopic> {
        return interestTopicDao.getAll().mapNotNull { entity ->
            ArticleTopics.all.find { it.topicId == entity.topicId }
        }.map { OnboardingTopic(it) }
    }

    suspend fun getPersistedArticles(lang: String): List<PageTitle> {
        return interestArticleDao.getAll(lang).map { entity ->
            PageTitle(
                text = entity.apiTitle,
                wiki = wikiSite,
                thumbUrl = entity.thumbUrl,
                description = entity.description,
                displayText = entity.displayTitle
            )
        }
    }

    suspend fun saveTopic(topic: OnboardingTopic) {
        interestTopicDao.insert(InterestTopic(topic.topic.topicId))
    }

    suspend fun deleteTopic(topic: OnboardingTopic) {
        interestTopicDao.delete(InterestTopic(topic.topic.topicId))
    }

    suspend fun saveArticle(article: PageTitle, lang: String) {
        interestArticleDao.insert(
            interestArticle = InterestArticle(
                apiTitle = article.prefixedText,
                lang = lang,
                namespace = article.namespace(),
                displayTitle = article.displayText,
                description = article.description.orEmpty(),
                thumbUrl = article.thumbUrl.orEmpty()
            )
        )
    }

    suspend fun deleteArticle(article: PageTitle, lang: String) {
        interestArticleDao.delete(
            interestArticle = InterestArticle(
                apiTitle = article.prefixedText,
                lang = lang,
                namespace = article.namespace(),
                displayTitle = article.displayText,
                description = article.description.orEmpty(),
                thumbUrl = article.thumbUrl.orEmpty()
            )
        )
    }

    @Transaction
    suspend fun deleteAllInterests() {
        interestTopicDao.deleteAll()
        interestArticleDao.deleteAll()
    }
}
