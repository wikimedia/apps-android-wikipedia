package org.wikipedia.usercontrib

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
            Constants.WIKI_CODE_COMMONS -> WikiSite(Service.COMMONS_URL)
            Constants.WIKI_CODE_WIKIDATA -> WikiSite(Service.WIKIDATA_URL)
            else -> WikiSite.forLanguageCode(langCode)
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
            val dateBefore = before?.item?.localDateTime?.toLocalDate()
            val dateAfter = after?.item?.localDateTime?.toLocalDate()
            if (dateAfter != null && dateAfter != dateBefore) {
                UserContribSeparator(DateUtil.getShortDateString(dateAfter))
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope)

    init {
        loadStats()
    }

    fun excludedFiltersCount(): Int {
        val excludedNsFilter = Prefs.userContribFilterExcludedNs
        return UserContribFilterActivity.NAMESPACE_LIST.count { excludedNsFilter.contains(it) }
    }

    fun loadStats() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            val messageName = "project-localized-name-${wikiSite.dbName()}"
            val query = ServiceFactory.get(wikiSite).userInfoWithMessages(userName, messageName).query

            userContribStatsData.postValue(Resource.Success(UserContribStats(query?.users!![0].editCount,
                    query.users[0].registrationDate, query.allmessages.orEmpty().getOrNull(0)?.content.orEmpty().ifEmpty { wikiSite.dbName() })))
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

                if (excludedFiltersCount() == UserContribFilterActivity.NAMESPACE_LIST.size) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                val nsFilter = if (Prefs.userContribFilterExcludedNs.isEmpty()) "" else
                    UserContribFilterActivity.NAMESPACE_LIST.filter { !Prefs.userContribFilterExcludedNs.contains(it) }.joinToString("|")

                val response = ServiceFactory.get(wikiSite).getUserContrib(userName, 500, nsFilter.ifEmpty { null }, null, params.key)
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
    class UserContribStats(val totalEdits: Int, val registrationDate: Date, val projectName: String) : UserContribItemModel()

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserContribListViewModel(bundle) as T
        }
    }
}
