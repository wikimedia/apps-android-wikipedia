package org.wikipedia.readinglist

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
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.recommended.RecommendedReadingListHelper
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.settings.Prefs
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource

class ReadingListFragmentViewModel : ViewModel() {

    private val _updateListByIdFlow = MutableSharedFlow<Resource<ReadingList>>()
    val updateListByIdFlow = _updateListByIdFlow.asSharedFlow()

    private val _updateListFlow = MutableSharedFlow<Resource<ReadingList>>()
    val updateListFlow = _updateListFlow.asSharedFlow()

    private val _recommendedListFlow = MutableStateFlow(Resource<ReadingList>())
    val recommendedListFlow = _recommendedListFlow.asStateFlow()

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

    fun generateRecommendedReadingList() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            viewModelScope.launch {
                _recommendedListFlow.value = Resource.Error(throwable)
            }
        }) {
            _recommendedListFlow.value = Resource.Loading()
            RecommendedReadingListHelper.generateRecommendedReadingList().let { list ->
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
}
