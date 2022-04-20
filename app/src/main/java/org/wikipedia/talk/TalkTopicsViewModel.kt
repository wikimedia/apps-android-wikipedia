package org.wikipedia.talk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.DiscussionToolsInfoResponse
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.edit.Edit
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserTalkAliasData

class TalkTopicsViewModel : ViewModel() {

    private val talkPageSeenRepository = TalkPageSeenRepository(AppDatabase.instance.talkPageSeenDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    private lateinit var pageTitle: PageTitle
    private var resolveTitleRequired = false

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState

    fun loadTopics(page: PageTitle) {

        pageTitle = page.copy()

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

            _uiState.value = UiState.Success(pageTitle, discussionToolsInfoResponse.await(), lastModifiedResponse.await())
        }
    }

    fun undoSave(newRevisionId: Long, topicId: String, undoneSubject: String, undoneBody: String) {
        viewModelScope.launch(handler) {
            val token = withContext(Dispatchers.IO) {
                CsrfTokenClient(pageTitle.wikiSite).getToken()
            }
            val undoResponse =  ServiceFactory.get(pageTitle.wikiSite).postUndoEdit(title = pageTitle.prefixedText, undoRevId = newRevisionId, token = token)
            _uiState.value = UiState.UndoEdit(undoResponse, topicId, undoneSubject, undoneBody)
        }
    }

    open class UiState {
        data class Success(val pageTitle: PageTitle,
                           val discussionToolsInfoResponse: DiscussionToolsInfoResponse,
                           val lastModifiedResponse: MwQueryResponse) : UiState()
        data class UndoEdit(val edit: Edit, val topicId: String, val undoneSubject: String, val undoneBody: String): UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
