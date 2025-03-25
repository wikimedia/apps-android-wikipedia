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
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
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
            val lang = "en"
            val combinedTitles = items.filterIsInstance<HistoryEntry>().filter { it.lang == lang }.take(50).map { it.apiTitle }
            val response = ServiceFactory.get(WikiSite.forLanguageCode(lang)).getCategories(combinedTitles.joinToString("|"))
            val titles = response.query?.pages?.map { page ->
                PageTitle(page.title, WikiSite.forLanguageCode(lang)).also {
                    it.displayText = page.displayTitle(lang)
                }
            }.orEmpty()

            // Grouping the titles by setting in a Pair<> with its name and count
            groupedTitles = titles.groupBy { it.prefixedText }.map { Pair(StringUtil.removeNamespace(it.key), it.value.size) }.sortedByDescending { it.second }
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
