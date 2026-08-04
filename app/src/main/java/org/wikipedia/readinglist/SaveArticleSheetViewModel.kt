package org.wikipedia.readinglist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.log.L

class SaveArticleSheetViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!

    private val _uiState = MutableStateFlow(
        SaveArticleSheetUiState(
            article = SaveArticleUiModel(
                title = pageTitle.displayText,
                description = pageTitle.description,
                thumbUrl = pageTitle.thumbUrl
            )
        )
    )
    val uiState: StateFlow<SaveArticleSheetUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SaveArticleSheetEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SaveArticleSheetEvent> = _events.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> L.e(throwable) }

    // TODO: replace the state above with a flow off readingListDao().getListsWithPagesFlow(),
    //  mapping each list to a SaveCollectionUiModel and setting containsArticle / article.isSaved
    //  from whether this pageTitle is present.

    fun toggleArticleSaved() {
        // TODO
    }

    fun requestNewCollection() {
        viewModelScope.launch(exceptionHandler) {
            val lists = AppDatabase.instance.readingListDao().getListsWithoutContents()
            _events.emit(
                if (lists.size >= Constants.MAX_READING_LISTS_LIMIT) {
                    SaveArticleSheetEvent.ListLimitReached
                } else {
                    SaveArticleSheetEvent.ShowCreateCollectionDialog(lists.map { it.title })
                }
            )
        }
    }

    fun createCollection(title: String, description: String) {
        viewModelScope.launch(exceptionHandler) {
            val list = AppDatabase.instance.readingListDao().createList(title, description)
            AppDatabase.instance.readingListPageDao().addPagesToListIfNotExist(list, listOf(pageTitle))
            _events.emit(SaveArticleSheetEvent.ArticleAddedToCollection(list))
        }
    }

    fun toggleArticleInCollection(collectionId: Long) {
        // TODO
    }
}

sealed interface SaveArticleSheetEvent {
    data class ShowCreateCollectionDialog(val existingTitles: List<String>) : SaveArticleSheetEvent
    data class ArticleAddedToCollection(val list: ReadingList) : SaveArticleSheetEvent
    data object ListLimitReached : SaveArticleSheetEvent
}

/** The article the sheet was opened for, shown pinned at the top. */
data class SaveArticleUiModel(
    val title: String,
    val description: String?,
    val thumbUrl: String? = null,
    val isSaved: Boolean = false
)

/** A collection the article can be added to, as shown in the sheet's list. */
data class SaveCollectionUiModel(
    val id: Long,
    val title: String,
    val totalPages: Int,
    val thumbUrl: String? = null,
    // determines if the article this sheet was opened for is already in this collection
    val containsArticle: Boolean = false
)

data class SaveArticleSheetUiState(
    val article: SaveArticleUiModel,
    val collections: List<SaveCollectionUiModel> = emptyList(),
    val isLoading: Boolean = true
)
