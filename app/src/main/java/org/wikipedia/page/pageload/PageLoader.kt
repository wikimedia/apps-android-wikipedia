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
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageBackStackItem
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.page.leadimages.LeadImagesHandler
import org.wikipedia.page.tabs.Tab
import org.wikipedia.util.log.L
import org.wikipedia.views.ObservableWebView
import kotlin.text.get

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
            is LoadType.WithScrollPosition -> loadWithScrollPosition(request)
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
        val isForeground = if (app.tabCount == 0) true else false
        prepareForLoad(request)
        createNewTab(request, isForeground = isForeground)
        (fragment.requireActivity() as PageActivity).animateTabsButton()
        executeLoad(request)
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
        }else {
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
        loadPage(request = PageLoadRequest(title = item.title, entry = item.historyEntry, options = PageLoadOptions(pushbackStack = false, stagedScrollY = item.scrollY)))
        L.d("Loaded page " + item.title.displayText + " from backstack")
    }

    private fun prepareForLoad(request: PageLoadRequest) {
        fragment.clearActivityActionBarTitle()
        fragment.dismissBottomSheet()

        if (request.options.squashBackStack) {
            if (app.tabCount > 0) {
                app.tabList.last().clearBackstack()
            }
        }

        fragment.updateProgressBar(true)
        fragment.sidePanelHandler.setEnabled(false)
        fragment.callback()?.onPageSetToolbarElevationEnabled(false)

        // Update model
        fragment.model.title = request.title
        fragment.model.curEntry = request.entry
        fragment.model.page = null
        fragment.model.readingListPage = null
        fragment.model.forceNetwork = request.options.isRefresh

        // Clear previous state
        fragment.errorState = false
        fragment.binding.pageError.visibility = View.GONE
        fragment.webView.visibility = View.VISIBLE
        fragment.binding.pageActionsTabLayout.visibility = View.VISIBLE
        fragment.binding.pageActionsTabLayout.enableAllTabs()

        // Reset references and other state
        fragment.references = null
        fragment.revision = 0
        fragment.pageRefreshed = request.options.isRefresh
    }

    private fun executeLoad(request: PageLoadRequest) {
        if (request.options.pushbackStack) {
            updateCurrentBackStackItem()
            currentTab.pushBackStackItem(PageBackStackItem(request.title, request.entry))
        }

        if (request.title.namespace() == Namespace.SPECIAL) {
            handleSpecialPage(request)
            return
        }

        fragment.lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e("Page details network error: ", throwable)
            handleLoadError(throwable)
        }) {
            updateLoadingState(LoadState.Loading)
            val result = dataFetcher.fetchPage(
                request.title,
                request.entry,
                request.options.isRefresh
            )
            when (result) {
                is PageResult.Success -> {
                    updateLoadingState(LoadState.Success())
                    handleLoadSuccess(result, request)
                }
                is PageResult.Error -> {

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
        val response = result.pageSummaryResponse
        val pageSummary = response.body()
        val page = pageSummary?.toPage(fragment.model.title)
        fragment.model.page = page
        fragment.model.isWatched = result.isWatched
        fragment.model.hasWatchlistExpiry = result.hasWatchlistExpiry
        fragment.model.title = page?.title

        if (!request.title.prefixedText.contains(":")) {
            bridge.resetHtml(request.title)
        }

        if (request.options.stagedScrollY > 0) {
            fragment.scrollTriggerListener.stagedScrollY = request.options.stagedScrollY
        }

        fragment.updateQuickActionsAndMenuOptions()
        fragment.requireActivity().invalidateOptionsMenu()

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

    private fun updateLoadingState(state: LoadState) {
        when (state) {
            is LoadState.Loading -> fragment.updateProgressBar(true)
            is LoadState.Success -> fragment.updateProgressBar(false)
            is LoadState.Error -> fragment.updateProgressBar(false)
            else -> {}
        }
    }

    private fun determineLoadType(request: PageLoadRequest): LoadType {
        return when {
            request.options.stagedScrollY > 0 -> LoadType.WithScrollPosition(request.options.stagedScrollY)
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