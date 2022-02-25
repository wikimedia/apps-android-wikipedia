package org.wikipedia.page.edithistory

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.EditCount
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
                val articleMetricsResponse = ServiceFactory.getRest(WikiSite("wikimedia.org")).getArticleMetrics(pageTitle.wikiSite.authority(), pageTitle.prefixedText, lastYear, today)

                list.add(EditHistoryStats(mwResponse.query?.pages?.first()?.revisions?.first()!!, editCountsResponse, articleMetricsResponse.firstItem.results))

                // Edit history
                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode)).getEditHistoryDetails(pageTitle.prefixedText)
                val revisions = response.query!!.pages?.get(0)?.revisions!!
                list.addAll(revisions)
                editHistoryListData.postValue(Success(list))
            }
        }
    }
}
