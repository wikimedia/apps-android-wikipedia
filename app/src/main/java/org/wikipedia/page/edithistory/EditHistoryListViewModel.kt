package org.wikipedia.page.edithistory

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.dataclient.restbase.Metrics
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil
import org.wikipedia.util.log.L
import java.util.*

class EditHistoryListViewModel(bundle: Bundle) : ViewModel() {

    private val _editHistoryStatsFlow = MutableStateFlow(EditHistoryItemModel())
    val editHistoryStatsFlow = _editHistoryStatsFlow

    var pageTitle: PageTitle = bundle.getParcelable(EditHistoryListActivity.INTENT_EXTRA_PAGE_TITLE)!!

    val editHistoryFlow = Pager(PagingConfig(pageSize = 10)) {
        EditHistoryPagingSource(pageTitle)
    }.flow.map { pagingData ->
        pagingData.map {
            EditHistoryItem(it)
        }.insertSeparators { before, after ->
            if (before != null && after != null) {
                before.item.diffSize = before.item.size - after.item.size
            }
            val dateBefore = if (before != null) DateUtil.getMonthOnlyDateString(DateUtil.iso8601DateParse(before.item.timeStamp)) else ""
            val dateAfter = if (after != null) DateUtil.getMonthOnlyDateString(DateUtil.iso8601DateParse(after.item.timeStamp)) else ""
            if (dateAfter.isNotEmpty() && dateAfter != dateBefore) {
                EditHistorySeparator(dateAfter)
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            withContext(Dispatchers.IO) {
                loadEditHistoryStats()
            }
        }
    }

    suspend fun loadEditHistoryStats() {

        var mwResponse: MwQueryResponse? = null
        var editCountsResponse: EditCount? = null
        var articleMetricsResponse: Metrics? = null

        val calendar = Calendar.getInstance()
        val today = DateUtil.getYMDDateString(calendar.time)
        calendar.add(Calendar.YEAR, -1)
        val lastYear = DateUtil.getYMDDateString(calendar.time)

        coroutineScope {
            launch {
                mwResponse = ServiceFactory.get(pageTitle.wikiSite).getArticleCreatedDate(pageTitle.prefixedText)
            }

            launch {
                editCountsResponse = ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_EDITS)
            }

            launch {
                articleMetricsResponse = ServiceFactory.getRest(WikiSite("wikimedia.org")).getArticleMetrics(pageTitle.wikiSite.authority(), pageTitle.prefixedText, lastYear, today)
            }
        }

        editHistoryStatsFlow.value = EditHistoryStats(mwResponse?.query?.pages?.first()?.revisions?.first()!!, editCountsResponse!!, articleMetricsResponse!!.firstItem.results)
    }

    class EditHistoryPagingSource(
            val pageTitle: PageTitle
    ) : PagingSource<String, MwQueryPage.Revision>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, MwQueryPage.Revision> {
            return try {
                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                        .getEditHistoryDetails(pageTitle.prefixedText, params.loadSize, params.key)
                LoadResult.Page(response.query!!.pages?.get(0)?.revisions!!, null, response.continuation?.rvContinuation)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<String, MwQueryPage.Revision>): String? {
            return null
        }
    }

    open class EditHistoryItemModel
    class EditHistoryItem(val item: MwQueryPage.Revision) : EditHistoryItemModel()
    class EditHistorySeparator(val date: String) : EditHistoryItemModel()
    class EditHistoryStats(val revision: MwQueryPage.Revision, val editCount: EditCount, val metrics: List<Metrics.Results>) : EditHistoryItemModel()

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EditHistoryListViewModel(bundle) as T
        }
    }
}
