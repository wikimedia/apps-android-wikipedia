package org.wikipedia.page.edithistory

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.dataclient.restbase.Metrics
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L
import retrofit2.HttpException
import java.io.IOException
import java.util.*

class EditHistoryListViewModel(bundle: Bundle) : ViewModel() {
    val editHistoryStatsData = MutableLiveData<Resource<EditHistoryStats>>()

    var pageTitle = bundle.parcelable<PageTitle>(Constants.ARG_TITLE)!!
    var pageId = -1
        private set
    var comparing = false
        private set
    var selectedRevisionFrom: MwQueryPage.Revision? = null
        private set
    var selectedRevisionTo: MwQueryPage.Revision? = null
        private set
    var currentQuery = ""
    var actionModeActive = false

    var editHistorySource: EditHistoryPagingSource? = null
    private val cachedRevisions = mutableListOf<MwQueryPage.Revision>()
    private var cachedContinueKey: String? = null

    val editHistoryFlow = Pager(PagingConfig(pageSize = 50), pagingSourceFactory = {
        editHistorySource = EditHistoryPagingSource(pageTitle)
        editHistorySource!!
    }).flow.map { pagingData ->
        val anonEditsOnly = Prefs.editHistoryFilterType == EditCount.EDIT_TYPE_ANONYMOUS
        val userEditsOnly = Prefs.editHistoryFilterType == EditCount.EDIT_TYPE_EDITORS

        pagingData.insertSeparators { before, after ->
            if (before != null && after != null) { before.diffSize = before.size - after.size }
            null
        }.filter {
            when {
                anonEditsOnly -> { it.isAnon || it.isTemp }
                userEditsOnly -> { !it.isAnon && !it.isTemp }
                else -> { true }
            }
        }.filter {
            if (currentQuery.isNotEmpty()) {
                it.comment.contains(currentQuery, true) ||
                        it.contentMain.contains(currentQuery, true) ||
                        it.user.contains(currentQuery, true)
            } else true
        }.map {
            EditHistoryItem(it)
        }.insertSeparators { before, after ->
            val dateBefore = before?.item?.localDateTime?.toLocalDate()
            val dateAfter = after?.item?.localDateTime?.toLocalDate()
            if (dateAfter != null && dateAfter != dateBefore) {
                EditHistorySeparator(DateUtil.getShortDateString(dateAfter))
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope)

    init {
        loadEditHistoryStatsAndEditCounts()
    }

    private fun loadEditHistoryStatsAndEditCounts() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            val calendar = Calendar.getInstance()
            val today = DateUtil.getYMDDateString(calendar.time)
            calendar.add(Calendar.YEAR, -1)
            val lastYear = DateUtil.getYMDDateString(calendar.time)

            val mwResponse = async { ServiceFactory.get(pageTitle.wikiSite).getRevisionDetailsAscending(pageTitle.prefixedText, null, 1, null) }
            val editCountsResponse = async { ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_EDITS) }
            val editCountsUserResponse = async { ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_EDITORS) }
            val editCountsAnonResponse = async { ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_ANONYMOUS) }
            val editCountsBotResponse = async { ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_BOT) }
            val articleMetricsResponse = async { ServiceFactory.getRest(WikiSite("wikimedia.org")).getArticleMetrics(pageTitle.wikiSite.authority(), pageTitle.prefixedText, lastYear, today) }

            val page = mwResponse.await().query?.pages?.first()
            pageId = page?.pageId ?: -1

            editHistoryStatsData.postValue(Resource.Success(EditHistoryStats(
                page?.revisions?.first()!!,
                articleMetricsResponse.await().firstItem.results,
                editCountsResponse.await(),
                editCountsUserResponse.await(),
                editCountsAnonResponse.await(),
                editCountsBotResponse.await()
            )))
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

    fun toggleSelectRevision(revision: MwQueryPage.Revision): Boolean {
        if (selectedRevisionFrom == null && selectedRevisionTo?.revId != revision.revId) {
            selectedRevisionFrom = revision
            return true
        } else if (selectedRevisionTo == null && selectedRevisionFrom?.revId != revision.revId) {
            selectedRevisionTo = revision
            return true
        } else if (selectedRevisionFrom?.revId == revision.revId) {
            selectedRevisionFrom = null
            return true
        } else if (selectedRevisionTo?.revId == revision.revId) {
            selectedRevisionTo = null
            return true
        }
        return false
    }

    fun getSelectedState(revision: MwQueryPage.Revision): Int {
        if (!comparing) {
            return SELECT_INACTIVE
        } else if (selectedRevisionFrom?.revId == revision.revId) {
            return SELECT_FROM
        } else if (selectedRevisionTo?.revId == revision.revId) {
            return SELECT_TO
        }
        return SELECT_NONE
    }

    fun clearCache() {
        cachedRevisions.clear()
    }

    inner class EditHistoryPagingSource(
        val pageTitle: PageTitle
    ) : PagingSource<String, MwQueryPage.Revision>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, MwQueryPage.Revision> {
            return try {
                if (params.key == null && cachedRevisions.isNotEmpty()) {
                    return LoadResult.Page(cachedRevisions, null, cachedContinueKey)
                }

                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                        .getRevisionDetailsDescending(pageTitle.prefixedText, 500, null, params.key)

                val revisions = response.query!!.pages?.first()?.revisions!!

                cachedContinueKey = response.continuation?.rvContinuation
                cachedRevisions.addAll(revisions)

                LoadResult.Page(revisions, null, cachedContinueKey)
            } catch (e: IOException) {
                LoadResult.Error(e)
            } catch (e: HttpException) {
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
    class EditHistoryStats(val revision: MwQueryPage.Revision, val metrics: List<Metrics.Results>,
                           val allEdits: EditCount, val userEdits: EditCount, val anonEdits: EditCount, val botEdits: EditCount) : EditHistoryItemModel()

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
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
