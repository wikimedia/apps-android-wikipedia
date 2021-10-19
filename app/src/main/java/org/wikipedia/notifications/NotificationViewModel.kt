package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import org.wikipedia.notifications.db.Notification
import org.wikipedia.util.log.L

class NotificationViewModel : ViewModel() {

    // TODO: revisit this to see if there's a better approach
    fun interface CoroutineCallback {
        fun onError(throwable: Throwable)
    }

    fun interface FetchAndSaveCallback {
        fun onReceive(continueStr: String?)
    }

    private val notificationRepository = NotificationRepository(AppDatabase.getAppDatabase().notificationDao())
    private val handler = CoroutineExceptionHandler { _, exception ->
        coroutineCallback?.onError(exception)
    }
    var coroutineCallback: CoroutineCallback? = null

    fun fetchAndSave(wikiList: String?, filter: String?, continueStr: String?, callback: FetchAndSaveCallback) {
        viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
                callback.onReceive(notificationRepository.fetchAndSave(wikiList, filter, continueStr))
            }
        }
    }

    fun getList(): List<Notification> {
        val list = mutableListOf<Notification>()
        viewModelScope.launch(handler) {
            notificationRepository.getAllNotifications().collect {
                L.d("getList " + it.size)
                list.addAll(it)
            }
        }
        return list
    }
}
