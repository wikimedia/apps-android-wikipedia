package org.wikipedia.random

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.RandomizerFunnel
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L

class RandomViewModel(bundle: Bundle) : ViewModel() {

	private val funnel: RandomizerFunnel
	private var saveShareJob: Job? = null

	private val handler = CoroutineExceptionHandler { _, throwable ->
		L.w(throwable)
	}

	private val _savaShareState = MutableStateFlow(false)
	val savaShareState: StateFlow<Boolean> = _savaShareState

	private val _saveToDefaultList = MutableStateFlow<SaveSharedState<PageTitle>>(Initial())
	val saveToDefaultList: StateFlow<SaveSharedState<PageTitle>> = _saveToDefaultList

	private val _saveToCustomList = MutableStateFlow<SaveSharedState<PageTitle>>(Initial())
	val saveToCustomList: StateFlow<SaveSharedState<PageTitle>> = _saveToCustomList

	private val _movePageToList = MutableStateFlow<SaveSharedState<MoveDataSource>>(Initial())
	val movePageToList: StateFlow<SaveSharedState<MoveDataSource>> = _movePageToList

	init {
		val wikiSite = bundle.getParcelable<WikiSite>(RandomActivity.INTENT_EXTRA_WIKISITE)
		val invokeSource =
			bundle.getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource
		funnel = RandomizerFunnel(WikipediaApp.getInstance(), wikiSite, invokeSource)
	}

	fun saveToDefaultList(title: PageTitle) {
		_saveToDefaultList.value = Result(title)
	}

	fun saveToCustomList(title: PageTitle) {
		_saveToCustomList.value = Result(title)
	}

	fun movePageToList(sourceReadingListId: Long, title: PageTitle) {
		_movePageToList.value = Result(MoveDataSource(sourceReadingListId, title))
	}

	fun actualizeSaveShare(title: PageTitle) {
		saveShareJob?.cancel()
		saveShareJob = viewModelScope.launch(handler) {
			withContext(Dispatchers.IO) {
				val exists =
					AppDatabase.instance.readingListPageDao().findPageInAnyList(title) != null
				_savaShareState.value = exists
			}
		}
	}

	fun clickedForward() {
		funnel.execAction(RandomizerFunnel.Action.CLICK_FORWARD)
	}

	fun clickedBack() {
		funnel.execAction(RandomizerFunnel.Action.CLICK_BACK)
	}

	fun swipedForward() {
		funnel.execAction(RandomizerFunnel.Action.SWIPE_FORWARD)
	}

	fun swipedBack() {
		funnel.execAction(RandomizerFunnel.Action.SWIPE_BACK)
	}

	override fun onCleared() {
		super.onCleared()

		funnel.done()
	}

	class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
		@Suppress("unchecked_cast")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return RandomViewModel(bundle) as T
		}
	}

	sealed class SaveSharedState<T>
	class Initial<T> : SaveSharedState<T>()
	class Result<T>(val value: T) : SaveSharedState<T>()

	data class MoveDataSource(val sourceReadingListId: Long, val title: PageTitle)
}