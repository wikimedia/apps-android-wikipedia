package org.wikipedia.readinglist.recommended

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.RecommendedPage
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource

class RecommendedReadingListViewModel : ViewModel() {

    private val _uiSourceState = MutableStateFlow(Resource<SourceSelectionUiState>())
    val uiSourceState: StateFlow<Resource<SourceSelectionUiState>> = _uiSourceState.asStateFlow()

    init {
        setupSourceSelection()
    }

    fun setupSourceSelection() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiSourceState.value = Resource.Error(throwable)
        }) {
            _uiSourceState.value = Resource.Loading()
            // TODO: discuss about using the same title to get recommended articles
            val isSavedOptionEnabled = AppDatabase.instance.readingListPageDao().getPagesCount() > 0
            val isHistoryOptionEnabled = AppDatabase.instance.historyEntryDao().getHistoryCount() > 0
            val selectedSource = Prefs.recommendedReadingListSource
            _uiSourceState.value = Resource.Success(
                SourceSelectionUiState(
                    isSavedOptionEnabled = isSavedOptionEnabled,
                    isHistoryOptionEnabled = isHistoryOptionEnabled,
                    selectedSource = selectedSource
                )
            )
        }
    }

    fun updateSourceSelection(newSource: RecommendedReadingListSource) {
        val stateValue = _uiSourceState.value
        if (stateValue is Resource.Success) {
            _uiSourceState.value = Resource.Success(
                SourceSelectionUiState(
                    isSavedOptionEnabled = stateValue.data.isSavedOptionEnabled,
                    isHistoryOptionEnabled = stateValue.data.isHistoryOptionEnabled,
                    selectedSource = newSource
                )
            )
        }
    }

    companion object {

        private const val MAX_RETRIES = 10

        suspend fun generateRecommendedReadingList() {
            if (!Prefs.isRecommendedReadingListEnabled) {
                return
            }
            var numberOfArticles = Prefs.recommendedReadingListArticlesNumber
            if (numberOfArticles <= 0) {
                return
            }
            // Check if amount of new articles to see if we really need to generate a new list
            val newArticles = AppDatabase.instance.recommendedPageDao().getNewRecommendedPages().size
            if (newArticles >= numberOfArticles) {
                return
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
            val recommendedPages = mutableListOf<RecommendedPage>()
            // Step 3: uses morelike API to get recommended article, but excludes the articles from database,
            // and update the offset everytime when re-query the API.
            sourcesWithOffset.forEach { sourcesWithOffset ->
                var recommendedPage: PageTitle? = null
                var retryCount = 0
                var offset = sourcesWithOffset.offset
                while (recommendedPage == null && retryCount < MAX_RETRIES) {
                    recommendedPage = getRecommendedPage(sourcesWithOffset, offset)
                    // Cannot find any recommended articles, so update the offset and retry.
                    if (recommendedPage == null) {
                        offset += Constants.SUGGESTION_REQUEST_ITEMS
                    }
                    retryCount++
                }
                recommendedPage?.let {
                    recommendedPages.add(
                        RecommendedPage(
                            wiki = it.wikiSite,
                            lang = it.wikiSite.languageCode,
                            namespace = it.namespace(),
                            apiTitle = it.prefixedText,
                            displayTitle = it.displayText,
                            description = it.description,
                            thumbUrl = it.thumbUrl
                        )
                    )
                    // Update the offset in the source list
                    newSourcesWithOffset.add(SourceWithOffset(sourcesWithOffset.title, sourcesWithOffset.language, offset))
                }
            }

            // Step 4: save the recommended articles to the database
            AppDatabase.instance.recommendedPageDao().insertAll(recommendedPages)
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

    data class SourceSelectionUiState(
        val isSavedOptionEnabled: Boolean,
        val isHistoryOptionEnabled: Boolean,
        val selectedSource: RecommendedReadingListSource
    )

    class SourceWithOffset(
        val title: String,
        val language: String,
        var offset: Int
    )
}
