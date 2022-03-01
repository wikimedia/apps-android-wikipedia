package org.wikipedia.page.edithistory

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.Resource.Success
import org.wikipedia.util.log.L

class EditHistoryListViewModel(bundle: Bundle) : ViewModel() {

    var pageTitle: PageTitle = bundle.getParcelable(EditHistoryListActivity.INTENT_EXTRA_PAGE_TITLE)!!

    val editHistoryListData = MutableLiveData<Resource<List<Revision>>>()

    init {
        fetchData(pageTitle)
    }

    fun fetchData(pageTitle: PageTitle) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            withContext(Dispatchers.IO) {
                val response = ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.wikiSite.languageCode))
                    .getEditHistoryDetails(pageTitle.prefixedText)
                val revisions = response.query!!.pages?.get(0)?.revisions
                editHistoryListData.postValue(Success(revisions!!))
            }
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return EditHistoryListViewModel(bundle) as T
        }
    }
}
