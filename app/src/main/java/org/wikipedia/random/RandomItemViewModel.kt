package org.wikipedia.random

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.Resource

class RandomItemViewModel(bundle: Bundle) : ViewModel() {

	private val wikiSite: WikiSite = bundle.getParcelable(RandomActivity.INTENT_EXTRA_WIKISITE)!!

	private val _saveShareState = MutableStateFlow<SaveSharedState>(Initial)
	val saveShareState = _saveShareState.asStateFlow()

	private val disposables = CompositeDisposable()

	init {
		getRandomPage()
	}

	fun getRandomPage() {
		val d = ServiceFactory.getRest(wikiSite).randomSummary
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe({ pageSummary ->
				_saveShareState.value = Result(Resource.Success(pageSummary))
			}, { throwable ->
				_saveShareState.value = Result(Resource.Error(throwable))
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

	sealed class SaveSharedState
	object Initial : SaveSharedState()
	class Result(val value: Resource<PageSummary>) : SaveSharedState()
}
