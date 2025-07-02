package org.wikipedia.talk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.edit.Edit
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.talk.db.TalkPageSeen
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.TalkTopicsSortOverflowView
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistViewModel

class TalkTopicsViewModel(var pageTitle: PageTitle) : ViewModel() {

    private val talkPageDao = AppDatabase.instance.talkPageSeenDao()
    private val handler = CoroutineExceptionHandler { _, throwable ->
        uiState.value = UiState.LoadError(throwable)
    }
    private val actionHandler = CoroutineExceptionHandler { _, throwable ->
        actionState.value = ActionState.OnError(throwable)
    }

    private val threadItems = mutableListOf<ThreadItem>()
    private val seenThreadItemsSha = mutableSetOf<String>()

    var sortedThreadItems = listOf<ThreadItem>()
    var isWatched = false
    var hasWatchlistExpiry = false
    var currentSearchQuery: String? = null
        set(value) {
            field = value
            sortAndFilterThreadItems()
        }
    var currentSortMode = Prefs.talkTopicsSortMode
        set(value) {
            field = value
            Prefs.talkTopicsSortMode = field
            sortAndFilterThreadItems()
        }

    val uiState = MutableStateFlow(UiState())
    val actionState = MutableStateFlow(ActionState())

    init {
        loadTopics()
    }

    fun loadTopics() {
        // Determine whether we need to resolve the PageTitle, since the calling activity might
        // have given us a non-Talk page, and we need to prepend the correct namespace.
        var resolveTitleRequired = false
        if (pageTitle.namespace.isEmpty()) {
            pageTitle.namespace = TalkAliasData.valueFor(pageTitle.wikiSite.languageCode)
        } else if (pageTitle.isUserPage) {
            // Make sure to remove HTML tags and duplicated namespace if it is a user page.
            if (pageTitle.namespace() == Namespace.USER) {
                pageTitle.displayText = StringUtil.removeNamespace(StringUtil.removeHTMLTags(pageTitle.displayText))
            }
            pageTitle.namespace = UserTalkAliasData.valueFor(pageTitle.wikiSite.languageCode)
        } else if (pageTitle.namespace() != Namespace.TALK && pageTitle.namespace() != Namespace.USER_TALK) {
            // defer resolution of Talk page title for an API call.
            resolveTitleRequired = true
        }

        uiState.value = UiState.UpdateNamespace(pageTitle)

        viewModelScope.launch(handler) {
            if (resolveTitleRequired) {
                val siteInfoResponse = ServiceFactory.get(pageTitle.wikiSite).getPageNamespaceWithSiteInfo(pageTitle.prefixedText,
                        OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle.wikiSite.languageCode, UriUtil.encodeURL(pageTitle.prefixedText))
                resolveTitleRequired = false
                siteInfoResponse.query?.namespaces?.let { namespaces ->
                    siteInfoResponse.query?.firstPage()?.let { page ->
                        // In MediaWiki, namespaces that are even-numbered are "regular" pages,
                        // and namespaces that are odd-numbered are the "Talk" versions of the
                        // corresponding even-numbered namespace. For example, "User"=2, "User talk"=3.
                        // So then, if the namespace of our pageTitle is even (i.e. not a Talk page),
                        // then increment the namespace by 1, and update the pageTitle with it.
                        val newNs = namespaces.values.find { it.id == page.namespace().code() + 1 }
                        if (page.namespace().code() % 2 == 0 && newNs != null) {
                            pageTitle.namespace = newNs.name
                        }
                    }
                }
            }

            val discussionToolsInfoResponse = ServiceFactory.get(pageTitle.wikiSite).getTalkPageTopics(pageTitle.prefixedText,
                    OfflineCacheInterceptor.SAVE_HEADER_SAVE, pageTitle.wikiSite.languageCode, UriUtil.encodeURL(pageTitle.prefixedText))

            seenThreadItemsSha.clear()
            seenThreadItemsSha.addAll(talkPageDao.getAll().map { it.sha })

            threadItems.clear()
            threadItems.addAll(discussionToolsInfoResponse.pageInfo?.threads ?: emptyList())
            sortAndFilterThreadItems()

            val watchStatus = if (WikipediaApp.instance.isOnline && AccountUtil.isLoggedIn) ServiceFactory.get(pageTitle.wikiSite)
                    .getWatchedStatus(pageTitle.prefixedText).query?.firstPage()!! else MwQueryPage()
            isWatched = watchStatus.watched
            hasWatchlistExpiry = watchStatus.hasWatchlistExpiry()

            uiState.value = UiState.LoadTopic(pageTitle, threadItems)
        }
    }

    fun updatePageTitle(pageTitle: PageTitle) {
        this.pageTitle = pageTitle.copy()
        loadTopics()
    }

    fun undoSave(newRevisionId: Long, undoneSubject: CharSequence, undoneBody: CharSequence) {
        viewModelScope.launch(actionHandler) {
            val token = CsrfTokenClient.getToken(pageTitle.wikiSite)
            val undoResponse = ServiceFactory.get(pageTitle.wikiSite).postUndoEdit(title = pageTitle.prefixedText, undoRevId = newRevisionId, token = token)
            actionState.value = ActionState.UndoEdit(undoResponse, undoneSubject, undoneBody)
        }
    }

