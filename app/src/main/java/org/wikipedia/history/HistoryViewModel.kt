package org.wikipedia.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import org.wikipedia.util.Resource

class HistoryViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    var searchQuery: String? = null

    private val _uiState = MutableStateFlow(Resource<List<Any>>())
    val uiState = _uiState.asStateFlow()

    private val _actionUiState = MutableStateFlow(Resource<Boolean>())
    val actionUiState = _actionUiState.asStateFlow()

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
            _uiState.value = Resource.Success(items)
        }
    }

    fun deleteHistoryItems() {
        viewModelScope.launch(handler) {
            AppDatabase.instance.historyEntryDao().deleteAll()
            _uiState.value = Resource.Success(emptyList())
        }
    }

    fun deleteHistoryItems(entries: List<HistoryEntry>) {
        viewModelScope.launch(handler) {
            entries.forEach {
                AppDatabase.instance.historyEntryDao().delete(it)
            }
            _actionUiState.value = Resource.Success(true)
        }
    }

    fun insertHistoryItem(entries: List<HistoryEntry>) {
        viewModelScope.launch(handler) {
            AppDatabase.instance.historyEntryDao().insert(entries)
            loadHistoryItems()
        }
    }
}
