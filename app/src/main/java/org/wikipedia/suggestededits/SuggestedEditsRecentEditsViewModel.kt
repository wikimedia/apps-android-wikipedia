package org.wikipedia.suggestededits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.flow.map
import org.wikipedia.Constants
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import retrofit2.HttpException
import java.io.IOException
import java.util.Date

class SuggestedEditsRecentEditsViewModel : ViewModel() {

    var langCode = Prefs.recentEditsWikiCode

    val wikiSite get(): WikiSite {
        return when (langCode) {
            Constants.WIKI_CODE_COMMONS -> WikiSite(Service.COMMONS_URL)
            Constants.WIKI_CODE_WIKIDATA -> WikiSite(Service.WIKIDATA_URL)
            else -> WikiSite.forLanguageCode(langCode)
        }
    }
    var currentQuery = ""
    var actionModeActive = false
    var recentEditsSource: RecentEditsPagingSource? = null

    private val cachedRecentEdits = mutableListOf<MwQueryResult.RecentChange>()
    private var cachedContinueKey: String? = null

    val recentEditsFlow = Pager(PagingConfig(pageSize = 50), pagingSourceFactory = {
        recentEditsSource = RecentEditsPagingSource()
        recentEditsSource!!
    }).flow.map { pagingData ->
        pagingData.filter {
            if (currentQuery.isNotEmpty()) {
                it.parsedComment.contains(currentQuery, true) ||
                        it.title.contains(currentQuery, true) ||
                        it.user.contains(currentQuery, true) ||
                        it.joinedTags.contains(currentQuery, true) ||
                        it.parsedDateTime.toString().contains(currentQuery, true)
            } else true
        }.map {
            RecentEditsItem(it)
        }.insertSeparators { before, after ->
            val dateBefore = before?.item?.parsedDateTime?.toLocalDate()
            val dateAfter = after?.item?.parsedDateTime?.toLocalDate()
            if (dateAfter != null && dateAfter != dateBefore) {
                RecentEditsSeparator(DateUtil.getShortDateString(dateAfter))
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope)

    fun clearCache() {
        cachedRecentEdits.clear()
    }

    fun filtersCount(): Int {
        val defaultTypeSet = SuggestedEditsRecentEditsFilterTypes.DEFAULT_FILTER_TYPE_SET.map { it.id }.toSet()
        val nonDefaultChangeTypes = Prefs.recentEditsIncludedTypeCodes.subtract(defaultTypeSet)
            .union(defaultTypeSet.subtract(Prefs.recentEditsIncludedTypeCodes.toSet()))
        return nonDefaultChangeTypes.size
    }

    private fun latestRevisions(): String? {
        val includedTypesCodes = Prefs.recentEditsIncludedTypeCodes
        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.LATEST_REVISIONS_GROUP.map { it.id }) &&
            !includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.LATEST_REVISION.id)) {
            return SuggestedEditsRecentEditsFilterTypes.NOT_LATEST_REVISION.value
        }
        return null
    }

    private fun showCriteriaString(): String {
        val includedTypesCodes = Prefs.recentEditsIncludedTypeCodes
        val list = mutableListOf<String>()

        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.BOT_EDITS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.BOT.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.BOT.value)
            }
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.HUMAN.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.HUMAN.value)
            }
        }

        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS.value)
            }
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.NON_MINOR_EDITS.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.NON_MINOR_EDITS.value)
            }
        }

        if (!includedTypesCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.USER_STATUS_GROUP.map { it.id })) {
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.REGISTERED.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.REGISTERED.value)
            }
            if (includedTypesCodes.contains(SuggestedEditsRecentEditsFilterTypes.UNREGISTERED.id)) {
                list.add(SuggestedEditsRecentEditsFilterTypes.UNREGISTERED.value)
            }
        }
        return list.joinToString(separator = "|")
    }

    inner class RecentEditsPagingSource : PagingSource<String, MwQueryResult.RecentChange>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, MwQueryResult.RecentChange> {
            return try {
                if (params.key == null && cachedRecentEdits.isNotEmpty()) {
                    return LoadResult.Page(cachedRecentEdits, null, cachedContinueKey)
                }

                val response = ServiceFactory.get(wikiSite)
                    .getRecentEdits(params.loadSize, Date().toInstant().toString(), latestRevisions(), showCriteriaString(), params.key)

                val recentChanges = response.query?.recentChanges.orEmpty()

                cachedContinueKey = response.continuation?.rcContinuation
                cachedRecentEdits.addAll(recentChanges)

                LoadResult.Page(recentChanges, null, cachedContinueKey)
            } catch (e: IOException) {
                LoadResult.Error(e)
            } catch (e: HttpException) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<String, MwQueryResult.RecentChange>): String? {
            return null
        }
    }

    open class RecentEditsItemModel
    class RecentEditsItem(val item: MwQueryResult.RecentChange) : RecentEditsItemModel()
    class RecentEditsSeparator(val date: String) : RecentEditsItemModel()
}
