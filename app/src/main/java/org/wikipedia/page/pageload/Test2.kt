//package org.wikipedia.page.pageload
//
//import org.wikipedia.WikipediaApp
//import org.wikipedia.bridge.CommunicationBridge
//import org.wikipedia.page.PageFragment
//import org.wikipedia.page.leadimages.LeadImagesHandler
//import org.wikipedia.views.ObservableWebView
//
//class PageLoader2(
//    private val fragment: PageFragment,
//    private val webView: ObservableWebView,
//    private val bridge: CommunicationBridge,
//    private val leadImagesHandler: LeadImagesHandler
//) {
//    private val dataFetcher = PageDataFetcher()
//    private val app = WikipediaApp.instance
//
//    fun loadPage(request: PageLoadRequest) {
//        when (determineLoadType(request)) {
//            is LoadType.CurrentTab -> loadInCurrentTab(request)
//            is LoadType.NewForegroundTab -> loadInNewForegroundTab(request)
//            is LoadType.NewBackgroundTab -> loadInNewBackgroundTab(request)
//            is LoadType.ExistingTab -> loadInExistingTab(request)
//            is LoadType.WithScrollPosition -> loadWithScrollPosition(request)
//        }
//    }
//
//    private fun loadInCurrentTab(request: PageLoadRequest) {
//        if (isSamePageLoaded(request)) {
//            handleSamePageLoad(request)
//            return
//        }
//
//        prepareForLoad(request)
//        executeLoad(request)
//    }
//
//    private fun loadInNewForegroundTab(request: PageLoadRequest) {
//        createNewTab(request, foreground = true)
//        executeLoad(request)
//    }
//
//    private fun loadInNewBackgroundTab(request: PageLoadRequest) {
//        createNewTab(request, foreground = false)
//        animateTabsButton()
//    }
//
//    private fun loadInExistingTab(request: PageLoadRequest) {
//        val existingTabPosition = findExistingTab(request.title)
//        if (existingTabPosition >= 0) {
//            switchToExistingTab(existingTabPosition)
//        } else {
//            loadInCurrentTab(request)
//        }
//    }
//
//    private fun loadWithScrollPosition(request: PageLoadRequest) {
//        prepareForLoad(request)
//        executeLoad(request)
//    }
//
//    private fun prepareForLoad(request: PageLoadRequest) {
//        fragment.clearActivityActionBarTitle()
//        fragment.dismissBottomSheet()
//
//        if (request.options.squashBackStack) {
//            squashBackstack()
//        }
//
//        fragment.updateProgressBar(true)
//        fragment.sidePanelHandler.setEnabled(false)
//        fragment.callback()?.onPageSetToolbarElevationEnabled(false)
//
//        // Update model
//        fragment.model.title = request.title
//        fragment.model.curEntry = request.entry
//        fragment.model.page = null
//        fragment.model.readingListPage = null
//        fragment.model.forceNetwork = request.options.isRefresh
//
//        // Clear previous state
//        fragment.errorState = false
//        fragment.binding.pageError.visibility = View.GONE
//        fragment.webView.visibility = View.VISIBLE
//        fragment.binding.pageActionsTabLayout.visibility = View.VISIBLE
//        fragment.binding.pageActionsTabLayout.enableAllTabs()
//
//        // Reset references and other state
//        fragment.references = null
//        fragment.revision = 0
//        fragment.pageRefreshed = request.options.isRefresh
//    }
//
//    private fun executeLoad(request: PageLoadRequest) {
//        if (request.options.pushBackStack) {
//            pushToBackstack(request.title, request.entry)
//        }
//
//        // Handle special pages
//        if (request.title.namespace() === Namespace.SPECIAL) {
//            handleSpecialPage(request)
//            return
//        }
//
//        // Regular page loading
//        fragment.lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
//            L.e("Page load error: ", throwable)
//            handleLoadError(throwable, request)
//        }) {
//            try {
//                updateLoadingState(LoadState.Loading)
//
//                val result = dataFetcher.fetchPage(
//                    request.title,
//                    request.entry,
//                    request.options.isRefresh
//                )
//
//                when (result) {
//                    is PageDataFetcher.PageResult.Success -> {
//                        updateLoadingState(LoadState.Success(result.page))
//                        handleLoadSuccess(result, request)
//                    }
//                    is PageDataFetcher.PageResult.Error -> {
//                        updateLoadingState(LoadState.Error(result.throwable))
//                        handleLoadError(result.throwable, request)
//                    }
//                }
//            } catch (e: Exception) {
//                updateLoadingState(LoadState.Error(e))
//                handleLoadError(e, request)
//            }
//        }
//    }
//
//    private fun handleLoadSuccess(result: PageDataFetcher.PageResult.Success, request: PageLoadRequest) {
//        // Update model
//        fragment.model.page = result.page
//        fragment.model.isWatched = result.isWatched
//        fragment.model.hasWatchlistExpiry = result.hasWatchlistExpiry
//        fragment.model.title = result.page.title
//
//        // Update UI
//        fragment.binding.pageRefreshContainer.isEnabled = true
//        fragment.binding.pageRefreshContainer.isRefreshing = false
//
//        // Load page content
//        if (!request.title.prefixedText.contains(":")) {
//            bridge.resetHtml(request.title)
//        }
//
//        leadImagesHandler.loadLeadImage()
//        fragment.onPageMetadataLoaded(result.redirectedFrom)
//
//        // Handle scroll position
//        if (request.options.stagedScrollY > 0) {
//            fragment.scrollTriggerListener.stagedScrollY = request.options.stagedScrollY
//        }
//
//        // Handle fragments
//        request.title.fragment?.let { fragment ->
//            if (fragment.isNotEmpty()) {
//                handleFragmentScroll(fragment)
//            }
//        }
//
//        fragment.updateQuickActionsAndMenuOptions()
//        fragment.requireActivity().invalidateOptionsMenu()
//    }
//
//    private fun handleLoadError(error: Throwable, request: PageLoadRequest) {
//        fragment.onPageLoadError(error)
//    }
//
//    private fun handleSpecialPage(request: PageLoadRequest) {
//        bridge.resetHtml(request.title)
//        leadImagesHandler.loadLeadImage()
//        fragment.requireActivity().invalidateOptionsMenu()
//        fragment.onPageMetadataLoaded()
//    }
//
//    private fun isSamePageLoaded(request: PageLoadRequest): Boolean {
//        return fragment.currentTab.backStack.isNotEmpty() &&
//                request.title == fragment.currentTab.backStack[fragment.currentTab.backStackPosition].title
//    }
//
//    private fun handleSamePageLoad(request: PageLoadRequest) {
//        if (fragment.model.page == null || request.options.isRefresh) {
//            fragment.pageFragmentLoadState.loadFromBackStack()
//        } else if (!request.title.fragment.isNullOrEmpty()) {
//            fragment.scrollToSection(request.title.fragment!!)
//        }
//    }
//
//    private fun createNewTab(request: PageLoadRequest, foreground: Boolean) {
//        val selectedTabPosition = findExistingTab(request.title)
//        if (selectedTabPosition >= 0) {
//            switchToExistingTab(selectedTabPosition)
//            return
//        }
//
//        if (fragment.shouldCreateNewTab) {
//            val tab = Tab()
//            val position = if (foreground) fragment.foregroundTabPosition else fragment.backgroundTabPosition
//
//            if (foreground) {
//                fragment.pageFragmentLoadState.setTab(tab)
//            }
//
//            app.tabList.add(position, tab)
//            trimTabCount()
//
//            tab.backStack.add(PageBackStackItem(request.title, request.entry))
//
//            if (!foreground) {
//                // Load metadata for background tab
//                loadBackgroundTabMetadata(request.title)
//            }
//
//            fragment.requireActivity().invalidateOptionsMenu()
//        } else {
//            fragment.pageFragmentLoadState.setTab(fragment.currentTab)
//            fragment.currentTab.backStack.add(PageBackStackItem(request.title, request.entry))
//        }
//    }
//
//    private fun findExistingTab(title: PageTitle): Int {
//        return app.tabList.indexOfFirst { tab ->
//            tab.backStackPositionTitle != null && title == tab.backStackPositionTitle
//        }
//    }
//
//    private fun switchToExistingTab(position: Int) {
//        if (position < app.tabList.size - 1) {
//            val tab = app.tabList.removeAt(position)
//            app.tabList.add(tab)
//            fragment.pageFragmentLoadState.setTab(tab)
//        }
//
//        if (app.tabCount > 0) {
//            app.tabList.last().squashBackstack()
//            fragment.pageFragmentLoadState.loadFromBackStack()
//        }
//    }
//
//    private fun pushToBackstack(title: PageTitle, entry: HistoryEntry) {
//        fragment.pageFragmentLoadState.updateCurrentBackStackItem()
//        fragment.currentTab.pushBackStackItem(PageBackStackItem(title, entry))
//    }
//
//    private fun squashBackstack() {
//        if (app.tabCount > 0) {
//            app.tabList.last().clearBackstack()
//        }
//    }
//
//    private fun trimTabCount() {
//        while (app.tabList.size > Constants.MAX_TABS) {
//            app.tabList.removeAt(0)
//        }
//    }
//
//    private fun loadBackgroundTabMetadata(title: PageTitle) {
//        fragment.lifecycleScope.launch(CoroutineExceptionHandler { _, t -> L.e(t) }) {
//            ServiceFactory.get(title.wikiSite)
//                .getInfoByPageIdsOrTitles(null, title.prefixedText)
//                .query?.firstPage()?.let { page ->
//                    app.tabList.find { it.backStackPositionTitle == title }
//                        ?.backStackPositionTitle?.apply {
//                            thumbUrl = page.thumbUrl()
//                            description = page.description
//                        }
//                }
//        }
//    }
//
//    private fun animateTabsButton() {
//        (fragment.requireActivity() as PageActivity).animateTabsButton()
//    }
//
//    private fun handleFragmentScroll(fragment: String) {
//        val scrollDelay = 100L
//        webView.postDelayed({
//            if (this.fragment.isAdded) {
//                this.fragment.scrollToSection(fragment)
//            }
//        }, scrollDelay)
//    }
//
//    private fun determineLoadType(request: PageLoadRequest): LoadType {
//        return when {
//            request.options.stagedScrollY > 0 -> LoadType.WithScrollPosition(request.options.stagedScrollY)
//            request.options.tabPosition == TabPosition.NEW_TAB_FOREGROUND -> LoadType.NewForegroundTab
//            request.options.tabPosition == TabPosition.NEW_TAB_BACKGROUND -> LoadType.NewBackgroundTab
//            request.options.tabPosition == TabPosition.EXISTING_TAB -> LoadType.ExistingTab
//            else -> LoadType.CurrentTab
//        }
//    }
//
//    private fun updateLoadingState(state: LoadState) {
//        when (state) {
//            is LoadState.Loading -> fragment.updateProgressBar(true)
//            is LoadState.Success -> fragment.updateProgressBar(false)
//            is LoadState.Error -> fragment.updateProgressBar(false)
//            else -> {}
//        }
//    }
//}
