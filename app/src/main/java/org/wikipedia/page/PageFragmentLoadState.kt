package org.wikipedia.page

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.leadimages.LeadImagesHandler
import org.wikipedia.page.tabs.Tab
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ObservableWebView
import retrofit2.Response
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PageFragmentLoadState(private var model: PageViewModel,
                            private var fragment: PageFragment,
                            private var webView: ObservableWebView,
                            private var bridge: CommunicationBridge,
                            private var leadImagesHandler: LeadImagesHandler,
                            private var currentTab: Tab) {

    fun load(pushBackStack: Boolean) {
        if (pushBackStack && model.title != null && model.curEntry != null) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem()
            currentTab.pushBackStackItem(PageBackStackItem(model.title!!, model.curEntry!!))
        }
        pageLoad()
    }

    fun loadFromBackStack() {
        if (currentTab.backStack.isEmpty()) {
            return
        }
        val item = currentTab.backStack[currentTab.backStackPosition]
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        fragment.loadPage(item.title, item.historyEntry, false, item.scrollY)
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

    fun setTab(tab: Tab): Boolean {
        val isDifferent = tab != currentTab
        currentTab = tab
        return isDifferent
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
        fragment.requireActivity().invalidateOptionsMenu()
        fragment.onPageLoadError(caught)
    }

    private fun pageLoad() {
        model.title?.let { title ->
            fragment.lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
                L.e("Page details network error: ", throwable)
                commonSectionFetchOnCatch(throwable)
            }) {
                model.readingListPage = AppDatabase.instance.readingListPageDao().findPageInAnyList(title)

                fragment.updateQuickActionsAndMenuOptions()
                fragment.requireActivity().invalidateOptionsMenu()
                fragment.callback()?.onPageUpdateProgressBar(true)
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
                    return@launch
                }

                val pageSummaryRequest = async {
                    ServiceFactory.getRest(title.wikiSite).getSummaryResponse(title.prefixedText, cacheControl = model.cacheControl.toString(),
                        saveHeader = if (model.isInReadingList) OfflineCacheInterceptor.SAVE_HEADER_SAVE else null,
                        langHeader = title.wikiSite.languageCode, titleHeader = UriUtil.encodeURL(title.prefixedText))
                }
                val watchedRequest = async {
                    if (WikipediaApp.instance.isOnline && AccountUtil.isLoggedIn) {
                        ServiceFactory.get(title.wikiSite).getWatchedStatus(title.prefixedText)
                    } else if (WikipediaApp.instance.isOnline && !AccountUtil.isLoggedIn) {
                        AnonymousNotificationHelper.observableForAnonUserInfo(title.wikiSite)
                    } else {
                        MwQueryResponse()
                    }
                }

                val pageSummaryResponse = pageSummaryRequest.await()
                val watchedResponse = watchedRequest.await()
                val isWatched = watchedResponse.query?.firstPage()?.watched == true
                val hasWatchlistExpiry = watchedResponse.query?.firstPage()?.hasWatchlistExpiry() == true
                if (pageSummaryResponse.body() == null) {
                    throw RuntimeException("Summary response was invalid.")
                }
                val redirectedFrom = if (pageSummaryResponse.raw().priorResponse?.isRedirect == true) model.title?.displayText else null
                createPageModel(pageSummaryResponse, isWatched, hasWatchlistExpiry)
                if (OfflineCacheInterceptor.SAVE_HEADER_SAVE == pageSummaryResponse.headers()[OfflineCacheInterceptor.SAVE_HEADER]) {
                    showPageOfflineMessage(pageSummaryResponse.headers().getInstant("date"))
                }
                if (delayLoadHtml) {
                    bridge.resetHtml(title)
                }
                fragment.onPageMetadataLoaded(redirectedFrom)

                if (AnonymousNotificationHelper.shouldCheckAnonNotifications(watchedResponse)) {
                    checkAnonNotifications(title)
                }
            }
        }
    }

    private fun checkAnonNotifications(title: PageTitle) {
        fragment.lifecycleScope.launch {
            val response = ServiceFactory.get(title.wikiSite)
                .getLastModified(UserTalkAliasData.valueFor(title.wikiSite.languageCode) + ":" + Prefs.lastAnonUserWithMessages)
            if (AnonymousNotificationHelper.anonTalkPageHasRecentMessage(response, title)) {
                fragment.showAnonNotification()
            }
        }
    }

    private fun showPageOfflineMessage(dateHeader: Instant?) {
        if (!fragment.isAdded || dateHeader == null) {
            return
        }
        val localDate = LocalDate.ofInstant(dateHeader, ZoneId.systemDefault())
        val dateStr = DateUtil.getShortDateString(localDate)
        Toast.makeText(fragment.requireContext().applicationContext,
            fragment.getString(R.string.page_offline_notice_last_date, dateStr),
            Toast.LENGTH_LONG).show()
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
                WikipediaApp.instance.appSessionEvent.noDescription()
            }
            if (!title.isMainPage) {
                title.displayText = page?.displayTitle.orEmpty()
            }
            title.thumbUrl = pageSummary?.thumbnailUrl
            leadImagesHandler.loadLeadImage()
            fragment.requireActivity().invalidateOptionsMenu()

            // Update our tab list to prevent ZH variants issue.
            WikipediaApp.instance.tabList.getOrNull(WikipediaApp.instance.tabCount - 1)?.setBackStackPositionTitle(title)

            // Update our history entry, in case the Title was changed (i.e. normalized)
            model.curEntry?.let {
                val entry = HistoryEntry(
                    title,
                    it.source,
                    timestamp = it.timestamp
                ).apply {
                    referrer = it.referrer
                    prevId = it.prevId
                }
                model.curEntry = entry

                MainScope().launch {
                    // Insert and/or update this history entry in the DB
                    AppDatabase.instance.historyEntryDao().upsert(entry).run {
                        model.curEntry?.id = this
                    }

                    // Update metadata in the DB
                    AppDatabase.instance.pageImagesDao().upsertForMetadata(entry, title.thumbUrl, title.description, pageSummary?.coordinates?.latitude, pageSummary?.coordinates?.longitude)
                }
            }
        }
    }
}
