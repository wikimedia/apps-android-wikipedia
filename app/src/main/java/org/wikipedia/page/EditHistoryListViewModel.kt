package org.wikipedia.page

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.util.Resource
import org.wikipedia.util.Resource.Success
import org.wikipedia.util.log.L

class EditHistoryListViewModel : ViewModel() {

    val editHistoryListData = MutableLiveData<Resource<List<Revision>>>()

    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
    }

    fun fetchData(pageTitle: PageTitle) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                    .getEditHistoryDetails(pageTitle.prefixedText)
                val revisions = response.query!!.pages?.get(0)?.revisions
                editHistoryListData.postValue(Success(revisions!!))
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
}
