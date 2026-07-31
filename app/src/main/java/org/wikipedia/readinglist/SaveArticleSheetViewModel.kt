package org.wikipedia.readinglist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.Constants
import org.wikipedia.page.PageTitle

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

    // TODO: replace the state above with a flow off readingListDao().getListsWithPagesFlow(),
    //  mapping each list to a SaveCollectionUiModel and setting containsArticle / article.isSaved
    //  from whether this pageTitle is present.

    fun onArticleSaveClick() {
        // TODO
    }

    fun onCreateCollectionClick() {
        // TODO
    }

    fun onCollectionClick(collectionId: Long) {
        // TODO
    }
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
