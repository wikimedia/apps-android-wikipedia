package org.wikipedia.readinglist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.Namespace
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.recommended.RecommendedReadingListHelper
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.yearinreview.YearInReviewViewModel
import java.time.Instant

class ReadingListFragmentViewModel : ViewModel() {

    private val _updateListByIdFlow = MutableSharedFlow<Resource<ReadingList>>()
    val updateListByIdFlow = _updateListByIdFlow.asSharedFlow()

    private val _updateListFlow = MutableSharedFlow<Resource<ReadingList>>()
    val updateListFlow = _updateListFlow.asSharedFlow()

    private val _saveReadingListFlow = MutableSharedFlow<Resource<ReadingList>>()
    val saveReadingListFlow = _saveReadingListFlow.asSharedFlow()

    private val _deleteSelectedPagesFlow = MutableSharedFlow<Resource<List<ReadingListPage>>>()
    val deleteSelectedPagesFlow = _deleteSelectedPagesFlow.asSharedFlow()

    private val _recommendedListFlow = MutableStateFlow(Resource<ReadingList>())
    val recommendedListFlow = _recommendedListFlow.asStateFlow()

    private val _yirListFlow = MutableStateFlow(Resource<ReadingList>())
    val yirListFlow = _yirListFlow.asStateFlow()

    fun updateListById(readingListId: Long) {
         viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
             viewModelScope.launch {
                 _updateListByIdFlow.emit(Resource.Error(throwable))
             }
        }) {
             val list = AppDatabase.instance.readingListDao().getListById(readingListId, true)
             if (list == null) {
                 _updateListByIdFlow.emit(Resource.Error(Throwable(L10nUtil.getString(R.string.error_message_generic))))
             } else {
                 _updateListByIdFlow.emit(Resource.Success(list))
             }
        }
    }

    fun updateList(emptyTitle: String, emptyDescription: String, encoded: Boolean) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            viewModelScope.launch {
                _updateListFlow.emit(Resource.Error(throwable))
            }
        }) {
            val json = Prefs.receiveReadingListsData
            if (!json.isNullOrEmpty()) {
                val list = ReadingListsReceiveHelper.receiveReadingLists(emptyTitle, emptyDescription, json, encoded)
                _updateListFlow.emit(Resource.Success(list))
            }
        }
    }

    fun saveReadingList(readingList: ReadingList) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            viewModelScope.launch {
                _saveReadingListFlow.emit(Resource.Error(throwable))
            }
        }) {
            readingList.id = AppDatabase.instance.readingListDao().insertReadingList(readingList)
            AppDatabase.instance.readingListPageDao().addPagesToList(readingList, readingList.pages, true)
            Prefs.readingListRecentReceivedId = readingList.id
            _saveReadingListFlow.emit(Resource.Success(readingList))
        }
    }

    fun deleteSelectedPages(readingList: ReadingList, pages: List<ReadingListPage>) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            viewModelScope.launch {
                _deleteSelectedPagesFlow.emit(Resource.Error(throwable))
            }
        }) {
            if (pages.isNotEmpty()) {
                AppDatabase.instance.readingListPageDao().markPagesForDeletion(readingList, pages)
                _deleteSelectedPagesFlow.emit(Resource.Success(pages))
            }
        }
    }

    fun generateRecommendedReadingList() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            viewModelScope.launch {
                _recommendedListFlow.value = Resource.Error(throwable)
            }
        }) {
            _recommendedListFlow.value = Resource.Loading()
            RecommendedReadingListHelper.generateRecommendedReadingList(shouldExpireOldPages = Prefs.resetRecommendedReadingList).let { list ->
                val context = WikipediaApp.instance
                if (list.isNotEmpty()) {
                    val description = when (Prefs.recommendedReadingListUpdateFrequency) {
                        RecommendedReadingListUpdateFrequency.DAILY -> R.string.recommended_reading_list_page_description_daily
                        RecommendedReadingListUpdateFrequency.WEEKLY -> R.string.recommended_reading_list_page_description_weekly
                        RecommendedReadingListUpdateFrequency.MONTHLY -> R.string.recommended_reading_list_page_description_monthly
                    }

                    // Get the recommended reading list from the database
                    val recommendedListPages = list.map {
                        ReadingListPage(
                            wiki = it.wiki,
                            lang = it.wiki.languageCode,
                            namespace = it.namespace,
                            displayTitle = it.displayTitle,
                            apiTitle = it.apiTitle,
                            description = it.description,
                            thumbUrl = it.thumbUrl,
                            remoteId = 0
                        ).apply {
                            mtime = System.currentTimeMillis()
                            atime = mtime
                        }
                    }
                    val recommendedList = ReadingList(
                        listTitle = context.getString(R.string.recommended_reading_list_title),
                        description = context.getString(description)
                    ).apply {
                        pages.addAll(recommendedListPages)
                        mtime = System.currentTimeMillis()
                        atime = mtime
                    }
                    _recommendedListFlow.value = Resource.Success(recommendedList)
                } else {
                    _recommendedListFlow.value = Resource.Error(Throwable(context.getString(R.string.error_message_generic)))
                }
            }
        }
    }

    fun generateYearInReviewReadingList(context: Context, userName: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            viewModelScope.launch {
                _yirListFlow.value = Resource.Error(throwable)
            }
        }) {
            println("orange generating yir reading list")
            val activeStartDate = "2025-01-01T00:00:00Z"
            val activeEndDate = "2025-12-31T23:59:59Z"
            val startMillis = Instant.parse(activeStartDate).toEpochMilli()
            val endMillis = Instant.parse(activeEndDate).toEpochMilli()

            val articles = AppDatabase.instance.historyEntryWithImageDao().getLongestReadArticlesInPeriod(startMillis, endMillis,
                YearInReviewViewModel.MAX_LONGEST_READ_ARTICLES)
            val yirReadingListPages = articles.map {
                ReadingListPage(
                    wiki = WikiSite.forLanguageCode(it.lang),
                    lang = it.lang,
                    namespace = Namespace.MAIN,
                    displayTitle = it.displayTitle,
                    apiTitle = it.apiTitle,
                    description = it.description,
                    thumbUrl = it.imageName,
                    remoteId = 0
                ).apply {
                    mtime = System.currentTimeMillis()
                    atime = mtime
                }
            }
            val readingList = ReadingList(
                listTitle = context.getString(R.string.year_in_review_reading_list_title, YearInReviewViewModel.YIR_YEAR),
                description = context.getString(R.string.year_in_review_reading_list_description, userName, YearInReviewViewModel.YIR_YEAR)
            ).apply {
                pages.addAll(yirReadingListPages)
                mtime = System.currentTimeMillis()
                atime = mtime
            }
            _yirListFlow.value = Resource.Success(readingList)
        }
    }
}
