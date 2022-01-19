package org.wikipedia.page

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.util.Resource
import org.wikipedia.util.Resource.Success
import org.wikipedia.util.log.L

class EditHistoryListModelView : ViewModel() {

    val editHistoryListData = MutableLiveData<Resource<List<Revision>>>()
    private val disposables = CompositeDisposable()

    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
    }

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                disposables.add(ServiceFactory.get(WikiSite.forLanguageCode("en")).getEditHistoryDetails("Earth").subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                        val revisions = it.query!!.pages?.get(0)?.revisions
                        editHistoryListData.postValue(Success(revisions!!))
                    }) { // setErrorState(it)
                    })
            }
        }
    }
}
