package org.wikipedia.readinglist.recommended

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource

class RecommendedReadingListViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    private val _uiState = MutableStateFlow(Resource<Unit>())
    val uiState = _uiState.asStateFlow()

    companion object {

        private const val MAX_RETRIES = 10

        suspend fun generateRecommendedReadingList() {
            if (!Prefs.isRecommendedReadingListEnabled) {
                return
            }
            val numberOfArticles = Prefs.recommendedReadingListArticlesNumber
            if (numberOfArticles <= 0) {
                return
            }
            // First: get titles from the source by number of articles
            val titles = when (Prefs.recommendedReadingListSource) {
                RecommendedReadingListSource.INTERESTS -> {
                    // TODO: Query from the RecommendedPageDao
                    emptyList()
                }
                RecommendedReadingListSource.READING_LIST -> {
                    AppDatabase.instance.readingListPageDao().getPagesByNumber(numberOfArticles).map {
                        PageTitle(it.apiTitle, it.wiki).apply {
                            namespace = it.namespace.name
                            displayText = it.displayTitle
                            description = it.description
                            thumbUrl = it.thumbUrl
                        }
                    }
                }
                RecommendedReadingListSource.HISTORY -> {
                    AppDatabase.instance.historyEntryDao().getHistoryEntriesByNumber(numberOfArticles).map {
                        it.title
                    }
                }
            }

            val recommendedPages = mutableListOf<PageTitle>()
            // Second: uses morelike API to get recommended article, but excludes the articles from database.
            // TODO: discuss the logic of possible re-query for duplicates
            // TODO: maybe we can use gsrsort=random to get random articles
            titles.forEach {
                var recommendedPage: PageTitle? = null
                var retryCount = 0
                while (recommendedPage == null && retryCount < MAX_RETRIES) {
                    recommendedPage = getRecommendedPage(it)
                    retryCount++
                }
                recommendedPage?.let {
                    recommendedPages.add(recommendedPage)
                }
            }

            // Third: save the recommended articles to the database
            // TODO: use RecommendedPageDao to save the recommended articles
        }

        private suspend fun getRecommendedPage(pageTitles: PageTitle): PageTitle? {
            val moreLikeResponse = ServiceFactory.get(pageTitles.wikiSite).searchMoreLike(
                searchTerm = "morelike:${pageTitles.prefixedText}",
                gsrLimit = Constants.SUGGESTION_REQUEST_ITEMS,
                piLimit = Constants.SUGGESTION_REQUEST_ITEMS
            )

            // Logic to check if the article is already in the database, if it exists, check the next one.
            val firstRecommendedPage = moreLikeResponse.query?.pages.orEmpty()
                .map { page ->
                    PageTitle(page.title, pageTitles.wikiSite).apply {
                        displayText = page.displayTitle(wikiSite.languageCode)
                        description = page.description
                        thumbUrl = page.thumbUrl()
                    }
                }
                .filterNot {
                    // TODO: filter wfrom the RecommendedPageDao
                    it != null
                }
                .firstOrNull()
            return firstRecommendedPage
        }
    }
}
