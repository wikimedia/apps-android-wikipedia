package org.wikipedia.page.pageload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageBackStackItem
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.page.tabs.Tab
import org.wikipedia.util.log.L

class PageLoadViewModel(private val app: WikipediaApp) : ViewModel() {

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Idle)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private val _progressVisible = MutableStateFlow(false)
    val progressVisible: StateFlow<Boolean> = _progressVisible.asStateFlow()

    private val _currentPageModel = MutableStateFlow<PageViewModel?>(null)
    val currentPageModel: StateFlow<PageViewModel?> = _currentPageModel.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorState = MutableStateFlow<Throwable?>(null)
    val errorState: StateFlow<Throwable?> = _errorState.asStateFlow()

    // Internal state
    private var currentTab: Tab = app.tabList.last()
    private val pageDataFetcher = PageDataFetcher()
    val foregroundTabPosition get() = app.tabList.size
    val backgroundTabPosition get() = 0.coerceAtLeast(foregroundTabPosition - 1)

    fun loadPage(request: PageLoadRequest, webScrollY: Int = 0) {
        viewModelScope.launch(CoroutineExceptionHandler {_, throwable ->
            handleError(throwable)
        }) {
            try {
                when (determineLoadType(request)) {
                    LoadType.CurrentTab -> loadInCurrentTab(request, webScrollY)
                    LoadType.ExistingTab -> loadInExistingTab(request)
                    LoadType.FromBackStack -> loadFromBackStack(request, webScrollY)
                    LoadType.NewBackgroundTab -> loadInNewBackgroundTab(request)
                    LoadType.NewForegroundTab -> loadInNewForegroundTab(request, webScrollY)
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _progressVisible.value = false
            }
        }
    }

    fun setTab(tab: Tab): Boolean {
        val isDifferent = tab != currentTab
        currentTab = tab
        return isDifferent
    }

    fun clearError() {
        _errorState.value = null
    }

    fun backStackEmpty(): Boolean {
        return currentTab.backStack.isEmpty()
    }

    fun updateCurrentBackStackItem(scrollY: Int) {
        if (currentTab.backStack.isEmpty()) {
            return
        }
        val item = currentTab.backStack[currentTab.backStackPosition]
        item.scrollY = scrollY
        _currentPageModel.value?.title?.let {
            item.title.description = it.description
            item.title.thumbUrl = it.thumbUrl
        }
    }

    private suspend fun loadInExistingTab(request: PageLoadRequest) {
        val selectedTabPosition = selectedTabPosition(request.title)
        if (selectedTabPosition == -1) {
            loadPageData(request)
        }
    }

    private suspend fun loadInCurrentTab(request: PageLoadRequest, webScrollY: Int) {
        val model = currentPageModel.value
        if (currentTab.backStack.isNotEmpty() &&
            request.title == currentTab.backStack[currentTab.backStackPosition].title) {
            if (model?.page == null || request.options.isRefresh) {
                loadFromBackStack()
            } else if (!request.title.fragment.isNullOrEmpty()) {
                _loadState.value = LoadState.Success(title = request.title, sectionAnchor = request.title.fragment)
            }
            return
        }
        if (request.options.squashBackStack) {
            if (app.tabCount > 0) {
                app.tabList.last().clearBackstack()
            }
        }
        loadFromBackStack(request, webScrollY)
    }

    private suspend fun loadFromBackStack(request: PageLoadRequest, webScrollY: Int, isNewTabCreated: Boolean = false) {
        if (request.options.pushbackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem(webScrollY)
            currentTab.pushBackStackItem(PageBackStackItem(request.title, request.entry))
        }
        loadPageData(request, isNewTabCreated)
    }

    private suspend fun loadInNewForegroundTab(request: PageLoadRequest, webScrollY: Int) {
        val isNewTabCreated = createNewTab(request, isForeground = true)
        loadFromBackStack(request, webScrollY, isNewTabCreated)
    }

    private fun loadInNewBackgroundTab(request: PageLoadRequest) {
        val isForeground = app.tabCount == 0
        if (isForeground) {
            createNewTab(request, isForeground = true)
            loadFromBackStack()
        } else {
            val isNewTabCreated = createNewTab(request, isForeground = false)
            _loadState.value = LoadState.Success(title = request.title, isNewTabCreated = isNewTabCreated, loadedFromBackground = true)
        }
    }

    fun loadFromBackStack() {
        if (currentTab.backStack.isEmpty()) {
            return
        }
        val item = currentTab.backStack[currentTab.backStackPosition]
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        loadPage(request = PageLoadRequest(
            title = item.title,
            entry = item.historyEntry,
            options = PageLoadOptions(
                pushbackStack = false,
                stagedScrollY = item.scrollY,
                shouldLoadFromBackStack = true,
            ))
        )
        L.d("Loaded page " + item.title.displayText + " from backstack")
    }

    private suspend fun loadPageData(request: PageLoadRequest, isNewTabCreated: Boolean = false) {
        _loadState.value = LoadState.Loading(request.options.isRefresh)
        _progressVisible.value = true
        _errorState.value = null
        val result = pageDataFetcher.fetchPage(
            title = request.title,
            entry = request.entry,
            forceNetwork = request.options.isRefresh
        )
        when (result) {
            is PageResult.Success -> {
                val pageModel = createPageModel(request, result)
                if (request.title.namespace() == Namespace.SPECIAL) {
                    _loadState.value = LoadState.SpecialPage(request)
                    return
                }
                _currentPageModel.value = pageModel
                _loadState.value = LoadState.Success(result, isNewTabCreated, request.title, request.options.stagedScrollY)
            }
            is PageResult.Error -> {
                handleError(result.throwable)
            }
        }
    }

    private fun loadBackgroundTabMetadata(title: PageTitle) {
        viewModelScope.launch(CoroutineExceptionHandler { _, t -> L.e(t) }) {
            ServiceFactory.get(title.wikiSite)
                .getInfoByPageIdsOrTitles(null, title.prefixedText)
                .query?.firstPage()?.let { page ->
                    app.tabList.find { it.backStackPositionTitle == title }
                        ?.backStackPositionTitle?.apply {
                            thumbUrl = page.thumbUrl()
                            description = page.description
                        }
                }
        }
    }

    private fun createNewTab(request: PageLoadRequest, isForeground: Boolean): Boolean {
        val existingTabPosition = selectedTabPosition(request.title)
        if (existingTabPosition >= 0) {
            switchToExistingTab(existingTabPosition)
            return false
        }

        val shouldCreateNewTab = currentTab.backStack.isNotEmpty()
        if (shouldCreateNewTab) {
            val tab = Tab()
            val position = if (isForeground) foregroundTabPosition else backgroundTabPosition
            if (isForeground) {
                setTab(tab)
            }
            app.tabList.add(position, tab)
            trimTabCount()

            tab.backStack.add(PageBackStackItem(request.title, request.entry))
            if (!isForeground) {
                // Load metadata for background tab
                loadBackgroundTabMetadata(request.title)
            }
            return true
        } else {
            setTab(currentTab)
            currentTab.backStack.add(PageBackStackItem(request.title, request.entry))
            return false
        }
    }

    private fun switchToExistingTab(position: Int) {
        if (position < app.tabList.size - 1) {
            val tab = app.tabList.removeAt(position)
            app.tabList.add(tab)
            setTab(tab)
        }

        if (app.tabCount > 0) {
            app.tabList.last().squashBackstack()
            loadFromBackStack()
        }
    }

    private fun trimTabCount() {
        while (app.tabList.size > Constants.MAX_TABS) {
            app.tabList.removeAt(0)
        }
    }

    private fun selectedTabPosition(title: PageTitle): Int {
        return app.tabList.firstOrNull { it.backStackPositionTitle != null &&
                title == it.backStackPositionTitle }?.let { app.tabList.indexOf(it) } ?: -1
    }

    private suspend fun createPageModel(request: PageLoadRequest, result: PageResult.Success): PageViewModel {
        return PageViewModel().apply {
            title = request.title
            curEntry = request.entry
            forceNetwork = request.options.isRefresh
            readingListPage = AppDatabase.instance.readingListPageDao().findPageInAnyList(request.title)
            page = result.pageSummaryResponse.body()?.toPage(title)
            isWatched = result.isWatched
            hasWatchlistExpiry = result.hasWatchlistExpiry
        }
    }

    private fun handleError(throwable: Throwable) {
        L.e(throwable)
        _errorState.value = throwable
        _loadState.value = LoadState.Error(throwable)
    }

    private fun determineLoadType(request: PageLoadRequest): LoadType {
        return when {
            request.options.shouldLoadFromBackStack -> LoadType.FromBackStack
            request.options.tabPosition == PageActivity.TabPosition.NEW_TAB_FOREGROUND -> LoadType.NewForegroundTab
            request.options.tabPosition == PageActivity.TabPosition.NEW_TAB_BACKGROUND -> LoadType.NewBackgroundTab
            request.options.tabPosition == PageActivity.TabPosition.EXISTING_TAB -> LoadType.ExistingTab
            else -> LoadType.CurrentTab
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WikipediaApp
                PageLoadViewModel(app)
            }
        }
    }
}