package org.wikipedia.categories

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource

class CategoryDialogViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val pageTitle = savedStateHandle.get<PageTitle>(Constants.ARG_TITLE)!!
    val categoriesData = MutableLiveData<Resource<List<PageTitle>>>()

    init {
        fetchCategories()
    }

    private fun fetchCategories() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            categoriesData.postValue(Resource.Error(throwable))
        }) {
            val response = ServiceFactory.get(pageTitle.wikiSite).getCategories(pageTitle.prefixedText)
            val titles = response.query?.pages?.map { page ->
                PageTitle(page.title, pageTitle.wikiSite).also {
                    it.displayText = page.displayTitle(pageTitle.wikiSite.languageCode)
                }
            }.orEmpty()
            categoriesData.postValue(Resource.Success(titles))
        }
    }
}
