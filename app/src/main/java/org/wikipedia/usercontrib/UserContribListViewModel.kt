package org.wikipedia.usercontrib

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.UserContribution
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L
import retrofit2.HttpException
import java.io.IOException
import java.util.*

class UserContribListViewModel(bundle: Bundle) : ViewModel() {

    val userContribStatsData = MutableLiveData<Resource<UserContribStats>>()

    var userName: String = bundle.getString(UserContribListActivity.INTENT_EXTRA_USER_NAME)!!
    var langCode: String = WikipediaApp.instance.appOrSystemLanguageCode

    val wikiSite get(): WikiSite {
        return when (langCode) {
            Constants.WIKI_CODE_COMMONS -> { WikiSite(Service.COMMONS_URL) }
            Constants.WIKI_CODE_WIKIDATA -> { WikiSite(Service.WIKIDATA_URL) }
            else -> { WikiSite.forLanguageCode(langCode) }
        }
    }

    var currentQuery = ""
    var actionModeActive = false

    var userContribSource: UserContribPagingSource? = null
    private val cachedContribs = mutableListOf<UserContribution>()
    private var cachedContinueKey: String? = null

    val userContribFlow = Pager(PagingConfig(pageSize = 50), pagingSourceFactory = {
        userContribSource = UserContribPagingSource()
        userContribSource!!
    }).flow.map { pagingData ->
        pagingData.filter {
            if (currentQuery.isNotEmpty()) {
                it.comment.contains(currentQuery, true) ||
                        it.title.contains(currentQuery, true)
            } else true
        }.map {
            UserContribItem(it)
        }.insertSeparators { before, after ->
            val dateBefore = if (before != null) DateUtil.getShortDateString(before.item.date()) else ""
            val dateAfter = if (after != null) DateUtil.getShortDateString(after.item.date()) else ""
            if (dateAfter.isNotEmpty() && dateAfter != dateBefore) {
                UserContribSeparator(dateAfter)
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope)

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            withContext(Dispatchers.IO) {
                val userInfo = ServiceFactory.get(wikiSite).userInfo(userName).query?.users!![0]
                userContribStatsData.postValue(Resource.Success(UserContribStats(userInfo.editCount, userInfo.registrationDate)))
            }
        }
    }

    fun clearCache() {
        cachedContribs.clear()
    }

    inner class UserContribPagingSource : PagingSource<String, UserContribution>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, UserContribution> {
            return try {
                if (params.key == null && cachedContribs.isNotEmpty()) {
                    return LoadResult.Page(cachedContribs, null, cachedContinueKey)
                }

                val nsFilter = Prefs.userContribFilterNs
                val response = ServiceFactory.get(wikiSite).getUserContrib(userName, 500, if (nsFilter >= 0) nsFilter else null, null, params.key)
                val contribs = response.query?.userContributions!!

                cachedContinueKey = response.continuation?.ucContinuation
                cachedContribs.addAll(contribs)

                LoadResult.Page(contribs, null, cachedContinueKey)
            } catch (e: IOException) {
                LoadResult.Error(e)
            } catch (e: HttpException) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<String, UserContribution>): String? {
            return null
        }
    }

    open class UserContribItemModel
    class UserContribItem(val item: UserContribution) : UserContribItemModel()
    class UserContribSeparator(val date: String) : UserContribItemModel()
    class UserContribStats(val totalEdits: Int, val registrationDate: Date) : UserContribItemModel()

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserContribListViewModel(bundle) as T
        }
    }
}
