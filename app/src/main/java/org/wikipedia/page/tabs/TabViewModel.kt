package org.wikipedia.page.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource

class TabViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    private val _saveToListState = MutableStateFlow(Resource<List<PageTitle>>())
    val saveToListState = _saveToListState.asStateFlow()

    private val _deleteTabsState = MutableStateFlow(Resource<Pair<Int, List<Tab>>>())
    val deleteTabsState = _deleteTabsState.asStateFlow()

    private val _uiState = MutableStateFlow(Resource<List<Tab>>())
    val uiState = _uiState.asStateFlow()

    private val _clickState = MutableStateFlow(Resource<Boolean>())
    val clickState = _clickState.asStateFlow()

    var list = mutableListOf<Tab>()

    init {
        fetchTabs()
    }

    private fun fetchTabs() {
        viewModelScope.launch(handler) {
            _uiState.value = Resource.Loading()
            val tabs = AppDatabase.instance.tabDao().getTabs()
            tabs.forEach { tab ->
                // Use the backStackIds to get the full backStack items from the database
                val backStackItems = AppDatabase.instance.pageBackStackItemDao().getPageBackStackItems(tab.getBackStackIds())
                tab.backStack = backStackItems.toMutableList()
            }
            list = tabs.toMutableList()
            _uiState.value = Resource.Success(tabs)
        }
    }

    fun saveToList() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _saveToListState.value = Resource.Error(throwable)
        }) {
            _saveToListState.value = Resource.Loading()
            val pageTitles = list.mapNotNull { it.getBackStackPositionTitle() }
            if (pageTitles.isNotEmpty()) {
                _saveToListState.value = Resource.Success(pageTitles)
            } else {
                _saveToListState.value = Resource.Error(Exception("Empty list"))
            }
        }
    }

    fun closeTabs(tabs: List<Tab>) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _deleteTabsState.value = Resource.Error(throwable)
        }) {
            _deleteTabsState.value = Resource.Loading()
            val firstIndexFromList = list.indexOfFirst { it.id == tabs.firstOrNull()?.id }
            TabHelper.deleteTabs(tabs)
            list.removeAll(tabs)
            _deleteTabsState.value = Resource.Success(firstIndexFromList to tabs)
        }
    }

    fun insertTabs(tabs: List<Tab>) {
        viewModelScope.launch(handler) {
            _uiState.value = Resource.Loading()
            // For simple close, reset the order of tabs to default `0`
            // TODO: discuss about this
            if (tabs.size == 1) {
                tabs.first().order = 0
            }
            TabHelper.insertTabs(tabs)
            fetchTabs()
        }
    }

    fun addTabToLastPosition(tab: Tab) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _clickState.value = Resource.Error(throwable)
        }) {
            // Remove tab from current position if it exists
            list.removeAll { it.id == tab.id }

            // Add tab to last position
            tab.order = list.size
            list.add(tab)

            // Update order for all tabs
            list.forEachIndexed { index, it ->
                it.order = index
            }
            AppDatabase.instance.tabDao().updateTabs(list)
            _clickState.value = Resource.Success(true)
        }
    }
}