    fun markAsSeen(threadItem: ThreadItem?, force: Boolean = false, action: (() -> Unit)) {
        threadSha(threadItem)?.let {
            viewModelScope.launch(actionHandler) {
                if (topicSeen(threadItem) && !force) {
                    talkPageDao.deleteTalkPageSeen(it)
                    seenThreadItemsSha.remove(it)
                } else {
                    talkPageDao.insertTalkPageSeen(TalkPageSeen(it))
                    seenThreadItemsSha.add(it)
                }
                action()
            }
        }
    }

    fun topicSeen(threadItem: ThreadItem?): Boolean {
        return threadSha(threadItem)?.let {
            seenThreadItemsSha.any { sha -> sha == it }
        } ?: false
    }

    private fun threadSha(threadItem: ThreadItem?): String? {
        return threadItem?.let { it.id + "|" + it.allReplies.maxOfOrNull { reply -> reply.timestamp } }
    }

    fun subscribeTopic(commentName: String, subscribed: Boolean) {
        viewModelScope.launch(actionHandler) {
            val token = CsrfTokenClient.getToken(pageTitle.wikiSite)
            ServiceFactory.get(pageTitle.wikiSite).subscribeTalkPageTopic(pageTitle.prefixedText, commentName, token, if (!subscribed) true else null)
        }
    }

    private fun sortAndFilterThreadItems() {
        when (currentSortMode) {
            TalkTopicsSortOverflowView.SORT_BY_DATE_PUBLISHED_DESCENDING -> {
                threadItems.sortByDescending { it.replies.firstOrNull()?.date }
            }
            TalkTopicsSortOverflowView.SORT_BY_DATE_PUBLISHED_ASCENDING -> {
                threadItems.sortBy { it.replies.firstOrNull()?.date }
            }
            TalkTopicsSortOverflowView.SORT_BY_TOPIC_NAME_DESCENDING -> {
                threadItems.sortByDescending { RichTextUtil.stripHtml(it.html) }
            }
            TalkTopicsSortOverflowView.SORT_BY_TOPIC_NAME_ASCENDING -> {
                threadItems.sortBy { RichTextUtil.stripHtml(it.html) }
            }
            TalkTopicsSortOverflowView.SORT_BY_DATE_UPDATED_DESCENDING -> {
                threadItems.sortByDescending { it.replies.lastOrNull()?.date }
            }
            TalkTopicsSortOverflowView.SORT_BY_DATE_UPDATED_ASCENDING -> {
                threadItems.sortBy { it.replies.lastOrNull()?.date }
            }
        }

        // Regardless of sort order, always put header template at the top, if we have one.
        val headerItem = threadItems.find { it.othercontent.isNotEmpty() && TalkTopicActivity.isHeaderTemplate(it) }
        if (headerItem != null) {
            threadItems.remove(headerItem)
            threadItems.add(0, headerItem)
        }

        // Remove empty items to prevent empty content from being displayed.
        threadItems.removeIf { TalkTopicActivity.isHeaderTemplate(it) && it.replies.isEmpty() && it.othercontent.isEmpty() }

        sortedThreadItems = threadItems.filter { it.plainText.contains(currentSearchQuery.orEmpty(), true) ||
                it.plainOtherContent.contains(currentSearchQuery.orEmpty(), true) ||
                it.allReplies.any { reply -> reply.plainText.contains(currentSearchQuery.orEmpty(), true) ||
                        reply.author.contains(currentSearchQuery.orEmpty(), true) } }
    }

    suspend fun isSubscribed(commentName: String): Boolean {
        if (!WikipediaApp.instance.isOnline) {
            return false
        }
        val response = ServiceFactory.get(pageTitle.wikiSite).getTalkPageTopicSubscriptions(commentName)
        return response.subscriptions[commentName] == 1
    }

    fun watchOrUnwatch(expiry: WatchlistExpiry, unwatch: Boolean) {
        viewModelScope.launch(actionHandler) {
            val pair = WatchlistViewModel.watchPageTitle(this, pageTitle, unwatch, expiry, isWatched, pageTitle.namespace().talk())
            isWatched = pair.first
            hasWatchlistExpiry = expiry != WatchlistExpiry.NEVER
            // We have to send values to the object, even if we use the variables from ViewModel.
            // Otherwise the status will not be updated in the activity since the values in the object remains the same.
            actionState.value = ActionState.DoWatch(isWatched, pair.second, hasWatchlistExpiry)
        }
    }

    class Factory(private val pageTitle: PageTitle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TalkTopicsViewModel(pageTitle.copy()) as T
        }
    }

    open class UiState {
        data class UpdateNamespace(val pageTitle: PageTitle) : UiState()
        data class LoadTopic(val pageTitle: PageTitle,
                             val threadItems: List<ThreadItem>) : UiState()
        data class LoadError(val throwable: Throwable) : UiState()
    }

    open class ActionState {
        data class UndoEdit(val edit: Edit, val undoneSubject: CharSequence, val undoneBody: CharSequence) : ActionState()
        data class DoWatch(val isWatched: Boolean, val message: String, val hasWatchlistExpiry: Boolean) : ActionState()
        data class OnError(val throwable: Throwable) : ActionState()
    }
}
