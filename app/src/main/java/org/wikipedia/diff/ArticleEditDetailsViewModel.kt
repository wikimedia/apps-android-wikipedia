package org.wikipedia.diff

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.analytics.WatchlistFunnel
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.watch.WatchPostResponse
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.watchlist.WatchlistExpiry

class ArticleEditDetailsViewModel : ViewModel() {

    val watchedStatus = MutableLiveData<Resource<MwQueryResponse>>()
    val revisionDetails = MutableLiveData<Resource<MwQueryResponse>>()
    val diffText = MutableLiveData<Resource<DiffResponse>>()
    val thankStatus = MutableLiveData<Resource<EntityPostResponse>>()
    val watchResponse = MutableLiveData<Resource<WatchPostResponse>>()
    var watchlistExpiryChanged = false
    var lastWatchExpiry = WatchlistExpiry.NEVER

    private val watchlistFunnel = WatchlistFunnel()

    fun getWatchedStatus(title: PageTitle) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            watchedStatus.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                watchedStatus.postValue(Resource.Success(ServiceFactory.get(title.wikiSite).getWatchedStatus(title.prefixedText)))
            }
        }
    }

    fun getRevisionDetails(title: PageTitle, revisionId: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            revisionDetails.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                revisionDetails.postValue(Resource.Success(ServiceFactory.get(title.wikiSite).getRevisionDetails(title.prefixedText, revisionId)))
            }
        }
    }

    fun getDiffText(wikiSite: WikiSite, oldRevisionId: Long, newRevisionId: Long) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            diffText.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                diffText.postValue(Resource.Success(ServiceFactory.getCoreRest(wikiSite).getDiff(oldRevisionId, newRevisionId)))
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
}
