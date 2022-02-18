package org.wikipedia.page.edit_history

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.dataclient.restbase.Metrics
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.Resource.Success
import org.wikipedia.util.log.L
import java.util.*

class EditHistoryListViewModel : ViewModel() {

    val editHistoryListData = MutableLiveData<Resource<List<Any>>>()

    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
    }

    fun fetchData(pageTitle: PageTitle) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                val list = mutableListOf<Any>()

                // Edit history stats
                val calendar = Calendar.getInstance()
                val today = DateUtil.getYMDDateString(calendar.time)
                calendar.add(Calendar.YEAR, -1)
                val lastYear = DateUtil.getYMDDateString(calendar.time)

                val mwResponse = ServiceFactory.get(pageTitle.wikiSite).getArticleCreatedDate(pageTitle.prefixedText)
                val editCountsResponse = ServiceFactory.getCoreRest(pageTitle.wikiSite).getEditCount(pageTitle.prefixedText, EditCount.EDIT_TYPE_EDITS)
                val articleMetricsResponse = ServiceFactory.getRest(WikiSite("wikimedia.org"))
                    .getArticleMetrics(pageTitle.wikiSite.authority(), pageTitle.prefixedText, lastYear, today)
                list.add(EditStats(mwResponse.query?.pages?.first()?.revisions?.first()!!, editCountsResponse, articleMetricsResponse.firstItem.results))

                // Edit history
                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode)).getEditHistoryDetails(pageTitle.prefixedText)
                val revisions = response.query!!.pages?.get(0)?.revisions!!
                list.addAll(revisions)
                editHistoryListData.postValue(Success(list))
            }
        }
    }

    suspend fun fetchDiffSize(languageCode: String, olderRevisionId: Long, revisionId: Long): Int {
        val response: DiffResponse = ServiceFactory.getCoreRest(WikiSite.forLanguageCode(languageCode))
            .getEditDiff(olderRevisionId, revisionId)
        var diffSize = 0
        for (diff in response.diff) {
            when (diff.type) {
                DiffResponse.DIFF_TYPE_LINE_ADDED -> {
                    diffSize += diff.text.length + 1
                }
                DiffResponse.DIFF_TYPE_LINE_REMOVED -> {
                    diffSize -= diff.text.length + 1
                }
                DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_FROM -> {
                    diffSize -= diff.text.length + 1
                }
                DiffResponse.DIFF_TYPE_PARAGRAPH_MOVED_TO -> {
                    diffSize += diff.text.length + 1
                }
            }

            if (diff.highlightRanges.isNotEmpty()) {
                for (editRange in diff.highlightRanges) {
                    if (editRange.type == DiffResponse.HIGHLIGHT_TYPE_ADD) {
                        diffSize += editRange.length
                    } else {
                        diffSize -= editRange.length
                    }
                }
            }
        }
        return diffSize
    }

    class EditStats(val revision: MwQueryPage.Revision, val editCount: EditCount, val metrics: List<Metrics.Results>)
}
