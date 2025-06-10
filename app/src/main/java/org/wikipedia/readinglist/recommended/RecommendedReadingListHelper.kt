package org.wikipedia.readinglist.recommended

import org.wikipedia.Constants
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.NewRecommendedReadingListEvent
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.RecommendedPage
import org.wikipedia.settings.Prefs

object RecommendedReadingListHelper {

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
            return existingRecommendedPages
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

        // If the sourcesWithOffset less than the number of articles, we need to fill it with the same titles
        while (sourcesWithOffset.size < numberOfArticles) {
            val additionalSources = sourcesWithOffset.shuffled().take(numberOfArticles - sourcesWithOffset.size)
            sourcesWithOffset.addAll(additionalSources)
        }

        val newSourcesWithOffset = mutableListOf<SourceWithOffset>()
        val newRecommendedPages = mutableListOf<RecommendedPage>()
        // Step 3: uses morelike API to get recommended article, but excludes the articles from database,
        // and update the offset everytime when re-query the API.
        sourcesWithOffset.forEach { sourceWithOffset ->
            var recommendedPage: PageTitle? = null
            var retryCount = 0
            var offset = sourceWithOffset.offset
            while (recommendedPage == null && retryCount < MAX_RETRIES) {
                recommendedPage = getRecommendedPage(sourceWithOffset, offset)
                // Cannot find any recommended articles, so update the offset and retry.
                if (recommendedPage == null) {
                    offset += Constants.SUGGESTION_REQUEST_ITEMS
                }
                retryCount++
            }

            // Step 4: if the recommended page is generated, insert it into the database,
            recommendedPage?.let {
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

        // Step 5: if the list is empty, we can get the expired pages from the database
        if (newRecommendedPages.isEmpty()) {
            val expiredPages = AppDatabase.instance.recommendedPageDao().getExpiredRecommendedPages(Prefs.recommendedReadingListArticlesNumber)
            expiredPages.map {
                it.status = 0
                it
            }.apply {
                AppDatabase.instance.recommendedPageDao().updateAll(this)
                newRecommendedPages.addAll(this)
            }
        }
        Prefs.isNewRecommendedReadingListGenerated = true
        FlowEventBus.post(NewRecommendedReadingListEvent())
        return (newRecommendedPages + existingRecommendedPages).distinct()
    }

    private suspend fun getRecommendedPage(sourceWithOffset: SourceWithOffset, offset: Int): PageTitle? {
        val wikiSite = WikiSite.forLanguageCode(sourceWithOffset.language)
        val moreLikeResponse = ServiceFactory.get(wikiSite).searchMoreLike(
            searchTerm = "morelike:${sourceWithOffset.title}",
            gsrLimit = Constants.SUGGESTION_REQUEST_ITEMS,
            piLimit = Constants.SUGGESTION_REQUEST_ITEMS,
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
            }.firstOrNull {
                AppDatabase.instance.recommendedPageDao()
                    .findIfAny(apiTitle = it.prefixedText, wiki = wikiSite) == 0
            }
        return firstRecommendedPage
    }
}

class SourceWithOffset(
    val title: String,
    val language: String,
    var offset: Int
)
