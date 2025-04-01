package org.wikipedia.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource

class ReadingListFragmentViewModel : ViewModel() {

    private val _updateListByIdFlow = MutableSharedFlow<Resource<ReadingList>>()
    val updateListByIdFlow = _updateListByIdFlow.asSharedFlow()

    private val _updateListFlow = MutableSharedFlow<Resource<ReadingList>>()
    val updateListFlow = _updateListFlow.asSharedFlow()

    fun updateListById(readingListId: Long) {
         viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
             viewModelScope.launch {
                 _updateListByIdFlow.emit(Resource.Error(throwable))
             }
        }) {
             val list = AppDatabase.instance.readingListDao().getListById(readingListId, true)
             if (list == null) {
                 _updateListByIdFlow.emit(Resource.Error(Throwable(WikipediaApp.instance.getString(R.string.error_message_generic))))
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
}
