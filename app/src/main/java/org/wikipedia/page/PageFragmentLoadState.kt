package org.wikipedia.page

import android.widget.Toast
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
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
import org.wikipedia.dataclient.page.Protection
import org.wikipedia.history.HistoryEntry
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.leadimages.LeadImagesHandler
import org.wikipedia.page.tabs.Tab
import org.wikipedia.pageimages.db.PageImage
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ObservableWebView
import retrofit2.Response
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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
        if (pushBackStack && model.title != null && model.curEntry != null) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem()
            currentTab.pushBackStackItem(PageBackStackItem(model.title!!, model.curEntry!!))
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
        fragment.loadPage(item.title, item.historyEntry, false, item.scrollY, isRefresh)
        L.d("Loaded page " + item.title.displayText + " from backstack")
    }

    fun updateCurrentBackStackItem() {
        if (currentTab.backStack.isEmpty()) {
            return
        }
        val item = currentTab.backStack[currentTab.backStackPosition]
        item.scrollY = webView.scrollY
        model.title?.let {
            item.title.description = it.description
            item.title.thumbUrl = it.thumbUrl
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
            disposables.add(Completable.fromAction { model.readingListPage = AppDatabase.instance.readingListPageDao().findPageInAnyList(it) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate { pageLoadFromNetwork { fragment.onPageLoadError(it) } }
                    .subscribe())
        }
    }

    private fun pageLoadFromNetwork(errorCallback: ErrorCallback) {
        model.title?.let { title ->
            fragment.updateQuickActionsAndMenuOptions()
            networkErrorCallback = errorCallback
            if (!fragment.isAdded) {
                return
            }
            fragment.requireActivity().invalidateOptionsMenu()
            fragment.callback()?.onPageUpdateProgressBar(true)
            model.page = null

            // kick off loading mobile-html contents into the WebView.
            bridge.resetHtml(title)

            if (title.namespace() === Namespace.SPECIAL) {
                // Short-circuit the entire process of fetching the Summary, since Special: pages
                // are not supported in RestBase.
                leadImagesHandler.loadLeadImage()
                fragment.requireActivity().invalidateOptionsMenu()
                fragment.onPageMetadataLoaded()
                return
            }

            disposables.add((if (app.isOnline && AccountUtil.isLoggedIn) ServiceFactory.get(title.wikiSite).getWatchedInfo(title.prefixedText)
                    else if (app.isOnline && !AccountUtil.isLoggedIn) AnonymousNotificationHelper.observableForAnonUserInfo(title.wikiSite)
                    else Observable.just(MwQueryResponse()))
                .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ watchedResponse ->
                        val isWatched = watchedResponse.query?.firstPage()?.watched ?: false
                        val hasWatchlistExpiry = watchedResponse.query?.firstPage()?.hasWatchlistExpiry() ?: false

                        //createPageModel(pageSummaryResponse, isWatched, hasWatchlistExpiry)
                        //if (OfflineCacheInterceptor.SAVE_HEADER_SAVE == pageSummaryResponse.headers()[OfflineCacheInterceptor.SAVE_HEADER]) {
                        //    showPageOfflineMessage(pageSummaryResponse.headers().getInstant("date"))
                        //}
                        // fragment.onPageMetadataLoaded()

                        if (AnonymousNotificationHelper.shouldCheckAnonNotifications(watchedResponse)) {
                            checkAnonNotifications(title)
                        }
                    }) {
                        L.e("Page details network response error: ", it)
                        commonSectionFetchOnCatch(it)
                    }
            )
        }
    }

    private fun checkAnonNotifications(title: PageTitle) {
        CoroutineScope(Dispatchers.Main).launch {
            val response = ServiceFactory.get(title.wikiSite).getLastModified(UserTalkAliasData.valueFor(title.wikiSite.languageCode) + ":" + Prefs.lastAnonUserWithMessages)
            if (AnonymousNotificationHelper.anonTalkPageHasRecentMessage(response, title)) {
                fragment.showAnonNotification()
            }
        }
    }

    private fun showPageOfflineMessage(dateHeader: Instant?) {
        if (!fragment.isAdded || dateHeader == null) {
            return
        }
        // TODO: Use LocalDate.ofInstant() instead once it is available in SDK 34.
        val localDate = LocalDateTime.ofInstant(dateHeader, ZoneId.systemDefault()).toLocalDate()
        val dateStr = DateUtil.getShortDateString(localDate)
        Toast.makeText(fragment.requireContext().applicationContext,
            fragment.getString(R.string.page_offline_notice_last_date, dateStr),
            Toast.LENGTH_LONG).show()
    }

    @Serializable
    class JsPageMetadata {
        val pageId: Int = 0
        val ns: Int = 0
        val revision: Long = 0
        val title: String = ""
        val timeStamp: String = ""
        val description: String = ""
        val descriptionSource: String = ""
        val wikibaseItem = ""
        val protection: Protection? = null
        val leadImage: JsLeadImage? = null
    }

    @Serializable
    class JsLeadImage {
        val source: String = ""
        val width: Int = 0
        val height: Int = 0
    }
}
