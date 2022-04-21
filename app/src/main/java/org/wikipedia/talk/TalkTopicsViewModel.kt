package org.wikipedia.talk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.edit.Edit
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.views.TalkTopicsSortOverflowView

class TalkTopicsViewModel(var pageTitle: PageTitle?) : ViewModel() {

    private val talkPageSeenRepository = TalkPageSeenRepository(AppDatabase.instance.talkPageSeenDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    private var resolveTitleRequired = false
    val threadItems = mutableListOf<ThreadItem>()
    var currentSortMode = Prefs.talkTopicsSortMode
    var currentSearchQuery: String? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState

    init {
        loadTopics()
    }

    fun loadTopics() {
        if (pageTitle == null) {
            return
        }
        val pageTitle = pageTitle!!

        // Determine whether we need to resolve the PageTitle, since the calling activity might
        // have given us a non-Talk page, and we need to prepend the correct namespace.
        if (pageTitle.namespace.isEmpty()) {
            pageTitle.namespace = TalkAliasData.valueFor(pageTitle.wikiSite.languageCode)
        } else if (pageTitle.isUserPage) {
            pageTitle.namespace = UserTalkAliasData.valueFor(pageTitle.wikiSite.languageCode)
        } else if (pageTitle.namespace() != Namespace.TALK && pageTitle.namespace() != Namespace.USER_TALK) {
            // defer resolution of Talk page title for an API call.
            resolveTitleRequired = true
        }

        viewModelScope.launch(handler) {
            if (resolveTitleRequired) {
                val siteInfoResponse = withContext(Dispatchers.IO) {
                    ServiceFactory.get(pageTitle.wikiSite).getPageNamespaceWithSiteInfo(pageTitle.prefixedText)
                }
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

            val discussionToolsInfoResponse = async { ServiceFactory.get(pageTitle.wikiSite).getTalkPageTopics(pageTitle.prefixedText) }
            val lastModifiedResponse = async { ServiceFactory.get(pageTitle.wikiSite).getLastModified(pageTitle.prefixedText) }

            threadItems.clear()
            threadItems.addAll(discussionToolsInfoResponse.await().pageInfo?.threads ?: emptyList())

            _uiState.value = UiState.Success(pageTitle, threadItems, lastModifiedResponse.await())
        }
    }

    val sortedThreadItems get(): List<ThreadItem> {
        when (currentSortMode) {
            TalkTopicsSortOverflowView.SORT_BY_DATE_PUBLISHED_DESCENDING -> {
                threadItems.sortByDescending { it.id }
            }
            TalkTopicsSortOverflowView.SORT_BY_DATE_PUBLISHED_ASCENDING -> {
                threadItems.sortBy { it.id }
            }
            TalkTopicsSortOverflowView.SORT_BY_TOPIC_NAME_DESCENDING -> {
                threadItems.sortByDescending { RichTextUtil.stripHtml(it.html) }
            }
            TalkTopicsSortOverflowView.SORT_BY_TOPIC_NAME_ASCENDING -> {
                threadItems.sortBy { RichTextUtil.stripHtml(it.html) }
            }
        }
        return threadItems.filter { it.html.contains(currentSearchQuery.orEmpty(), true) }
    }

    fun undoSave(newRevisionId: Long, topicId: String, undoneSubject: String, undoneBody: String) {
        if (pageTitle == null) {
            return
        }
        val pageTitle = pageTitle!!
        viewModelScope.launch(handler) {
            val token = withContext(Dispatchers.IO) {
                CsrfTokenClient(pageTitle.wikiSite).token.blockingFirst()
            }
            val undoResponse = ServiceFactory.get(pageTitle.wikiSite).postUndoEdit(title = pageTitle.prefixedText, undoRevId = newRevisionId, token = token)
            _uiState.value = UiState.UndoEdit(undoResponse, topicId, undoneSubject, undoneBody)
        }
    }

    class Factory(private val pageTitle: PageTitle?) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TalkTopicsViewModel(pageTitle) as T
        }
    }

    open class UiState {
        data class Success(val pageTitle: PageTitle,
                           val threadItems: List<ThreadItem>,
                           val lastModifiedResponse: MwQueryResponse) : UiState()
        data class UndoEdit(val edit: Edit, val topicId: String, val undoneSubject: String, val undoneBody: String) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
