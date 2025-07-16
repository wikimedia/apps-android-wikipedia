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

    private val _deleteTabsState = MutableStateFlow(Resource<List<Tab>>())
    val deleteTabsState = _deleteTabsState.asStateFlow()
    private val _uiState = MutableStateFlow(Resource<List<Tab>>())
    val uiState = _uiState.asStateFlow()

    var hasTabs = false

    init {
        fetchTabs()
    }

    private fun fetchTabs() {
        viewModelScope.launch(handler) {
            _uiState.value = Resource.Loading()
            val tabs = AppDatabase.instance.tabDao().getTabs()
            hasTabs = tabs.isNotEmpty()
            if (!hasTabs) {
                _uiState.value = Resource.Success(emptyList())
                return@launch
            }
            tabs.forEach { tab ->
                // Use the backStackIds to get the full backStack items from the database
                val backStackItems = AppDatabase.instance.pageBackStackItemDao().getPageBackStackItems(tab.getBackStackIds())
                tab.backStack = backStackItems.toMutableList()
            }
            _uiState.value = Resource.Success(tabs)
        }
    }

    fun saveToList() {
        viewModelScope.launch(handler) {
            _saveToListState.value = Resource.Loading()
            if (!hasTabs) {
                _saveToListState.value = Resource.Success(emptyList())
                return@launch
            }
            val pageTitles = AppDatabase.instance.tabDao().getTabs()
                .mapNotNull { it.getBackStackPositionTitle() }
            if (pageTitles.isNotEmpty()) {
                _saveToListState.value = Resource.Success(pageTitles)
            } else {
                _saveToListState.value = Resource.Error(Exception("Empty list"))
            }
        }
    }

    fun closeTabs(tabs: List<Tab> = emptyList()) {
        viewModelScope.launch(handler) {
            _deleteTabsState.value = Resource.Loading()
            var tabsToDelete = tabs
            if (tabs.isEmpty()) {
                tabsToDelete = AppDatabase.instance.tabDao().getTabs()
            }
            TabHelper.deleteTabs(tabsToDelete)
            _deleteTabsState.value = Resource.Success(tabsToDelete)
        }
    }

    fun insertTabs(tab: List<Tab>) {
        viewModelScope.launch(handler) {
            _uiState.value = Resource.Loading()
            TabHelper.insertTabs(tab)
            fetchTabs()
        }
    }
}
