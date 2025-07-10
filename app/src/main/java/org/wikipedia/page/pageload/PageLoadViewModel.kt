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
import kotlinx.coroutines.flow.update
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

    private val _pageLoadState = MutableStateFlow<PageLoadState>(PageLoadState())
    val pageLoadState: StateFlow<PageLoadState> = _pageLoadState.asStateFlow()

    // Internal state
    private var currentTab: Tab = app.tabList.last()
    private val pageDataFetcher = PageDataFetcher()
    val foregroundTabPosition get() = app.tabList.size
    val backgroundTabPosition get() = 0.coerceAtLeast(foregroundTabPosition - 1)

    fun loadPage(request: PageLoadRequest, webScrollY: Int = 0) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            handleError(throwable)
        }) {
            when (determineLoadType(request)) {
                LoadType.CurrentTab -> loadInCurrentTab(request, webScrollY)
                LoadType.ExistingTab -> loadInExistingTab(request)
                LoadType.FromBackStack -> loadPageData(request)
                LoadType.NewBackgroundTab -> loadInNewBackgroundTab(request)
                LoadType.NewForegroundTab -> loadInNewForegroundTab(request, webScrollY)
            }
        }
    }

    fun setTab(tab: Tab): Boolean {
        val isDifferent = tab != currentTab
        currentTab = tab
        return isDifferent
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
        _pageLoadState.value.currentPageModel?.title?.let {
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
        val model = _pageLoadState.value.currentPageModel
        if (currentTab.backStack.isNotEmpty() &&
            request.title == currentTab.backStack[currentTab.backStackPosition].title) {
            if (model?.page == null || request.options.isRefresh) {
                loadFromBackStack()
            } else if (!request.title.fragment.isNullOrEmpty()) {
                _pageLoadState.update {
                    it.copy(uiState = PageLoadUiState.Success(title = request.title, sectionAnchor = request.title.fragment))
                }
            }
            return
        }
        if (request.options.squashBackStack) {
            if (app.tabCount > 0) {
                app.tabList.last().clearBackstack()
            }
        }
        if (request.options.pushbackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem(webScrollY)
            currentTab.pushBackStackItem(PageBackStackItem(request.title, request.entry))
        }
        loadPageData(request)
    }

    private fun loadInNewForegroundTab(request: PageLoadRequest, webScrollY: Int) {
        createNewTab(request, isForeground = true)
        if (request.options.pushbackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem(webScrollY)
            currentTab.pushBackStackItem(PageBackStackItem(request.title, request.entry))
        }
        loadFromBackStack()
    }

    private fun loadInNewBackgroundTab(request: PageLoadRequest) {
        val isForeground = app.tabCount == 0
        if (isForeground) {
            createNewTab(request, isForeground = true)
            loadFromBackStack()
        } else {
            createNewTab(request, isForeground = false)
            _pageLoadState.update {
                it.copy(uiState = PageLoadUiState.Success(title = request.title, loadedFromBackground = true))
            }
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
        _pageLoadState.update { it.copy(uiState = PageLoadUiState.Loading(request.options.isRefresh)) }
        val result = pageDataFetcher.fetchPage(
            title = request.title,
            entry = request.entry,
            forceNetwork = request.options.isRefresh
        )
        when (result) {
            is PageResult.Success -> {
                val pageModel = createPageModel(request, result)
                if (request.title.namespace() == Namespace.SPECIAL) {
                    _pageLoadState.update { it.copy(uiState = PageLoadUiState.SpecialPage(request)) }
                    return
                }
                _pageLoadState.update {
                    it.copy(
                        uiState = PageLoadUiState.Success(result, request.title, request.options.stagedScrollY),
                        currentPageModel = pageModel
                    )
                }
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

    private fun createNewTab(request: PageLoadRequest, isForeground: Boolean) {
        val existingTabPosition = selectedTabPosition(request.title)
        if (existingTabPosition >= 0) {
            switchToExistingTab(existingTabPosition)
            return
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
            _pageLoadState.update { it.copy(isTabCreated = true) }
        } else {
            setTab(currentTab)
            currentTab.backStack.add(PageBackStackItem(request.title, request.entry))
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
        _pageLoadState.update { it.copy(uiState = PageLoadUiState.Error(throwable)) }
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

    data class PageLoadState(
        val uiState: PageLoadUiState = PageLoadUiState.Loading(),
        val currentPageModel: PageViewModel? = null,
        val isTabCreated: Boolean = false
    )
}
