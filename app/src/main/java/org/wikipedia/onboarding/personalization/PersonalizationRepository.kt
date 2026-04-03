package org.wikipedia.onboarding.personalization

import androidx.core.net.toUri
import kotlinx.coroutines.delay
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.StringUtil
import kotlin.collections.forEach
import kotlin.collections.orEmpty

class PersonalizationRepository {

    // TODO: add actual api call if needed otherwise go with static data
    suspend fun getTopics(langCode: String): List<OnboardingTopic> {
        val allMsgKey = OnboardingTopics.all.joinToString("|") { it.msgKey }
        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getMessages(messages = allMsgKey, args = null, lang = langCode)
        val translations = response.query?.allmessages
            ?.filterNot { it.missing }
            ?.associate { it.name to it.content }
            .orEmpty()

        return OnboardingTopics.all.map { topic ->
            topic.copy(displayTitle = translations[topic.msgKey] ?: topic.displayTitle)
        }
    }

    // TODO: add actual api call
    suspend fun getArticlesBytTopic(topics: List<String>): List<PageTitle> {
        println("orange loading articles for topics $topics...")
        delay(5000) // simulate network delay
        val site = WikiSite("https://en.wikipedia.org/".toUri(), "en")
        val titles = listOf(
            PageTitle(text = "Psychology of art", wiki = site, thumbUrl = "foo.jpg", description = "Study of mental functions and behaviors", displayText = null),
            PageTitle(text = "Industrial design", wiki = site, thumbUrl = "foo.jpg", description = "Process of design applied to physical products", displayText = null),
            PageTitle(text = "Dufourspitze", wiki = site, thumbUrl = "foo.jpg", description = "Highest mountain in Switzerland", displayText = null),
            PageTitle(text = "Sample title without description", wiki = site, thumbUrl = "foo.jpg", description = "", displayText = null),
            PageTitle(text = "Sample title without thumbnail", wiki = site, thumbUrl = "", description = "Sample description", displayText = null),
            PageTitle(text = "Octagon house", wiki = site, thumbUrl = "foo.jpg", description = "North American house style briefly popular in the 1850s", displayText = null),
            PageTitle(text = "Barack Obama", wiki = site, thumbUrl = "foo.jpg", description = "President of the United States from 2009 to 2017", displayText = null),
        )
        return titles
    }

    suspend fun loadInitialArticles(selectedItems: List<PageTitle>): List<PageTitle> {
        println("orange loading initial articles...")
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
