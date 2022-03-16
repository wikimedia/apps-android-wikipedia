package org.wikipedia.categories

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
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource

class CategoryDialogViewModel(bundle: Bundle) : ViewModel() {

    val pageTitle = bundle.getParcelable<PageTitle>(CategoryDialog.ARG_TITLE)!!
    val categoriesData = MutableLiveData<Resource<List<MwQueryPage.Category>>>()

    init {
        fetchCategories()
    }

    private fun fetchCategories() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            categoriesData.postValue(Resource.Error(throwable))
        }) {
            withContext(Dispatchers.IO) {
                val response = ServiceFactory.get(pageTitle.wikiSite).getCategories(pageTitle.prefixedText)
                categoriesData.postValue(Resource.Success(response.query!!.firstPage()!!.categories!!.filter { !it.hidden }))
            }
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return CategoryDialogViewModel(bundle) as T
        }
    }
}
