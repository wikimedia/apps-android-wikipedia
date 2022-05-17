package org.wikipedia.random

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.Resource

class RandomItemViewModel(bundle: Bundle) : ViewModel() {

	private val wikiSite: WikiSite = bundle.getParcelable(RandomActivity.INTENT_EXTRA_WIKISITE)!!

	val requestRandomPageData = MutableLiveData<Resource<PageSummary>>()

	private val disposables = CompositeDisposable()

	init {
		getRandomPage()
	}

	fun getRandomPage() {
		val d = ServiceFactory.getRest(wikiSite).randomSummary
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe({ pageSummary ->
				requestRandomPageData.postValue(Resource.Success(pageSummary))
			}, { throwable ->
				requestRandomPageData.postValue(Resource.Error(throwable))
			})

		disposables.add(d)
	}

	override fun onCleared() {
		disposables.clear()

		super.onCleared()
	}

	class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
		@Suppress("unchecked_cast")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return RandomItemViewModel(bundle) as T
		}
	}
}
