package org.wikipedia.diff

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.analytics.eventplatform.WatchlistAnalyticsHelper
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.restbase.Revision
import org.wikipedia.dataclient.rollback.RollbackPostResponse
import org.wikipedia.dataclient.watch.WatchPostResponse
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.descriptions.DescriptionEditFragment
import org.wikipedia.edit.Edit
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData
import org.wikipedia.watchlist.WatchlistExpiry

class ArticleEditDetailsViewModel(bundle: Bundle) : ViewModel() {

    private val invokeSource = bundle.getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource

    val watchedStatus = MutableLiveData<Resource<MwQueryPage>>()
    val rollbackRights = MutableLiveData<Resource<Boolean>>()
    val revisionDetails = MutableLiveData<Resource<Unit>>()
    val diffText = MutableLiveData<Resource<DiffResponse>>()
    val singleRevisionText = MutableLiveData<Resource<Revision>>()
    val thankStatus = SingleLiveData<Resource<EntityPostResponse>>()
    val watchResponse = SingleLiveData<Resource<WatchPostResponse>>()
    val undoEditResponse = SingleLiveData<Resource<Edit>>()
    val rollbackResponse = SingleLiveData<Resource<RollbackPostResponse>>()

    val fromRecentEdits = invokeSource == InvokeSource.SUGGESTED_EDITS_RECENT_EDITS

    var pageTitle = bundle.parcelable<PageTitle>(ArticleEditDetailsActivity.EXTRA_ARTICLE_TITLE)!!
        private set
    var pageId = bundle.getInt(ArticleEditDetailsActivity.EXTRA_PAGE_ID, -1)
        private set
    var revisionToId = bundle.getLong(ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_TO, -1)
    var revisionTo: MwQueryPage.Revision? = null
    var revisionFromId = bundle.getLong(ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_FROM, -1)
    var revisionFrom: MwQueryPage.Revision? = null
    var canGoForward = false
    var hasRollbackRights = false
    var isWatched = false

    var feedbackInput = ""

    val diffSize get() = if (revisionFrom != null) revisionTo!!.size - revisionFrom!!.size else revisionTo!!.size

    init {
        if (!fromRecentEdits) {
            getRevisionDetails(revisionToId, revisionFromId)
        } else {
            getNextRecentEdit()
        }
    }

