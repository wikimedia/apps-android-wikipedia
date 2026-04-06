package org.wikipedia.onboarding.personalization

import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.StringUtil

class PersonalizationRepository {

    suspend fun getTopics(langCode: String): List<OnboardingTopic> {
        val allMsgKey = OnboardingTopics.all.joinToString("|") { it.msgKey }
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getMessages(messages = allMsgKey, args = null, lang = langCode)
        val translations = response.query?.allmessages
            ?.filterNot { it.missing }
            ?.associate { it.name to it.content }
            .orEmpty()
        return OnboardingTopics.all.map { topic ->
            topic.copy(displayTitle = translations[topic.msgKey] ?: "no")
        }
    }

    suspend fun getArticlesBytTopic(topic: String): List<PageTitle> {
        val searchTerm = "articletopic:$topic"
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getArticlesByTopic(searchTerm, 25)
        val pageList = response.query?.pages
            ?.map { page ->
                PageTitle(
                    text = page.title,
                    wiki = WikipediaApp.instance.wikiSite,
                    thumbUrl = page.thumbUrl(),
                    description = page.description,
                    displayText = page.displayTitle(WikipediaApp.instance.wikiSite.languageCode)
                )
            } ?: emptyList()
        return pageList
    }

    suspend fun loadInitialArticles(selectedItems: List<PageTitle>): List<PageTitle> {
        val maxItems = 20
        val results = mutableListOf<PageTitle>()
        results.addAll(selectedItems)

        if (results.size < maxItems) {
            // get most recent history entries
            val historyTitles = AppDatabase.instance.historyEntryWithImageDao().findEntryForReadMore(maxItems, 0)
                .map { it.title }
            // and a random sampling of reading list pages
            val readingListTitles = AppDatabase.instance.readingListPageDao().getPagesByRandom(maxItems)
                .map { ReadingListPage.toPageTitle(it) }
            // take the two lists and interleave them
            for (i in 0 until maxItems) {
                if (i < historyTitles.size && !results.contains(historyTitles[i])) results.add(historyTitles[i])
                if (i < readingListTitles.size && !results.contains(readingListTitles[i])) results.add(readingListTitles[i])
            }
            // remove non-main namespace articles, or Main page
            results.removeAll { it.isMainPage || it.namespace() != Namespace.MAIN }
        }

        // If there are still VERY few items, include a few random articles.
        val maxRandomItems = 6
        if (results.size < maxRandomItems) {
            for (i in results.size until maxRandomItems) {
                val title = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getRandomSummary()
                    .getPageTitle(WikipediaApp.instance.wikiSite)
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
}
