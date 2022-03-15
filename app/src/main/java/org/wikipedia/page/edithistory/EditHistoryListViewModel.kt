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
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.dataclient.restbase.Metrics
import org.wikipedia.dataclient.restbase.PageHistory
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil
import org.wikipedia.util.log.L
import java.util.*

class EditHistoryListViewModel(bundle: Bundle) : ViewModel() {

    val editHistoryStatsFlow = MutableStateFlow(EditHistoryItemModel())

    var pageTitle: PageTitle = bundle.getParcelable(EditHistoryListActivity.INTENT_EXTRA_PAGE_TITLE)!!
    var comparing = false
        private set
    var selectedRevisionFrom: PageHistory.Revision? = null
        private set
    var selectedRevisionTo: PageHistory.Revision? = null
        private set

    val editHistoryFlow = Pager(PagingConfig(pageSize = 20)) {
        EditHistoryPagingSource(pageTitle)
    }.flow.map { pagingData ->
        pagingData.map {
            EditHistoryItem(it)
        }.insertSeparators { before, after ->
            val dateBefore = if (before != null) DateUtil.getMonthOnlyDateString(DateUtil.iso8601DateParse(before.item.timestamp)) else ""
            val dateAfter = if (after != null) DateUtil.getMonthOnlyDateString(DateUtil.iso8601DateParse(after.item.timestamp)) else ""
            if (dateAfter.isNotEmpty() && dateAfter != dateBefore) {
                EditHistorySeparator(dateAfter)
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope)

    init {
        loadEditHistoryStats()
    }

    private fun loadEditHistoryStats() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            withContext(Dispatchers.IO) {

                val calendar = Calendar.getInstance()
                val today = DateUtil.getYMDDateString(calendar.time)
                calendar.add(Calendar.YEAR, -1)
                val lastYear = DateUtil.getYMDDateString(calendar.time)

                val mwResponse = async { ServiceFactory.get(pageTitle.wikiSite).getRevisionDetailsAscending(pageTitle.prefixedText, 0, null) }
                val editCountsResponse = async { ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_EDITS) }
                val articleMetricsResponse = async { ServiceFactory.getRest(WikiSite("wikimedia.org")).getArticleMetrics(pageTitle.wikiSite.authority(), pageTitle.prefixedText, lastYear, today) }

                editHistoryStatsFlow.value = EditHistoryStats(
                    mwResponse.await().query?.pages?.first()?.revisions?.first()!!,
                    editCountsResponse.await(),
                    articleMetricsResponse.await().firstItem.results
                )
            }
        }
    }

    fun toggleCompareState() {
        comparing = !comparing
        if (!comparing) {
            cancelSelectRevision()
        }
    }

    private fun cancelSelectRevision() {
        selectedRevisionFrom = null
        selectedRevisionTo = null
    }

    fun toggleSelectRevision(revision: PageHistory.Revision): Boolean {
        if (selectedRevisionFrom == null && selectedRevisionTo?.id != revision.id) {
            selectedRevisionFrom = revision
            return true
        } else if (selectedRevisionTo == null && selectedRevisionFrom?.id != revision.id) {
            selectedRevisionTo = revision
            return true
        } else if (selectedRevisionFrom?.id == revision.id) {
            selectedRevisionFrom = null
            return true
        } else if (selectedRevisionTo?.id == revision.id) {
            selectedRevisionTo = null
            return true
        }
        return false
    }

    fun getSelectedState(revision: PageHistory.Revision): Int {
        if (!comparing) {
            return SELECT_INACTIVE
        } else if (selectedRevisionFrom?.id == revision.id) {
            return SELECT_FROM
        } else if (selectedRevisionTo?.id == revision.id) {
            return SELECT_TO
        }
        return SELECT_NONE
    }

    class EditHistoryPagingSource(
            val pageTitle: PageTitle
    ) : PagingSource<Long, PageHistory.Revision>() {
        override suspend fun load(params: LoadParams<Long>): LoadResult<Long, PageHistory.Revision> {
            return try {
                val response = ServiceFactory.getCoreRest(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                        .getPageHistory(pageTitle.prefixedText, params.key, null)
                LoadResult.Page(response.revisions, null, if (response.older.isNullOrEmpty()) null else response.revisions.last().id)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Long, PageHistory.Revision>): Long? {
            return null
        }
    }

    open class EditHistoryItemModel
    class EditHistoryItem(val item: PageHistory.Revision) : EditHistoryItemModel()
    class EditHistorySeparator(val date: String) : EditHistoryItemModel()
    class EditHistoryStats(val revision: MwQueryPage.Revision, val editCount: EditCount, val metrics: List<Metrics.Results>) : EditHistoryItemModel()

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EditHistoryListViewModel(bundle) as T
        }
    }

    companion object {
        const val SELECT_INACTIVE = 0
        const val SELECT_NONE = 1
        const val SELECT_FROM = 2
        const val SELECT_TO = 3
    }
}
