package org.wikipedia.diff

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.wikipedia.analytics.WatchlistFunnel
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.watch.WatchPostResponse
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.edit.Edit
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData
import org.wikipedia.watchlist.WatchlistExpiry

class ArticleEditDetailsViewModel : ViewModel() {

    val watchedStatus = MutableLiveData<Resource<MwQueryResponse>>()
    val revisionDetails = MutableLiveData<Resource<RevisionDiffContainer>>()
    val diffText = MutableLiveData<Resource<DiffResponse>>()
    val thankStatus = SingleLiveData<Resource<EntityPostResponse>>()
    val watchResponse = SingleLiveData<Resource<WatchPostResponse>>()
    val undoEditResponse = SingleLiveData<Resource<Edit>>()

    var watchlistExpiryChanged = false
    var lastWatchExpiry = WatchlistExpiry.NEVER
    var curTitle: PageTitle? = null
    private var diffRevisionId = 0L

    private val watchlistFunnel = WatchlistFunnel()

    fun setup(pageTitle: PageTitle, revisionId: Long) {
        if (curTitle == null) {
            curTitle = pageTitle
            getWatchedStatus(pageTitle)
            getRevisionDetails(pageTitle, revisionId)
        }
    }

    private fun getWatchedStatus(title: PageTitle) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            watchedStatus.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                watchedStatus.postValue(Resource.Success(ServiceFactory.get(title.wikiSite).getWatchedStatus(title.prefixedText)))
            }
        }
    }

    fun getRevisionDetails(title: PageTitle, revisionIdTo: Long, revisionIdFrom: Long = -1) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            revisionDetails.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                val ret: RevisionDiffContainer
                if (revisionIdFrom >= 0) {
                    var responseFrom: MwQueryResponse? = null
                    var responseTo: MwQueryResponse? = null
                    coroutineScope {
                        launch {
                            responseFrom = ServiceFactory.get(title.wikiSite).getRevisionDetails(title.prefixedText, revisionIdFrom, "older")
                        }
                        launch {
                            responseTo = ServiceFactory.get(title.wikiSite).getRevisionDetails(title.prefixedText, revisionIdTo, "older")
                        }
                    }
                    ret = RevisionDiffContainer(responseFrom?.query?.firstPage()!!.revisions[0],
                            responseTo?.query?.firstPage()!!.revisions[0], false, false)
                } else {
                    val response = ServiceFactory.get(title.wikiSite).getRevisionDetails(title.prefixedText, revisionIdTo, "older")
                    val page = response.query?.firstPage()!!
                    val revisions = page.revisions
                    ret = if (revisions.size > 1) {
                        RevisionDiffContainer(revisions[1], revisions[0], true, revisions[0].revId < page.lastrevid)
                    } else {
                        RevisionDiffContainer(null, revisions[0], false, revisions[0].revId < page.lastrevid)
                    }
                }
                revisionDetails.postValue(Resource.Success(ret))
            }
        }
    }

    fun getRevisionDetailsNewer(title: PageTitle, revisionIdFrom: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            revisionDetails.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                val response = ServiceFactory.get(title.wikiSite).getRevisionDetails(title.prefixedText, revisionIdFrom, "newer")
                val page = response.query?.firstPage()!!
                val revisions = page.revisions
                val ret = if (revisions.size > 1) {
                    RevisionDiffContainer(revisions[0], revisions[1], revisions[0].parentRevId > 0, revisions[1].revId < page.lastrevid)
                } else {
                    RevisionDiffContainer(revisions[0], revisions[0], revisions[0].parentRevId > 0, false)
                }
                revisionDetails.postValue(Resource.Success(ret))
            }
        }
    }

    fun getDiffText(wikiSite: WikiSite, oldRevisionId: Long, newRevisionId: Long) {
        if (diffRevisionId == newRevisionId) {
            return
        }
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            diffText.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                diffText.postValue(Resource.Success(ServiceFactory.getCoreRest(wikiSite).getDiff(oldRevisionId, newRevisionId)))
                diffRevisionId = newRevisionId
            }
        }
    }

    fun sendThanks(wikiSite: WikiSite, revisionId: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            thankStatus.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                val token = ServiceFactory.get(wikiSite).getCsrfToken().query?.csrfToken()
                thankStatus.postValue(Resource.Success(ServiceFactory.get(wikiSite).postThanksToRevision(revisionId, token!!)))
            }
        }
    }

    fun watchOrUnwatch(title: PageTitle, isWatched: Boolean, expiry: WatchlistExpiry, unwatch: Boolean) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            watchResponse.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                if (expiry != WatchlistExpiry.NEVER) {
                    watchlistFunnel.logAddExpiry()
                } else {
                    if (isWatched) {
                        watchlistFunnel.logRemoveArticle()
                    } else {
                        watchlistFunnel.logAddArticle()
                    }
                }
                val token = ServiceFactory.get(title.wikiSite).getWatchToken().query?.watchToken()
                val response = ServiceFactory.get(title.wikiSite)
                        .watch(if (unwatch) 1 else null, null, title.prefixedText, expiry.expiry, token!!)

                lastWatchExpiry = expiry
                if (watchlistExpiryChanged && unwatch) {
                    watchlistExpiryChanged = false
                }

                if (unwatch) {
                    watchlistFunnel.logRemoveSuccess()
                } else {
                    watchlistFunnel.logAddSuccess()
                }
                watchResponse.postValue(Resource.Success(response))
            }
        }
    }

    fun undoEdit(title: PageTitle, user: String, comment: String, revisionId: Long, revisionIdAfter: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            undoEditResponse.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                val msgResponse = ServiceFactory.get(title.wikiSite).getMessages("undo-summary", "$revisionId|$user")
                val undoMessage = msgResponse.query?.allmessages?.find { it.name == "undo-summary" }?.content
                val summary = if (undoMessage != null) "$undoMessage $comment" else comment
                val token = ServiceFactory.get(title.wikiSite).getCsrfToken().query!!.csrfToken()!!
                val undoResponse = ServiceFactory.get(title.wikiSite).postUndoEdit(title.prefixedText, summary,
                        null, token, revisionId, if (revisionIdAfter > 0) revisionIdAfter else null)
                undoEditResponse.postValue(Resource.Success(undoResponse))
            }
        }
    }

    class RevisionDiffContainer(
            val revisionFrom: MwQueryPage.Revision?,
            val revisionTo: MwQueryPage.Revision,
            val canGoBack: Boolean,
            val canGoForward: Boolean
            )
}
