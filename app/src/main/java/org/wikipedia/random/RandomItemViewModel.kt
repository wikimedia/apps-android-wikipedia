package org.wikipedia.random

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
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.Resource

class RandomItemViewModel(bundle: Bundle) : ViewModel() {

	val wikiSite: WikiSite = bundle.getParcelable(RandomActivity.INTENT_EXTRA_WIKISITE)!!

	val requestRandomPageData = MutableLiveData<Resource<PageSummary>>()

	private val errorHandler = CoroutineExceptionHandler { _, throwable ->
		requestRandomPageData.value = Resource.Error(throwable)
	}

	init {
		getRandomPage()
	}

	fun getRandomPage() {
		viewModelScope.launch(errorHandler) {
			withContext(Dispatchers.IO) {
				val randomSummary = ServiceFactory.getRest(wikiSite).getRandomSummary()
				requestRandomPageData.postValue(Resource.Success(randomSummary))
			}
		}
	}

	class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
		@Suppress("unchecked_cast")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return RandomItemViewModel(bundle) as T
		}
	}
}
