package org.wikipedia.history

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage.Category
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData
import org.wikipedia.util.StringUtil

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
    var groupedTitles = emptyList<Pair<String, Int>>()

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
            val historyEntryItems = items.filterIsInstance<HistoryEntry>()

            val categories = mutableListOf<Category>()
            val deferredCategories = historyEntryItems.map { item ->
                async {
                    try {
                        val response = ServiceFactory.get(WikipediaApp.instance.wikiSite).getCategoriesProps(item.apiTitle)
                        response.query?.firstPage()?.categoriesProps ?: emptyList()
                    } catch (e: Exception) {
                        // Handle exceptions appropriately, e.g., log, return emptyList, etc.
                        println("Error fetching categories for ${item.apiTitle}: ${e.message}")
                        emptyList() // or throw e; depending on your error handling.
                    }
                }
            }

            deferredCategories.awaitAll().forEach { categories.addAll(it) }

            // Grouping the titles by setting in a Pair<> with its name and count
            groupedTitles = categories.groupBy { it.title }.map { Pair(StringUtil.removeNamespace(it.key), it.value.size) }.sortedByDescending { it.second }
            historyItems.postValue(Resource.Success(items))
        }
    }

    fun deleteAllHistoryItems() {
        viewModelScope.launch(handler) {
            AppDatabase.instance.historyEntryDao().deleteAll()
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
