package org.wikipedia.page.pageload

import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageBackStackItem
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.page.leadimages.LeadImagesHandler
import org.wikipedia.page.tabs.Tab
import org.wikipedia.util.log.L
import org.wikipedia.views.ObservableWebView

class PageLoader(
    private val fragment: PageFragment,
    private val webView: ObservableWebView,
    private val bridge: CommunicationBridge,
    private val leadImagesHandler: LeadImagesHandler,
    private var currentTab: Tab
) {
    private val dataFetcher = PageDataFetcher()
    private val app = WikipediaApp.instance

    fun loadPage(request: PageLoadRequest) {
        when (determineLoadType(request)) {
            is LoadType.CurrentTab -> loadInCurrentTab(request)
            is LoadType.NewForegroundTab -> loadInNewForegroundTab(request)
            is LoadType.NewBackgroundTab -> loadInNewBackgroundTab(request)
            is LoadType.ExistingTab -> loadInExistingTab(request)
            is LoadType.FromBackStack -> loadWithScrollPosition(request)
        }
    }

    private fun loadInCurrentTab(request: PageLoadRequest) {
        // is the new title the same as what's already being displayed?
        if (fragment.currentTab.backStack.isNotEmpty() &&
            request.title == fragment.currentTab.backStack[fragment.currentTab.backStackPosition].title) {
            if (fragment.model.page == null || request.options.isRefresh) {
                loadFromBackStack()
            } else if (!request.title.fragment.isNullOrEmpty()) {
                fragment.scrollToSection(request.title.fragment!!)
            }
            return
        }

        prepareForLoad(request)
        executeLoad(request)
    }

    fun backStackEmpty(): Boolean {
        return currentTab.backStack.isEmpty()
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

    fun goForward(): Boolean {
        if (currentTab.canGoForward()) {
            currentTab.moveForward()
            loadFromBackStack()
            return true
        }
        return false
    }

    private fun loadInNewForegroundTab(request: PageLoadRequest) {
        prepareForLoad(request)
        createNewTab(request, isForeground = true)
        executeLoad(request)
    }

    private fun loadInNewBackgroundTab(request: PageLoadRequest) {
        val isForeground = app.tabCount == 0
        if (isForeground) {
            createNewTab(request, isForeground = true)
            loadFromBackStack()
        } else {
            createNewTab(request, isForeground = false)
            (fragment.requireActivity() as PageActivity).animateTabsButton()
        }
    }

    private fun loadInExistingTab(request: PageLoadRequest) {
        val existingTabPosition = selectedTabPosition(request.title)
        if (existingTabPosition >= 0) {
            switchToExistingTab(existingTabPosition)
        } else {
            loadInCurrentTab(request)
        }
    }

    private fun loadWithScrollPosition(request: PageLoadRequest) {
        prepareForLoad(request)
        executeLoad(request)
    }

    private fun createNewTab(request: PageLoadRequest, isForeground: Boolean) {
        val selectedTabPosition = selectedTabPosition(request.title)
        if (selectedTabPosition >= 0) {
            switchToExistingTab(selectedTabPosition)
            return
        }

        if (fragment.shouldCreateNewTab) {
            val tab = Tab()
            val position = if (isForeground) fragment.foregroundTabPosition else fragment.backgroundTabPosition

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
            fragment.requireActivity().invalidateOptionsMenu()
        } else {
            setTab(fragment.currentTab)
            fragment.currentTab.backStack.add(PageBackStackItem(request.title, request.entry))
        }
    }

    fun setTab(tab: Tab): Boolean {
        val isDifferent = tab != currentTab
        currentTab = tab
        return isDifferent
    }

    private fun loadBackgroundTabMetadata(title: PageTitle) {
        fragment.lifecycleScope.launch(CoroutineExceptionHandler { _, t -> L.e(t) }) {
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

    private fun trimTabCount() {
        while (app.tabList.size > Constants.MAX_TABS) {
            app.tabList.removeAt(0)
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
                shouldLoadFromBackStack = true
            ))
        )
        L.d("Loaded page " + item.title.displayText + " from backstack")
    }

    private fun prepareForLoad(request: PageLoadRequest) {
        // added to fragment
        fragment.clearActivityActionBarTitle()
        fragment.dismissBottomSheet()

        // done added to load function
        if (request.options.squashBackStack) {
            if (app.tabCount > 0) {
                app.tabList.last().clearBackstack()
            }
        }

        // added to fragment
        fragment.updateProgressBar(true)
        fragment.sidePanelHandler.setEnabled(false)
        fragment.callback()?.onPageSetToolbarElevationEnabled(false)

        // done
        // Update model
        fragment.model.title = request.title
        fragment.model.curEntry = request.entry
        fragment.model.page = null
        fragment.model.readingListPage = null
        fragment.model.forceNetwork = request.options.isRefresh

        // done
        // Clear previous state
        fragment.errorState = false
        fragment.binding.pageError.visibility = View.GONE
        fragment.webView.visibility = View.VISIBLE
        fragment.binding.pageActionsTabLayout.visibility = View.VISIBLE
        fragment.binding.pageActionsTabLayout.enableAllTabs()

        // done
        // Reset references and other state
        fragment.references = null
        fragment.revision = 0
        fragment.pageRefreshed = request.options.isRefresh
    }

    private fun executeLoad(request: PageLoadRequest) {
        // added to load function
        if (request.options.pushbackStack) {
            updateCurrentBackStackItem()
            currentTab.pushBackStackItem(PageBackStackItem(request.title, request.entry))
        }

        // added to on success state because this check should run before
        if (request.title.namespace() == Namespace.SPECIAL) {
            handleSpecialPage(request)
            return
        }

        // done
        fragment.lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e("Page details network error: ", throwable)
            handleLoadError(throwable)
        }) {
            updateLoadingState(PageLoadUiState.Loading())
            val result = dataFetcher.fetchPage(
                request.title,
                request.entry,
                request.options.isRefresh
            )
            when (result) {
                is PageResult.Success -> {
                    // updateLoadingState(LoadState.Success())
                    handleLoadSuccess(result, request)
                }
                is PageResult.Error -> {
                    updateLoadingState(PageLoadUiState.Error(result.throwable))
                    handleLoadError(result.throwable)
                }
            }
        }
    }

    fun updateCurrentBackStackItem() {
        if (currentTab.backStack.isEmpty()) {
            return
        }
        val item = currentTab.backStack[currentTab.backStackPosition]
        item.scrollY = webView.scrollY
        fragment.model.title?.let {
            item.title.description = it.description
            item.title.thumbUrl = it.thumbUrl
        }
    }

    fun handleSpecialPage(request: PageLoadRequest) {
        bridge.resetHtml(request.title)
        leadImagesHandler.loadLeadImage()
        fragment.requireActivity().invalidateOptionsMenu()
        fragment.onPageMetadataLoaded()
    }

    private fun handleLoadSuccess(result: PageResult.Success, request: PageLoadRequest) {
        // done in createPageModel function
        // data update
        val response = result.pageSummaryResponse
        val pageSummary = response.body()
        val page = pageSummary?.toPage(fragment.model.title)
        fragment.model.page = page
        fragment.model.isWatched = result.isWatched
        fragment.model.hasWatchlistExpiry = result.hasWatchlistExpiry
        fragment.model.title = page?.title

        // in sucess state on fragment
        // ui update
        if (!request.title.prefixedText.contains(":")) {
            bridge.resetHtml(request.title)
        }

        // in sucess state on fragment
        // ui update
        // @TODO: scrollY is received from the item when loading back from stack done
        if (request.options.stagedScrollY > 0) {
            fragment.scrollTriggerListener.stagedScrollY = request.options.stagedScrollY
        }

        // in sucess state on fragment
        // ui update
        fragment.updateQuickActionsAndMenuOptions()
        fragment.requireActivity().invalidateOptionsMenu()

        // in sucess state on fragment
        // ui update
        leadImagesHandler.loadLeadImage()
        fragment.onPageMetadataLoaded(result.redirectedFrom)
    }

    private fun handleLoadError(error: Throwable) {
        if (!fragment.isAdded) {
            return
        }
        fragment.requireActivity().invalidateOptionsMenu()
        fragment.onPageLoadError(error)
    }

    private fun updateLoadingState(state: PageLoadUiState) {
        when (state) {
            is PageLoadUiState.Loading -> fragment.updateProgressBar(true)
            is PageLoadUiState.Success -> fragment.updateProgressBar(false)
            is PageLoadUiState.Error -> fragment.updateProgressBar(false)
            else -> {}
        }
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

    private fun selectedTabPosition(title: PageTitle): Int {
        return app.tabList.firstOrNull { it.backStackPositionTitle != null &&
                title == it.backStackPositionTitle }?.let { app.tabList.indexOf(it) } ?: -1
    }

    fun onConfigurationChanged() {
        leadImagesHandler.loadLeadImage()
        bridge.execute(JavaScriptActionHandler.setTopMargin(leadImagesHandler.topMargin))
    }
}
