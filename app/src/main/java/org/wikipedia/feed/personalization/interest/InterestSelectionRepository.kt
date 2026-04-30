package org.wikipedia.feed.personalization.interest

import androidx.room.Transaction
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.personalization.db.dao.InterestArticleDao
import org.wikipedia.feed.personalization.db.dao.InterestTopicDao
import org.wikipedia.feed.personalization.db.entity.InterestArticle
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.feed.personalization.topics.OnboardingTopics
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.db.ReadingListPageDao
import org.wikipedia.util.StringUtil

class InterestSelectionRepository(
    private val interestTopicDao: InterestTopicDao,
    private val interestArticleDao: InterestArticleDao,
    private val historyEntryWithImageDao: HistoryEntryWithImageDao,
    private val readingListPageDao: ReadingListPageDao,
    val wikiSite: WikiSite
) {

    suspend fun getArticlesByTopic(topic: String): List<PageTitle> {
        val searchTerm = "articletopic:$topic"
        val response = ServiceFactory.get(wikiSite).getArticlesByTopic(searchTerm, 25)
        val pageList = response.query?.pages
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
        val historyTitles = historyEntryWithImageDao.findEntryForReadMore(maxItems, 0)
            .map { it.title }
        // and a random sampling of reading list pages
        val readingListTitles = readingListPageDao.getPagesByRandom(maxItems)
            .map { ReadingListPage.toPageTitle(it) }
        // take the two lists and interleave them
        for (i in 0 until maxItems) {
            if (i < historyTitles.size && !results.contains(historyTitles[i])) results.add(historyTitles[i])
            if (i < readingListTitles.size && !results.contains(readingListTitles[i])) results.add(readingListTitles[i])
        }
        // remove non-main namespace articles, or Main page
        results.removeAll { it.isMainPage || it.namespace() != Namespace.MAIN }

        // If there are still VERY few items, include a few random articles.
        val maxRandomItems = 6
        if (results.size < maxRandomItems) {
            for (i in results.size until maxRandomItems) {
                val title = ServiceFactory.getRest(wikiSite).getRandomSummary()
                    .getPageTitle(wikiSite)
                if (!results.contains(title)) {
                    results.add(title)
                }
            }
        }

        // Hydrate titles, if necessary
        val itemsNeedingCall = results
            .filter { it.description.isNullOrEmpty() || it.thumbUrl.isNullOrEmpty() }
            .groupBy { it.wikiSite }
        itemsNeedingCall.keys.forEach { site ->
            val pageList = ServiceFactory.get(site).getInfoByPageIdsOrTitles(titles = itemsNeedingCall[site]?.joinToString("|") { it.prefixedText })
                .query?.pages.orEmpty()
            pageList.forEach { page ->
                results.find { it.prefixedText == StringUtil.addUnderscores(page.title) }?.let { title ->
                    title.description = page.description
                    title.thumbUrl = page.thumbUrl()
                }
            }
        }

        return results.distinctBy { it.prefixedText }
    }

    suspend fun getPersistedTopics(lang: String): List<OnboardingTopic> {
        return interestTopicDao.getAll(lang).mapNotNull { entity ->
            OnboardingTopics.all.find { it.topicId == entity.topicId }
        }
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

    suspend fun saveTopic(topic: OnboardingTopic, lang: String) {
        interestTopicDao.insert(
            interestTopic = InterestTopic(
                topicId = topic.topicId,
                queryTopicId = topic.queryTopicId,
                lang = lang
            )
        )
    }

    suspend fun deleteTopic(topic: OnboardingTopic, lang: String) {
        interestTopicDao.delete(
            interestTopic = InterestTopic(
                topicId = topic.topicId,
                queryTopicId = topic.queryTopicId,
                lang = lang
            )
        )
    }

    suspend fun saveArticle(article: PageTitle, lang: String, topic: OnboardingTopic?) {
        interestArticleDao.insert(
            interestArticle = InterestArticle(
                apiTitle = article.prefixedText,
                lang = lang,
                namespace = article.namespace(),
                displayTitle = article.displayText,
                description = article.description.orEmpty(),
                thumbUrl = article.thumbUrl.orEmpty(),
                topicId = topic?.topicId,
                topicLang = lang
            )
        )
    }

    suspend fun deleteArticle(article: PageTitle, lang: String, topic: OnboardingTopic?) {
        interestArticleDao.delete(
            interestArticle = InterestArticle(
                apiTitle = article.prefixedText,
                lang = lang,
                namespace = article.namespace(),
                displayTitle = article.displayText,
                description = article.description.orEmpty(),
                thumbUrl = article.thumbUrl.orEmpty(),
                topicId = topic?.topicId,
                topicLang = lang
            )
        )
    }

    @Transaction
    suspend fun deleteAllInterests() {
        interestTopicDao.deleteAll()
        interestArticleDao.deleteAll()
    }
}
