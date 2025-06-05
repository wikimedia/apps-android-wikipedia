package org.wikipedia.readinglist.recommended

import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.RecommendedPage
import org.wikipedia.settings.Prefs

object RecommendedReadingListHelper {

    private const val MAX_RETRIES = 10

    suspend fun generateRecommendedReadingList(): Boolean {
        if (!Prefs.isRecommendedReadingListEnabled) {
            return false
        }
        var numberOfArticles = Prefs.recommendedReadingListArticlesNumber
        if (numberOfArticles <= 0) {
            return false
        }
        // Check if amount of new articles to see if we really need to generate a new list
        val newArticles = AppDatabase.instance.recommendedPageDao().getNewRecommendedPages().size
        if (newArticles >= numberOfArticles) {
            return true
        } else {
            // If the number of articles is less than the number of new articles, adjust the number of articles
            numberOfArticles -= newArticles
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

        // Step 2: combine the titles with the offsite from Prefs.
        val sourcesWithOffset = mutableListOf<SourceWithOffset>()
        titles.forEach { pageTitle ->
            val offset = Prefs.recommendedReadingListSourceTitlesWithOffset.find { it.title == pageTitle.prefixedText }?.offset ?: 0
            sourcesWithOffset.add(SourceWithOffset(pageTitle.prefixedText, pageTitle.wikiSite.languageCode, offset))
        }

        val newSourcesWithOffset = mutableListOf<SourceWithOffset>()
        // Step 3: uses morelike API to get recommended article, but excludes the articles from database,
        // and update the offset everytime when re-query the API.
        var newListGenerated = false
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
                newListGenerated = true
            }
        }

        Prefs.isNewRecommendedReadingListGenerated = newListGenerated

        return true
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
