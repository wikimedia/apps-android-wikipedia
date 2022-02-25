package org.wikipedia.page.edithistory

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.flow.map
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil

class EditHistoryListViewModel(bundle: Bundle) : ViewModel() {

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

    private var selectedRevisionFrom: MwQueryPage.Revision? = null
    private var selectedRevisionTo: MwQueryPage.Revision? = null

    fun toggleSelectRevision(revision: MwQueryPage.Revision): Boolean {
        if (selectedRevisionFrom == null && selectedRevisionTo != revision) {
            selectedRevisionFrom = revision
            return true
        } else if (selectedRevisionTo == null && selectedRevisionFrom != revision) {
            selectedRevisionTo = revision
            return true
        } else if (selectedRevisionFrom == revision) {
            selectedRevisionFrom = null
            return true
        } else if (selectedRevisionTo == revision) {
            selectedRevisionTo = null
            return true
        }
        return false
    }

    fun getSelectedState(revision: MwQueryPage.Revision): Int {
        return when (revision) {
            selectedRevisionFrom -> SELECT_FROM
            selectedRevisionTo -> SELECT_TO
            else -> SELECT_NONE
        }
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
