package org.wikipedia.readinglist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.database.ReadingListWithPages
import org.wikipedia.util.log.L
import java.io.IOException

class SaveArticleSheetViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!

    private val savedPageTitle = MutableStateFlow(pageTitle)

    val uiState: StateFlow<SaveArticleSheetUiState> = combine(
        AppDatabase.instance.readingListDao().getListsWithPagesFlow(),
        savedPageTitle
    ) { lists, title ->
        buildUiState(lists, title)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SaveArticleSheetUiState(
            article = SaveArticleUiModel(
                title = pageTitle.displayText,
                description = pageTitle.description,
                thumbUrl = pageTitle.thumbUrl
            )
        )
    )

    // these are for side effects that the UI needs to handle, like showing a dialog or a snackbar
    private val _events = MutableSharedFlow<SaveArticleSheetEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SaveArticleSheetEvent> = _events.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> L.e(throwable) }

    init {
        saveArticle()
    }

    private fun buildUiState(
        relations: List<ReadingListWithPages>,
        title: PageTitle
    ): SaveArticleSheetUiState {
        val readingLists = relations.map { it.toReadingList() }
        return SaveArticleSheetUiState(
            article = SaveArticleUiModel(
                title = title.displayText,
                description = title.description,
                thumbUrl = title.thumbUrl,
                isSaved = readingLists.any { list -> list.containsArticle(title) }
            ),
            collections = readingLists
                .filterNot { it.isDefault }
                .sortedWith(compareByDescending<ReadingList> { it.mtime }.thenByDescending { it.id })
                .map { list -> list.toCollectionUiModel(title) },
            isLoading = false
        )
    }

    private fun ReadingList.containsArticle(title: PageTitle) =
        pages.any { page -> page.isSameArticle(title) }

    private fun ReadingList.toCollectionUiModel(articleTitle: PageTitle) =
        SaveCollectionUiModel(
            id = id,
            title = title,
            totalPages = pages.size,
            thumbUrl = pages.firstOrNull()?.thumbUrl,
            containsArticle = containsArticle(articleTitle)
        )

    private fun saveArticle() {
        viewModelScope.launch(exceptionHandler) {
            savedPageTitle.value = resolveRedirect(pageTitle)
            val pageDao = AppDatabase.instance.readingListPageDao()
            if (pageDao.findPageInAnyList(savedPageTitle.value) == null) {
                val defaultList = AppDatabase.instance.readingListDao().getDefaultList()
                pageDao.addPagesToListIfNotExist(defaultList, listOf(savedPageTitle.value))
            }
        }
    }

    private suspend fun resolveRedirect(title: PageTitle): PageTitle {
        // If the title is a redirect, resolve it before saving to the reading list.
        val pageInfo = try {
            ServiceFactory.get(title.wikiSite).getInfoByPageIdsOrTitles(null, title.prefixedText).query?.firstPage()
        } catch (exception: IOException) {
            // A network error (or being offline) during the redirect resolution is not a big deal,
            // and we can just proceed with the original title.
            L.e(exception)
            null
        }
        return pageInfo?.let {
            PageTitle(
                it.title,
                title.wikiSite,
                it.thumbUrl(),
                it.description,
                it.displayTitle(title.wikiSite.languageCode),
                null
            )
        } ?: title
    }

    private fun ReadingListPage.isSameArticle(title: PageTitle): Boolean {
        return wiki == title.wikiSite &&
            lang == title.wikiSite.languageCode &&
            namespace == title.namespace() &&
            apiTitle == title.prefixedText
    }

    fun toggleArticleSaved() {
        viewModelScope.launch(exceptionHandler) {
            val lists = getListsContainingArticle()
            when {
                lists.isEmpty() -> {
                    val defaultList = AppDatabase.instance.readingListDao().getDefaultList()
                    AppDatabase.instance.readingListPageDao()
                        .addPagesToListIfNotExist(defaultList, listOf(savedPageTitle.value))
                }
                lists.any { !it.isDefault } -> {
                    _events.emit(SaveArticleSheetEvent.ShowUnsaveConfirmation)
                }
                else -> removeArticleFromLists(lists)
            }
        }
    }

    fun confirmUnsaveArticle() {
        viewModelScope.launch(exceptionHandler) {
            removeArticleFromLists(getListsContainingArticle())
        }
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
            AppDatabase.instance.readingListPageDao().addPagesToListIfNotExist(list, listOf(savedPageTitle.value))
            _events.emit(SaveArticleSheetEvent.ArticleAddedToCollection(list))
        }
    }

    fun toggleArticleInCollection(collectionId: Long) {
        viewModelScope.launch(exceptionHandler) {
            val list = AppDatabase.instance.readingListDao().getListWithPagesById(collectionId)?.toReadingList() ?: return@launch
            list.pages.firstOrNull { it.isSameArticle(savedPageTitle.value) }?.let { page ->
                AppDatabase.instance.readingListPageDao().markPagesForDeletion(list, listOf(page))
                return@launch
            }
            if (list.pages.size >= Constants.MAX_READING_LIST_ARTICLE_LIMIT) {
                _events.emit(SaveArticleSheetEvent.CollectionArticleLimitReached(list))
                return@launch
            }

            val addedTitles = AppDatabase.instance.readingListPageDao().addPagesToListIfNotExist(list, listOf(savedPageTitle.value))
            if (addedTitles.isNotEmpty()) {
                _events.emit(SaveArticleSheetEvent.ArticleAddedToCollection(list))
            }
        }
    }

    private suspend fun getListsContainingArticle(): List<ReadingList> {
        val pages = AppDatabase.instance.readingListPageDao().getAllPageOccurrences(savedPageTitle.value)
        if (pages.isEmpty()) {
            return emptyList()
        }
        return AppDatabase.instance.readingListDao().getListsFromPageOccurrences(pages)
    }

    private suspend fun removeArticleFromLists(lists: List<ReadingList>) {
        if (lists.isNotEmpty()) {
            AppDatabase.instance.readingListPageDao().markPagesForDeletionFromLists(lists)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

sealed interface SaveArticleSheetEvent {
    data class ShowCreateCollectionDialog(val existingTitles: List<String>) : SaveArticleSheetEvent
    data class ArticleAddedToCollection(val list: ReadingList) : SaveArticleSheetEvent
    data class CollectionArticleLimitReached(val list: ReadingList) : SaveArticleSheetEvent
    data object ShowUnsaveConfirmation : SaveArticleSheetEvent
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
