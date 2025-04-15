package org.wikipedia.history

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class HistoryViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        historyItems.postValue(Resource.Error(throwable))
    }

    private var searchJob: Job? = null

    var searchQuery: String? = null
        set(value) {
            field = value
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(500)
                reloadHistoryItems()
            }
        }

    val historyItems = MutableLiveData(Resource<List<Any>>())
    val deleteHistoryItemsAction = SingleLiveData<Resource<Boolean>>()

    init {
        reloadHistoryItems()
    }

    fun reloadHistoryItems() {
        viewModelScope.launch(handler) {
            loadHistoryItems()
        }
    }

    private suspend fun loadHistoryItems() {
        withContext(Dispatchers.IO) {
            val items = AppDatabase.instance.historyEntryWithImageDao().filterHistoryItems(searchQuery.orEmpty())
            historyItems.postValue(Resource.Success(items))
        }
    }

    fun deleteAllHistoryItems() {
        viewModelScope.launch(handler) {
            AppDatabase.instance.historyEntryDao().deleteAll()
            AppDatabase.instance.pageImagesDao().deleteAll()
            historyItems.postValue(Resource.Success(emptyList()))
        }
    }

    fun deleteHistoryItems(entries: List<HistoryEntry>) {
        viewModelScope.launch(handler) {
            entries.forEach {
                AppDatabase.instance.historyEntryDao().delete(it)
            }
            deleteHistoryItemsAction.postValue(Resource.Success(true))
        }
    }

    fun insertHistoryItem(entries: List<HistoryEntry>) {
        viewModelScope.launch(handler) {
            AppDatabase.instance.historyEntryDao().insert(entries)
            loadHistoryItems()
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
