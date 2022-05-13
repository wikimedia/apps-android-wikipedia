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

	private val _saveShareState = MutableStateFlow(false)
	val saveShareState: StateFlow<Boolean> = _saveShareState

	fun actualizeSaveShare(title: PageTitle) {
		saveShareJob?.cancel()
		saveShareJob = viewModelScope.launch(handler) {
			withContext(Dispatchers.IO) {
				val exists =
					AppDatabase.instance.readingListPageDao().findPageInAnyList(title) != null
				_saveShareState.value = exists
			}
		}
	}

	class Factory : ViewModelProvider.Factory {
		@Suppress("unchecked_cast")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return RandomViewModel() as T
		}
	}
}
