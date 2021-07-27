package org.wikipedia.page

import android.widget.Toast
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.leadimages.LeadImagesHandler
import org.wikipedia.page.tabs.Tab
import org.wikipedia.pageimages.db.PageImage
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ObservableWebView
import retrofit2.Response

class PageFragmentLoadState(private var model: PageViewModel,
                            private var fragment: PageFragment,
                            private var webView: ObservableWebView,
                            private var bridge: CommunicationBridge,
                            private var leadImagesHandler: LeadImagesHandler,
                            private var currentTab: Tab) {

    private fun interface ErrorCallback {
        fun call(error: Throwable)
    }

    private var networkErrorCallback: ErrorCallback? = null
    private val app = WikipediaApp.instance
    private val disposables = CompositeDisposable()

    fun load(pushBackStack: Boolean) {
        if (pushBackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem()
            currentTab.pushBackStackItem(PageBackStackItem(model.title, model.curEntry))
        }
        pageLoadCheckReadingLists()
    }

    fun loadFromBackStack(isRefresh: Boolean = false) {
        if (currentTab.backStack.isEmpty()) {
            return
        }
        val item = currentTab.backStack[currentTab.backStackPosition]
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        item.title?.let { title ->
            item.historyEntry?.let { entry ->
                fragment.loadPage(title, entry, false, item.scrollY, isRefresh)
                L.d("Loaded page " + item.title!!.displayText + " from backstack")
            }
        }
    }

    fun updateCurrentBackStackItem() {
        if (currentTab.backStack.isEmpty()) {
            return
        }
        val item = currentTab.backStack[currentTab.backStackPosition]
        item.scrollY = webView.scrollY
        item.title?.apply {
            model.title?.let {
                this.description = it.description
                this.thumbUrl = it.thumbUrl
            }
        }
    }

    fun setTab(tab: Tab) {
        currentTab = tab
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

    fun backStackEmpty(): Boolean {
        return currentTab.backStack.isEmpty()
    }

    fun onConfigurationChanged() {
        leadImagesHandler.loadLeadImage()
        bridge.execute(JavaScriptActionHandler.setTopMargin(leadImagesHandler.topMargin))
    }

    private fun commonSectionFetchOnCatch(caught: Throwable) {
        if (!fragment.isAdded) {
            return
        }
        val callback = networkErrorCallback
        networkErrorCallback = null
        fragment.requireActivity().invalidateOptionsMenu()
        callback?.call(caught)
    }

    private fun pageLoadCheckReadingLists() {
        model.title?.let {
            disposables.clear()
            disposables.add(Observable.fromCallable { AppDatabase.getAppDatabase().readingListPageDao().findPageInAnyList(it) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate { pageLoadFromNetwork { networkError -> fragment.onPageLoadError(networkError) } }
                    .subscribe({ page -> model.readingListPage = page }
                    ) { model.readingListPage = null }
            )
        }
    }

    private fun pageLoadFromNetwork(errorCallback: ErrorCallback) {
        model.title?.let { title ->
            fragment.updateBookmarkAndMenuOptions()
            networkErrorCallback = errorCallback
            if (!fragment.isAdded) {
                return
            }
            fragment.requireActivity().invalidateOptionsMenu()
            fragment.callback()?.onPageUpdateProgressBar(true)
            app.sessionFunnel.leadSectionFetchStart()
            model.page = null
            val delayLoadHtml = title.prefixedText.contains(":")
            if (!delayLoadHtml) {
                bridge.resetHtml(title)
            }
            if (title.namespace() === Namespace.SPECIAL) {
                // Short-circuit the entire process of fetching the Summary, since Special: pages
                // are not supported in RestBase.
                bridge.resetHtml(title)
                leadImagesHandler.loadLeadImage()
                fragment.requireActivity().invalidateOptionsMenu()
                fragment.onPageMetadataLoaded()
                return
            }
            disposables.add(Observable.zip(ServiceFactory.getRest(title.wikiSite)
                    .getSummaryResponse(title.prefixedText, null, model.cacheControl.toString(),
                            if (model.isInReadingList) OfflineCacheInterceptor.SAVE_HEADER_SAVE else null,
                            title.wikiSite.languageCode(), UriUtil.encodeURL(title.prefixedText)),
                    if (app.isOnline && AccountUtil.isLoggedIn) ServiceFactory.get(title.wikiSite).getWatchedInfo(title.prefixedText) else Observable.just(MwQueryResponse()), { first, second -> Pair(first, second) })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ pair ->
                        val pageSummaryResponse = pair.first
                        val watchedResponse = pair.second
                        val isWatched = watchedResponse?.query?.firstPage()?.isWatched ?: false
                        val hasWatchlistExpiry = watchedResponse?.query?.firstPage()?.hasWatchlistExpiry() ?: false
                        if (pageSummaryResponse.body() == null) {
                            throw RuntimeException("Summary response was invalid.")
                        }
                        createPageModel(pageSummaryResponse, isWatched, hasWatchlistExpiry)
                        if (OfflineCacheInterceptor.SAVE_HEADER_SAVE == pageSummaryResponse.headers()[OfflineCacheInterceptor.SAVE_HEADER]) {
                            showPageOfflineMessage(pageSummaryResponse.raw().header("date", ""))
                        }
                        if (delayLoadHtml) {
                            bridge.resetHtml(title)
                        }
                        fragment.onPageMetadataLoaded()
                    }) { throwable ->
                        L.e("Page details network response error: ", throwable)
                        commonSectionFetchOnCatch(throwable)
                    }
            )
        }
    }

    private fun showPageOfflineMessage(dateHeader: String?) {
        if (!fragment.isAdded || dateHeader.isNullOrEmpty()) {
            return
        }
        try {
            val dateStr = DateUtil.getShortDateString(DateUtil.getHttpLastModifiedDate(dateHeader))
            Toast.makeText(fragment.requireContext().applicationContext,
                    fragment.getString(R.string.page_offline_notice_last_date, dateStr),
                    Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun createPageModel(response: Response<PageSummary>,
                                isWatched: Boolean,
                                hasWatchlistExpiry: Boolean) {
        if (!fragment.isAdded || response.body() == null) {
            return
        }
        val pageSummary = response.body()
        val page = pageSummary?.toPage(model.title)
        model.page = page
        model.isWatched = isWatched
        model.hasWatchlistExpiry = hasWatchlistExpiry
        model.title = page?.title
        model.title?.let { title ->
            if (!response.raw().request.url.fragment.isNullOrEmpty()) {
                title.fragment = response.raw().request.url.fragment
            }
            if (title.description.isNullOrEmpty()) {
                app.sessionFunnel.noDescription()
            }
            if (!title.isMainPage) {
                title.setDisplayText(page?.displayTitle)
            }
            leadImagesHandler.loadLeadImage()
            fragment.requireActivity().invalidateOptionsMenu()

            // Update our history entry, in case the Title was changed (i.e. normalized)
            val curEntry = model.curEntry
            curEntry?.let {
                model.curEntry = HistoryEntry(title, it.source, timestamp = it.timestamp)
                model.curEntry!!.referrer = it.referrer
            }

            // Update our tab list to prevent ZH variants issue.
            if (app.tabList[app.tabCount - 1] != null) {
                app.tabList[app.tabCount - 1].setBackStackPositionTitle(title)
            }

            // Save the thumbnail URL to the DB
            val pageImage = PageImage(title, pageSummary?.thumbnailUrl)
            Completable.fromAction { AppDatabase.getAppDatabase().pageImagesDao().insertPageImage(pageImage) }.subscribeOn(Schedulers.io()).subscribe()
            title.thumbUrl = pageImage.imageName
        }
    }
}
