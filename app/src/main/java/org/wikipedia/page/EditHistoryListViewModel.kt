package org.wikipedia.page

import android.text.SpannableStringBuilder
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
    val editSizeDetailsData = MutableLiveData<Resource<EditSizeDetails>>()
    var diffSize: Int = 0
    var diffText: CharSequence = ""

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

    fun fetchEditDetails(languageCode: String, olderRevisionId: Long, revisionId: Long) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                val response: DiffResponse = ServiceFactory.getCoreRest(WikiSite.forLanguageCode(languageCode)).getEditDiff(olderRevisionId, revisionId)
                createSpannable(response.diff)
                editSizeDetailsData.postValue(Success(EditSizeDetails(diffSize, diffText)))
            }
        }
    }

    private fun createSpannable(diffs: List<DiffResponse.DiffItem>) {
        val spannableString = SpannableStringBuilder()
        diffSize = 0
        for (diff in diffs) {
            spannableString.append(diff.text.ifEmpty { "\n" })
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
            spannableString.append("\n")
        }
        diffText = spannableString
    }

    class EditSizeDetails(val diffSize: Int, val text: CharSequence)
}
