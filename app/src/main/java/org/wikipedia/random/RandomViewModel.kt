package org.wikipedia.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wikipedia.database.AppDatabase
import org.wikipedia.page.PageTitle
import org.wikipedia.util.log.L

class RandomViewModel : ViewModel() {

	private var job: Job? = null

	private val errorHandler = CoroutineExceptionHandler { _, throwable ->
		L.w(throwable)
	}

	private val _titleExistsInListFlow = MutableStateFlow(false)
	val titleExistsInListFlow: StateFlow<Boolean> = _titleExistsInListFlow

	fun actualizeSaveShareButton(title: PageTitle) {
		job?.cancel()
		job = viewModelScope.launch(errorHandler) {
			withContext(Dispatchers.IO) {
				val exists =
					AppDatabase.instance.readingListPageDao().findPageInAnyList(title) != null
				_titleExistsInListFlow.value = exists
			}
		}
	}
}
