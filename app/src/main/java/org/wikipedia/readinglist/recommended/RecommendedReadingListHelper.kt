package org.wikipedia.readinglist.recommended

import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.NewRecommendedReadingListEvent
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.RecommendedPage
import org.wikipedia.settings.Prefs
import kotlin.math.ceil

object RecommendedReadingListHelper {

    private const val SUGGESTION_REQUEST_ITEMS = 50
    private const val MAX_RETRIES = 10

    suspend fun generateRecommendedReadingList(shouldExpireOldPages: Boolean = false): List<RecommendedPage> {
        if (!Prefs.isRecommendedReadingListEnabled) {
            return emptyList()
        }
        var numberOfArticles = Prefs.recommendedReadingListArticlesNumber
        if (numberOfArticles <= 0) {
            return emptyList()
        }

        if (shouldExpireOldPages) {
            // Expire old recommended pages
            AppDatabase.instance.recommendedPageDao().expireOldRecommendedPages()
            Prefs.resetRecommendedReadingList = false
        }

        // Check if amount of new articles to see if we really need to generate a new list
        val existingRecommendedPages = AppDatabase.instance.recommendedPageDao().getNewRecommendedPages()
        if (existingRecommendedPages.size >= numberOfArticles) {
            return existingRecommendedPages.take(numberOfArticles)
        } else {
            // If the number of articles is less than the number of new articles, adjust the number of articles
            numberOfArticles -= existingRecommendedPages.size
        }

        // Step 1: get titles from the source by number of articles
        val titles = when (Prefs.recommendedReadingListSource) {
            RecommendedReadingListSource.INTERESTS -> {
                Prefs.recommendedReadingListInterests.shuffled().take(numberOfArticles)
            }
            RecommendedReadingListSource.READING_LIST -> {
                AppDatabase.instance.readingListPageDao().getPagesByRandom(numberOfArticles).map {
                    PageTitle(it.apiTitle, it.wiki).apply {
                        namespace = it.namespace.name
                        displayText = it.displayTitle
                        description = it.description
                        thumbUrl = it.thumbUrl
                    }
                }
            }
            RecommendedReadingListSource.HISTORY -> {
                AppDatabase.instance.historyEntryDao().getHistoryEntriesByRandom(numberOfArticles).map {
                    it.title
                }
            }
        }

        // If no titles are found, return an empty list
        if (titles.isEmpty()) {
            return emptyList()
        }

        // Step 2: combine the titles with the offsite from Prefs.
        val sourcesWithOffset = mutableListOf<SourceWithOffset>()
        titles.forEach { pageTitle ->
            val offset = Prefs.recommendedReadingListSourceTitlesWithOffset.find { it.title == pageTitle.text }?.offset ?: 0
            sourcesWithOffset.add(SourceWithOffset(pageTitle.text, pageTitle.wikiSite.languageCode, offset))
        }

        // This is the default take size for the response per source title.
        // If the number of articles is less than the default take size, we need to adjust it.
        var defaultTakeSize = 1

        if (sourcesWithOffset.size < numberOfArticles) {
            defaultTakeSize = ceil(numberOfArticles.toDouble() / sourcesWithOffset.size).toInt()
        }

        val newSourcesWithOffset = mutableListOf<SourceWithOffset>()
        val newRecommendedPages = mutableListOf<RecommendedPage>()
        // Step 3: uses morelike API to get recommended article, but excludes the articles from database,
        // and update the offset everytime when re-query the API.
        sourcesWithOffset.forEach { sourceWithOffset ->
            var recommendedPages = mutableListOf<PageTitle>()
            var retryCount = 0
            var offset = sourceWithOffset.offset
            while ((recommendedPages.isEmpty() || recommendedPages.size < defaultTakeSize) && retryCount < MAX_RETRIES) {
                recommendedPages.addAll(getRecommendedPage(sourceWithOffset, offset, defaultTakeSize))

                // Bump the offset if the size of the list does not meet the default take size.
                if (recommendedPages.isEmpty() || recommendedPages.size < defaultTakeSize) {
                    offset += SUGGESTION_REQUEST_ITEMS
                }
                retryCount++
            }

            // Step 4: if the recommended page is generated, insert it into the database,
            recommendedPages.forEach {
                val finalRecommendedPage = RecommendedPage(
                    wiki = it.wikiSite,
                    lang = it.wikiSite.languageCode,
                    namespace = it.namespace(),
                    apiTitle = it.prefixedText,
                    displayTitle = it.displayText,
                    description = it.description,
                    thumbUrl = it.thumbUrl
                )
                // Update the offset in the source list
                newSourcesWithOffset.add(SourceWithOffset(sourceWithOffset.title, sourceWithOffset.language, offset))

                // Insert the recommended page into the database
                AppDatabase.instance.recommendedPageDao().insert(finalRecommendedPage)
                newRecommendedPages.add(finalRecommendedPage)
            }
        }

        val finalList = (newRecommendedPages + existingRecommendedPages).distinct().toMutableList()

        // Step 5: if the list is empty or nothing new, we can get the expired pages from the database
        if (finalList.size < Prefs.recommendedReadingListArticlesNumber) {
            val pagesShouldGetFromExpire = Prefs.recommendedReadingListArticlesNumber - finalList.size
            val expiredPages = AppDatabase.instance.recommendedPageDao().getExpiredRecommendedPages(pagesShouldGetFromExpire, finalList.map { it.apiTitle })
            expiredPages.map {
                it.status = 0
                it
            }.apply {
                AppDatabase.instance.recommendedPageDao().updateAll(this)
                finalList.addAll(this)
            }
        }
        Prefs.isNewRecommendedReadingListGenerated = true
        FlowEventBus.post(NewRecommendedReadingListEvent())
        return finalList.take(Prefs.recommendedReadingListArticlesNumber)
    }

    private suspend fun getRecommendedPage(sourceWithOffset: SourceWithOffset, offset: Int, takeSize: Int): List<PageTitle> {
        val wikiSite = WikiSite.forLanguageCode(sourceWithOffset.language)
        val moreLikeResponse = ServiceFactory.get(wikiSite).searchMoreLike(
            searchTerm = "morelike:${sourceWithOffset.title}",
            gsrLimit = SUGGESTION_REQUEST_ITEMS,
            piLimit = SUGGESTION_REQUEST_ITEMS,
            gsrOffset = offset
        )

        // Logic to check if the article is already in the database, if it exists, check the next one.
        val firstRecommendedPage = moreLikeResponse.query?.pages.orEmpty()
            .sortedBy { it.index }
            .map { page ->
                PageTitle(page.title, wikiSite).apply {
                    displayText = page.displayTitle(wikiSite.languageCode)
                    description = page.description
                    thumbUrl = page.thumbUrl()
                }
            }.filter {
                AppDatabase.instance.recommendedPageDao()
                    .findIfAny(apiTitle = it.prefixedText, wiki = wikiSite) == 0
            }.take(takeSize)
        return firstRecommendedPage
    }
}

class SourceWithOffset(
    val title: String,
    val language: String,
    var offset: Int
)
