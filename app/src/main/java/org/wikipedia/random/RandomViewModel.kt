package org.wikipedia.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wikipedia.database.AppDatabase
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L

class RandomViewModel : ViewModel() {

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

	class Factory() : ViewModelProvider.Factory {
		@Suppress("unchecked_cast")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return RandomViewModel() as T
		}
	}

	sealed class SaveSharedState<T>
	class Initial<T> : SaveSharedState<T>()
	class Result<T>(val value: T) : SaveSharedState<T>()

	data class MoveDataSource(val sourceReadingListId: Long, val title: PageTitle)
}