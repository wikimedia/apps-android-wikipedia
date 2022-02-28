package org.wikipedia.diff

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData
import org.wikipedia.watchlist.WatchlistExpiry

class ArticleEditDetailsViewModel(bundle: Bundle) : ViewModel() {

    val watchedStatus = MutableLiveData<Resource<MwQueryResponse>>()
    val revisionDetails = MutableLiveData<Resource<Unit>>()
    val diffText = MutableLiveData<Resource<DiffResponse>>()
    val thankStatus = SingleLiveData<Resource<EntityPostResponse>>()
    val watchResponse = SingleLiveData<Resource<WatchPostResponse>>()
    val undoEditResponse = SingleLiveData<Resource<Edit>>()

    var watchlistExpiryChanged = false
    var lastWatchExpiry = WatchlistExpiry.NEVER

    val pageTitle = PageTitle(bundle.getString(ArticleEditDetailsActivity.EXTRA_ARTICLE_TITLE, ""),
            WikiSite.forLanguageCode(bundle.getString(ArticleEditDetailsActivity.EXTRA_EDIT_LANGUAGE_CODE,
                    AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE)))

    var revisionToId = bundle.getLong(ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_TO, 0)
    var revisionTo: MwQueryPage.Revision? = null
    var revisionFromId = bundle.getLong(ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_FROM, 0)
    var revisionFrom: MwQueryPage.Revision? = null
    var canGoForward = false

    private var diffRevisionId = 0L

    val diffSize get() = if (revisionFrom != null) revisionTo!!.size - revisionFrom!!.size else revisionTo!!.size

    private val watchlistFunnel = WatchlistFunnel()

    init {
        getWatchedStatus()
        getRevisionDetails(revisionToId, revisionFromId)
    }

    private fun getWatchedStatus() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            watchedStatus.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                watchedStatus.postValue(Resource.Success(ServiceFactory.get(pageTitle.wikiSite).getWatchedStatus(pageTitle.prefixedText)))
            }
        }
    }

    fun getRevisionDetails(revisionIdTo: Long, revisionIdFrom: Long = -1) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            revisionDetails.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                if (revisionIdFrom >= 0) {
                    var responseFrom: MwQueryResponse? = null
                    var responseTo: MwQueryResponse? = null
                    coroutineScope {
                        launch {
                            responseFrom = ServiceFactory.get(pageTitle.wikiSite).getRevisionDetails(pageTitle.prefixedText, revisionIdFrom, "older")
                        }
                        launch {
                            responseTo = ServiceFactory.get(pageTitle.wikiSite).getRevisionDetails(pageTitle.prefixedText, revisionIdTo, "older")
                        }
                    }

                    val pageTo = responseTo?.query?.firstPage()!!
                    revisionFrom = responseFrom?.query?.firstPage()!!.revisions[0]
                    revisionTo = pageTo.revisions[0]
                    canGoForward = revisionTo!!.revId < pageTo.lastrevid
                } else {
                    val response = ServiceFactory.get(pageTitle.wikiSite).getRevisionDetails(pageTitle.prefixedText, revisionIdTo, "older")
                    val page = response.query?.firstPage()!!
                    val revisions = page.revisions
                    revisionTo = revisions[0]
                    canGoForward = revisions[0].revId < page.lastrevid
                    revisionFrom = if (revisions.size > 1) { revisions[1] } else null
                }

                revisionToId = revisionTo!!.revId
                revisionFromId = if (revisionFrom != null) revisionFrom!!.revId else revisionTo!!.parentRevId

                revisionDetails.postValue(Resource.Success(Unit))
                getDiffText(revisionFromId, revisionToId)
            }
        }
    }

    fun goBackward() {
        revisionToId = revisionFromId
        getRevisionDetails(revisionToId)
    }

    fun goForward() {
        val revisionIdFrom = revisionToId
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            revisionDetails.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                val response = ServiceFactory.get(pageTitle.wikiSite).getRevisionDetails(pageTitle.prefixedText, revisionIdFrom, "newer")
                val page = response.query?.firstPage()!!
                val revisions = page.revisions

                revisionFrom = revisions[0]
                revisionTo = if (revisions.size > 1) { revisions[1] } else revisions[0]
                canGoForward = revisions.size > 1 && revisions[1].revId < page.lastrevid

                revisionToId = revisionTo!!.revId
                revisionFromId = if (revisionFrom != null) revisionFrom!!.revId else revisionTo!!.parentRevId

                revisionDetails.postValue(Resource.Success(Unit))
                getDiffText(revisionFromId, revisionToId)
            }
        }
    }

    private fun getDiffText(oldRevisionId: Long, newRevisionId: Long) {
        if (diffRevisionId == newRevisionId) {
            return
        }
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            diffText.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                diffText.postValue(Resource.Success(ServiceFactory.getCoreRest(pageTitle.wikiSite).getDiff(oldRevisionId, newRevisionId)))
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

    fun watchOrUnwatch(isWatched: Boolean, expiry: WatchlistExpiry, unwatch: Boolean) {
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
                val token = ServiceFactory.get(pageTitle.wikiSite).getWatchToken().query?.watchToken()
                val response = ServiceFactory.get(pageTitle.wikiSite)
                        .watch(if (unwatch) 1 else null, null, pageTitle.prefixedText, expiry.expiry, token!!)

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

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ArticleEditDetailsViewModel(bundle) as T
        }
    }
}
