package org.wikipedia.page.pageload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageBackStackItem
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.page.tabs.Tab
import org.wikipedia.util.Resource
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L
import retrofit2.Response

class PageLoadViewModel : ViewModel() {
    private val app = WikipediaApp.instance

    private val _pageLoadUiState = MutableStateFlow<PageLoadUiState>(PageLoadUiState.LoadingPrep())
    val pageLoadUiState = _pageLoadUiState.asStateFlow()

    private val _watchResponseState = MutableStateFlow<UiState<WatchStatus>>(UiState.Loading)
    val watchResponseState = _watchResponseState.asStateFlow()

    private val _categories = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categories = _categories.asStateFlow()

    private val _animateType = MutableSharedFlow<Animate>(replay = 1)
    val animateType = _animateType

    private val _currentPageViewModel = MutableStateFlow<PageViewModel>(PageViewModel())
    val currentPageViewModel = _currentPageViewModel.asStateFlow()

    // Internal state
    private var currentTab: Tab = app.tabList.last()
    private val pageDataFetcher = PageDataFetcher()
    val foregroundTabPosition get() = app.tabList.size
    val backgroundTabPosition get() = 0.coerceAtLeast(foregroundTabPosition - 1)

    fun loadPage(request: PageLoadRequest, webScrollY: Int = 0) {
        val loadType = determineLoadType(request)
        when (loadType) {
            LoadType.CurrentTab -> loadInCurrentTab(request, webScrollY)
            LoadType.ExistingTab -> loadInExistingTab(request)
            LoadType.FromBackStack -> {
                if (request.options.pushBackStack) {
                    // update the topmost entry in the backstack, before we start overwriting things.
                    updateCurrentBackStackItem(webScrollY)
                    currentTab.pushBackStackItem(PageBackStackItem(request.title, request.entry))
                }
                loadPageData(request)
            }
            LoadType.NewBackgroundTab -> loadInNewBackgroundTab(request)
            LoadType.NewForegroundTab -> loadInNewForegroundTab(request, webScrollY)
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
        _currentPageViewModel.value.title?.let {
            item.title.description = it.description
            item.title.thumbUrl = it.thumbUrl
        }
    }

    fun updateTabListToPreventZHVariantIssue(title: PageTitle?) {
        if (title == null) return
        WikipediaApp.instance.tabList.getOrNull(WikipediaApp.instance.tabCount - 1)?.setBackStackPositionTitle(title)
    }

    fun saveCategories(categories: List<Category>) {
        viewModelScope.launch {
            if (categories.isNotEmpty()) {
                AppDatabase.instance.categoryDao().upsertAll(categories)
            }
        }
    }

    fun saveInformationToDatabase(
        pageModel: PageViewModel,
        pageSummary: PageSummary,
        sendEvent: (HistoryEntry) -> Unit
    ) {
        val title = pageModel.title ?: return

        viewModelScope.launch {
            pageModel.curEntry?.let {
                val entry = HistoryEntry(
                    title,
                    it.source,
                    timestamp = it.timestamp
                ).apply {
                    referrer = it.referrer
                    prevId = it.prevId
                }
                pageModel.curEntry = entry
                // Insert and/or update this history entry in the DB
                AppDatabase.instance.historyEntryDao().upsert(entry).run {
                    pageModel.curEntry?.id = this
                }

                // Update metadata in the DB
                AppDatabase.instance.pageImagesDao().upsertForMetadata(entry, title.thumbUrl, title.description, pageSummary.coordinates?.latitude, pageSummary.coordinates?.longitude)

                // And finally, count this as a page view.
                sendEvent(entry)
            }
        }
    }

    fun goForward(): Boolean {
        if (currentTab.canGoForward()) {
            currentTab.moveForward()
            loadFromBackStack()
            return true
        }
        return false
    }

    fun goBack(): Boolean {
        if (currentTab.canGoBack()) {
            currentTab.moveBack()
            if (!backStackEmpty()) {
                loadFromBackStack()
                return true
            }
        }
        return false
    }

    fun updateWatchStatusInModel(watchStatus: WatchStatus) {
        _currentPageViewModel.update { currentModel ->
            currentModel.copy(
                isWatched = watchStatus.isWatched,
                hasWatchlistExpiry = watchStatus.hasWatchlistExpiry
            )
        }
    }

    private fun loadInExistingTab(request: PageLoadRequest) {
        val selectedTabPosition = selectedTabPosition(request.title)
        if (selectedTabPosition == -1) {
            loadPageData(request)
            return
        }
        switchToExistingTab(selectedTabPosition)
    }

    private fun loadInCurrentTab(request: PageLoadRequest, webScrollY: Int) {
        val model = _currentPageViewModel.value
        if (currentTab.backStack.isNotEmpty() &&
            request.title == currentTab.backStack[currentTab.backStackPosition].title) {
            if (model.page == null || request.options.isRefresh) {
                loadFromBackStack()
            } else if (!request.title.fragment.isNullOrEmpty()) {
                _animateType.tryEmit(Animate(sectionAnchor = request.title.fragment))
            }
            return
        }
        if (request.options.squashBackStack) {
            if (app.tabCount > 0) {
                app.tabList.last().clearBackstack()
            }
        }
        if (request.options.pushBackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem(webScrollY)
            currentTab.pushBackStackItem(PageBackStackItem(request.title, request.entry))
        }
        loadPageData(request)
    }

    private fun loadInNewForegroundTab(request: PageLoadRequest, webScrollY: Int) {
        createOrReuseExistingTab(request, isForeground = true)
        loadFromBackStack()
    }

    private fun loadInNewBackgroundTab(request: PageLoadRequest) {
        val isForeground = app.tabCount == 0
        if (isForeground) {
            createOrReuseExistingTab(request, isForeground = true)
            loadFromBackStack()
        } else {
            createOrReuseExistingTab(request, isForeground = false)
            _animateType.tryEmit(Animate(animateButtons = true))
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
                pushBackStack = false,
                stagedScrollY = item.scrollY,
                shouldLoadFromBackStack = true,
            ))
        )
        L.d("Loaded page " + item.title.displayText + " from backstack")
    }

    fun loadPageData(request: PageLoadRequest) {
        _pageLoadUiState.value = PageLoadUiState.LoadingPrep(isRefresh = request.options.isRefresh, title = request.title)
        if (request.title.namespace() == Namespace.SPECIAL) {
            _pageLoadUiState.value = PageLoadUiState.SpecialPage(request)
            return
        }
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            handleError(throwable)
        }) {
            val cacheControl = if (request.options.isRefresh) "no-cache" else "default"
            val pageSummary = pageDataFetcher.fetchPageSummary(request.title, cacheControl)
            handlePageSummary(pageSummary, request)

            val canMakeWatchRequest = WikipediaApp.instance.isOnline && AccountUtil.isLoggedIn
            if (canMakeWatchRequest) {
                val watchStatus = pageDataFetcher.fetchWatchStatus(request.title)
                handleWatchStatus(watchStatus, request.title)
            } else if (WikipediaApp.instance.isOnline) {
                val categoriesStatus = pageDataFetcher.fetchCategories(request.title)
                handleCategoriesStatus(categoriesStatus, request.title)
            }
        }
    }

    private suspend fun handlePageSummary(pageSummary: Response<PageSummary>, request: PageLoadRequest) {
        pageSummary.body()?.let { value ->
            val pageModel = createPageModel(request, pageSummary)
            _currentPageViewModel.value = pageModel
            _pageLoadUiState.value = PageLoadUiState.Success(
                result = value,
                title = request.title,
                stagedScrollY = request.options.stagedScrollY,
                redirectedFrom = if (pageSummary.raw().priorResponse?.isRedirect == true) request.title.displayText else null
            )
        }
    }

    private fun handleWatchStatus(watchStatus: Resource<WatchStatus>, title: PageTitle) {
        when (watchStatus) {
            is Resource.Success -> {
                _watchResponseState.value = UiState.Success(watchStatus.data)
                val categories = unwrapCategories(watchStatus.data.myQueryResponse, title)
                _categories.value = UiState.Success(categories)
            }
            is Resource.Error -> _watchResponseState.value = UiState.Error(watchStatus.throwable)
        }
    }

    private fun handleCategoriesStatus(categoriesStatus: Resource<MwQueryResponse>, title: PageTitle) {
        when (categoriesStatus) {
            is Resource.Success -> {
                val categories = unwrapCategories(categoriesStatus.data, title)
                _categories.value = UiState.Success(categories)
            }
            is Resource.Error -> { _categories.value = UiState.Error(categoriesStatus.throwable) }
        }
    }

    private fun unwrapCategories(response: MwQueryResponse, title: PageTitle): List<Category> {
        return response.query?.firstPage()?.categories?.map { category ->
            Category(title = category.title, lang = title.wikiSite.languageCode)
        } ?: emptyList()
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

    private fun createOrReuseExistingTab(request: PageLoadRequest, isForeground: Boolean) {
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

    private suspend fun createPageModel(request: PageLoadRequest, response: Response<PageSummary>): PageViewModel {
        return PageViewModel().apply {
            curEntry = request.entry
            forceNetwork = request.options.isRefresh
            readingListPage = AppDatabase.instance.readingListPageDao().findPageInAnyList(request.title)
            val pageSummary = response.body()
            page = pageSummary?.toPage(request.title)
            title = page?.title

            title?.let {
                if (!response.raw().request.url.fragment.isNullOrEmpty()) {
                    it.fragment = response.raw().request.url.fragment
                }
                if (it.description.isNullOrEmpty()) {
                    WikipediaApp.instance.appSessionEvent.noDescription()
                }
                if (!it.isMainPage) {
                    it.displayText = page?.displayTitle.orEmpty()
                }
                it.thumbUrl = pageSummary?.thumbnailUrl
            }
        }
    }

    private fun handleError(throwable: Throwable) {
        L.e(throwable)
        _pageLoadUiState.value = PageLoadUiState.Error(throwable)
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

    data class Animate(
        val animateButtons: Boolean = false,
        val sectionAnchor: String? = null
    )
}
