package org.wikipedia.page.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource

class TabViewModel : ViewModel() {

    private val _saveToListState = MutableStateFlow(Resource<List<PageTitle>>())
    val saveToListState = _saveToListState.asStateFlow()

    private val _deleteTabsState = MutableStateFlow(Resource<Pair<List<Tab>, List<Tab>>>())
    val deleteTabsState = _deleteTabsState.asStateFlow()

    private val _undoTabsState = MutableStateFlow(Resource<Pair<Int, List<Tab>>>())
    val undoTabsState = _undoTabsState.asStateFlow()

    private val _uiState = MutableStateFlow(Resource<List<Tab>>())
    val uiState = _uiState.asStateFlow()

    private val _clickState = MutableStateFlow(Resource<Boolean>())
    val clickState = _clickState.asStateFlow()

    var list = mutableListOf<Tab>()

    init {
        setup()
    }

    private fun setup() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            _uiState.value = Resource.Loading()
            val tabs = fetchTabs()
            _uiState.value = Resource.Success(tabs)
        }
    }

    private suspend fun fetchTabs(): List<Tab> {
        return withContext(Dispatchers.IO) {
            val tabs = AppDatabase.instance.tabDao().getTabs()
            tabs.forEach { tab ->
                // Use the backStackIds to get the full backStack items from the database
                val backStackItems = AppDatabase.instance.pageBackStackItemDao()
                    .getPageBackStackItems(tab.getBackStackIds())
                tab.backStack = backStackItems.toMutableList()
            }
            list = tabs.toMutableList()
            tabs
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

    fun deleteTabs(deletedTabs: List<Tab>) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _deleteTabsState.value = Resource.Error(throwable)
        }) {
            _deleteTabsState.value = Resource.Loading()
            val originalList = list.toList()
            TabHelper.deleteTabs(deletedTabs)
            list.removeAll(deletedTabs)
            _deleteTabsState.value = Resource.Success(originalList to deletedTabs)
        }
    }

    fun undoDeleteTabs(undoPosition: Int, tabs: List<Tab>) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _undoTabsState.value = Resource.Error(throwable)
        }) {
            TabHelper.insertTabs(tabs)
            val tabs = fetchTabs()
            _undoTabsState.value = Resource.Success(undoPosition to tabs)
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