    fun getRevisionDetails(revisionIdTo: Long, revisionIdFrom: Long = -1) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            revisionDetails.postValue(Resource.Error(throwable))
        }) {
            revisionToId = revisionIdTo
            if (watchedStatus.value !is Resource.Success) {
                val query = ServiceFactory.get(pageTitle.wikiSite).getWatchedStatusWithRights(pageTitle.prefixedText).query!!
                val page = query.firstPage()!!
                if (pageId < 0) {
                    pageId = page.pageId
                }
                if (revisionToId < 0) {
                    revisionToId = page.lastrevid
                }
                isWatched = page.watched
                watchedStatus.postValue(Resource.Success(page))
                hasRollbackRights = query.userInfo?.rights?.contains("rollback") == true
                rollbackRights.postValue(Resource.Success(hasRollbackRights))
            }
            if (revisionIdFrom >= 0) {
                val responseFrom = async { ServiceFactory.get(pageTitle.wikiSite).getRevisionDetailsWithInfo(pageId.toString(), 2, revisionIdFrom) }
                val responseTo = async { ServiceFactory.get(pageTitle.wikiSite).getRevisionDetailsWithInfo(pageId.toString(), 2, revisionToId) }
                val pageTo = responseTo.await().query?.firstPage()!!
                revisionFrom = responseFrom.await().query?.firstPage()!!.revisions[0]
                revisionTo = pageTo.revisions[0]
                canGoForward = revisionTo!!.revId < pageTo.lastrevid
            } else {
                val response = ServiceFactory.get(pageTitle.wikiSite).getRevisionDetailsWithInfo(pageId.toString(), 2, revisionToId)
                val page = response.query?.firstPage()!!
                val revisions = page.revisions
                revisionTo = revisions[0]
                canGoForward = revisions[0].revId < page.lastrevid
                revisionFrom = revisions.getOrNull(1)
            }

            revisionToId = revisionTo!!.revId
            revisionFromId = if (revisionFrom != null) revisionFrom!!.revId else revisionTo!!.parentRevId

            revisionDetails.postValue(Resource.Success(Unit))
            getDiffText(revisionFromId, revisionToId)
        }
    }

    private fun getNextRecentEdit() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            revisionDetails.postValue(Resource.Error(throwable))
        }) {
            val candidate = EditingSuggestionsProvider.getNextRevertCandidate(pageTitle.wikiSite.languageCode)
            pageId = candidate.pageid
            revisionToId = candidate.curRev

            val response = ServiceFactory.get(pageTitle.wikiSite).getRevisionDetailsWithUserInfo(pageId.toString(), 2, revisionToId)
            val page = response.query?.firstPage()!!
            val revisions = page.revisions

            pageTitle = PageTitle(page.title, pageTitle.wikiSite)
            pageTitle.displayText = page.displayTitle(pageTitle.wikiSite.languageCode)

            watchedStatus.postValue(Resource.Success(page))
            hasRollbackRights = response.query?.userInfo?.rights?.contains("rollback") == true
            rollbackRights.postValue(Resource.Success(hasRollbackRights))

            revisionTo = revisions[0]
            canGoForward = revisions[0].revId < page.lastrevid
            revisionFrom = revisions.getOrNull(1)

            revisionToId = revisionTo!!.revId
            revisionFromId = if (revisionFrom != null) revisionFrom!!.revId else revisionTo!!.parentRevId

            revisionDetails.postValue(Resource.Success(Unit))
            getDiffText(revisionFromId, revisionToId)
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
            val response = ServiceFactory.get(pageTitle.wikiSite).getRevisionDetailsAscending(null, pageId.toString(), 2, revisionIdFrom)
            val page = response.query?.firstPage()!!
            val revisions = page.revisions

            revisionFrom = revisions[0]
            revisionTo = revisions.getOrElse(1) { revisions.first() }
            canGoForward = revisions.size > 1 && revisions[1].revId < page.lastrevid

            revisionToId = revisionTo!!.revId
            revisionFromId = if (revisionFrom != null) revisionFrom!!.revId else revisionTo!!.parentRevId

            revisionDetails.postValue(Resource.Success(Unit))
            getDiffText(revisionFromId, revisionToId)
        }
    }

    private fun getDiffText(oldRevisionId: Long, newRevisionId: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            diffText.postValue(Resource.Error(throwable))
        }) {
            if (pageTitle.wikiSite.uri.authority == Uri.parse(Service.WIKIDATA_URL).authority) {
                // For the special case of Wikidata we return a blank Revision object, since the
                // Rest API in Wikidata cannot render diffs properly yet.
                // TODO: wait until Wikidata API returns diffs correctly
                singleRevisionText.postValue(Resource.Success(Revision()))
            } else if (oldRevisionId > 0) {
                diffText.postValue(Resource.Success(ServiceFactory.getCoreRest(pageTitle.wikiSite).getDiff(oldRevisionId, newRevisionId)))
            } else {
                singleRevisionText.postValue(Resource.Success(ServiceFactory.getCoreRest(pageTitle.wikiSite).getRevision(newRevisionId)))
            }
        }
    }

    fun sendThanks(wikiSite: WikiSite, revisionId: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            thankStatus.postValue(Resource.Error(throwable))
        }) {
            val token = ServiceFactory.get(wikiSite).getToken().query?.csrfToken()
            thankStatus.postValue(Resource.Success(ServiceFactory.get(wikiSite).postThanksToRevision(revisionId, token!!)))
        }
    }

    fun watchOrUnwatch(unwatch: Boolean) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            watchResponse.postValue(Resource.Error(throwable))
        }) {
            if (unwatch) {
                WatchlistAnalyticsHelper.logRemovedFromWatchlist(pageTitle)
            } else {
                WatchlistAnalyticsHelper.logAddedToWatchlist(pageTitle)
            }
            val token = ServiceFactory.get(pageTitle.wikiSite).getWatchToken().query?.watchToken()
            val response = ServiceFactory.get(pageTitle.wikiSite)
                    .watch(if (unwatch) 1 else null, null, pageTitle.prefixedText, WatchlistExpiry.NEVER.expiry, token!!)

            if (unwatch) {
                WatchlistAnalyticsHelper.logRemovedFromWatchlistSuccess(pageTitle)
            } else {
                WatchlistAnalyticsHelper.logAddedToWatchlistSuccess(pageTitle)
            }

            isWatched = response.getFirst()?.watched ?: false
            watchResponse.postValue(Resource.Success(response))
        }
    }

    @Suppress("KotlinConstantConditions")
    fun undoEdit(title: PageTitle, user: String, comment: String, revisionId: Long, revisionIdAfter: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            undoEditResponse.postValue(Resource.Error(throwable))
        }) {
            val msgResponse = ServiceFactory.get(title.wikiSite).getMessages("undo-summary", "$revisionId|$user")
            val undoMessage = msgResponse.query?.allmessages?.find { it.name == "undo-summary" }?.content
            var summary = if (undoMessage != null) "$undoMessage $comment" else comment
            if (fromRecentEdits) {
                summary += ", " + DescriptionEditFragment.SUGGESTED_EDITS_PATROLLER_TASKS_UNDO
            }
            val token = ServiceFactory.get(title.wikiSite).getToken().query!!.csrfToken()!!
            val undoResponse = ServiceFactory.get(title.wikiSite).postUndoEdit(title.prefixedText, summary,
                    null, token, revisionId, if (revisionIdAfter > 0) revisionIdAfter else null)
            undoEditResponse.postValue(Resource.Success(undoResponse))
        }
    }

    fun postRollback(title: PageTitle, user: String) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            rollbackResponse.postValue(Resource.Error(throwable))
        }) {

            val rollbackSummaryMsg = ServiceFactory.get(title.wikiSite).getMessages("revertpage", null)
                .query?.allmessages?.firstOrNull { it.name == "revertpage" }?.content

            val rollbackToken = ServiceFactory.get(title.wikiSite).getToken("rollback").query!!.rollbackToken()!!
            var summary = rollbackSummaryMsg
            if (fromRecentEdits) {
                summary += ", " + DescriptionEditFragment.SUGGESTED_EDITS_PATROLLER_TASKS_ROLLBACK
            }
            val rollbackPostResponse = ServiceFactory.get(title.wikiSite).postRollback(title.prefixedText, summary, user, rollbackToken)
            rollbackResponse.postValue(Resource.Success(rollbackPostResponse))
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ArticleEditDetailsViewModel(bundle) as T
        }
    }
}
