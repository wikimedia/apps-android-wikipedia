package org.wikipedia.suggestededits

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class SuggestedEditsVandalismPatrolViewModel(private val langCode: String) : ViewModel() {

    val candidateLiveData = MutableLiveData<Resource<Pair<MwQueryResult.RecentChange, DiffResponse>>>()
    val rollbackResponse = SingleLiveData<Resource<Unit>>()

    init {
        getCandidate()
    }

    val wikiSite: WikiSite get() = WikiSite.forLanguageCode(langCode)
    var candidate: MwQueryResult.RecentChange? = null

    fun getCandidate() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            candidateLiveData.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                while (this.coroutineContext.isActive) {
                    try {
                        candidate = EditingSuggestionsProvider.getNextRevertCandidate(langCode)
                        val diff = ServiceFactory.getCoreRest(wikiSite)
                            .getDiff(candidate!!.revFrom, candidate!!.curRev)
                        candidateLiveData.postValue(Resource.Success(Pair(candidate!!, diff)))
                        break
                    } catch (e: EditingSuggestionsProvider.ListEmptyException) {
                        // continue indefinitely until new data comes in.
                        Thread.sleep(5000)
                    }
                }
            }
        }
    }

    fun doRollback() {
        if (candidate == null) {
            return
        }
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            rollbackResponse.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                val wiki = wikiSite
                val token = ServiceFactory.get(wiki).getToken("rollback").query!!.rollbackToken()!!
                ServiceFactory.get(wiki).postRollback(candidate!!.title, "", candidate!!.user, token)
                rollbackResponse.postValue(Resource.Success(Unit))
            }
        }
    }

    class Factory(private val langCode: String) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SuggestedEditsVandalismPatrolViewModel(langCode) as T
        }
    }
}
