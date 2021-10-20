package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import org.wikipedia.notifications.db.Notification

class NotificationViewModel : ViewModel() {

    fun interface FetchAndSaveCallback {
        fun onReceive(continueStr: String?)
    }

    private val notificationRepository = NotificationRepository(AppDatabase.getAppDatabase().notificationDao())
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }
    private val _uiState = MutableStateFlow<UiState>(UiState.Success(emptyList()))

    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch(handler) {
            collectAllNotifications()
        }
    }

    private suspend fun collectAllNotifications() = notificationRepository.getAllNotifications()
        .collect {
            _uiState.value = UiState.Success(it)
        }

    fun fetchAndSave(wikiList: String?, filter: String?, continueStr: String?, callback: FetchAndSaveCallback) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                callback.onReceive(notificationRepository.fetchAndSave(wikiList, filter, continueStr))
            }
            // TODO: revisit this
            collectAllNotifications()
        }
    }

    sealed class UiState {
        data class Success(val notifications: List<Notification>) : UiState()
        data class Error(val throwable: Throwable) : UiState()
    }
}
