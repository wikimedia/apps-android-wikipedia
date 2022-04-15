package org.wikipedia.talk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.discussiontools.DiscussionToolsInfoResponse
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserTalkAliasData

class TalkTopicsViewModel(title: PageTitle) : ViewModel() {

    private val talkPageSeenRepository = TalkPageSeenRepository(AppDatabase.instance.talkPageSeenDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    private val pageTitle = title.copy()
    private var resolveTitleRequired = false

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState

    init {
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
    }

    fun load() {
        viewModelScope.launch(handler) {
            if (resolveTitleRequired) {
                val siteInfoResponse = withContext(Dispatchers.IO) {
                    ServiceFactory.get(pageTitle.wikiSite).getPageNamespaceWithSiteInfo(pageTitle.prefixedText)
                }
            }
        }
    }

    class Factory(private val pageTitle: PageTitle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TalkTopicsViewModel(pageTitle) as T
        }
    }

    open class UiState {
        data class Success(val discussionToolsInfoResponse: DiscussionToolsInfoResponse) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
