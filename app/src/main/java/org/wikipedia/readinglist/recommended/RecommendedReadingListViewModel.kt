package org.wikipedia.readinglist.recommended

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.database.AppDatabase
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource

class RecommendedReadingListViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    private val _uiState = MutableStateFlow(Resource<Unit>())
    val uiState = _uiState.asStateFlow()

    companion object {
        suspend fun generateRecommendedReadingList() {
            if (!Prefs.isRecommendedReadingListEnabled) {
                return
            }
            val numberOfArticles = Prefs.recommendedReadingListArticlesNumber
            val titles = when (Prefs.recommendedReadingListSource) {
                RecommendedReadingListSource.INTERESTS -> {

                }
                RecommendedReadingListSource.READING_LIST -> {
                    // Pick articles from the reading list from top X number of articles, and use the titles to generate the list
                    AppDatabase.instance.readingListPageDao().getPagesByNumber(numberOfArticles)
                }
                RecommendedReadingListSource.HISTORY -> {

                }
            }
        }
    }
}
