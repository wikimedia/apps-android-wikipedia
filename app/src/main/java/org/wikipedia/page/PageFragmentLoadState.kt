package org.wikipedia.page

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.page.Protection
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.leadimages.LeadImagesHandler
import org.wikipedia.page.tabs.Tab
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.log.L
import org.wikipedia.views.ObservableWebView

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

        // point of no return: null out the current page object.
        model.page = null
        model.readingListPage = null
        pageLoad()
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

                // Kick off loading mobile-html contents into the WebView.
                // The onPageFinished callback, which should be called by the WebView, will signal
                // the next stage of the loading process.
                bridge.resetHtml(title)

                // The final step is to fetch the watched status of the page (in the background),
                // but not if it's a Special page, which can't be watched.
                if (title.namespace() === Namespace.SPECIAL) {
                    return@launch
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

                val watchedResponse = watchedRequest.await()
                model.isWatched = watchedResponse.query?.firstPage()?.watched ?: false
                model.hasWatchlistExpiry = watchedResponse.query?.firstPage()?.hasWatchlistExpiry() ?: false

                fragment.updateQuickActionsAndMenuOptions()
                fragment.requireActivity().invalidateOptionsMenu()

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
