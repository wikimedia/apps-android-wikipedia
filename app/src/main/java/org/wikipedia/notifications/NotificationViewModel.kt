package org.wikipedia.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification

class NotificationViewModel : ViewModel() {

    private val notificationRepository = NotificationRepository(AppDatabase.getAppDatabase().notificationDao())

    fun fetchAndSave(wikiList: String?, filter: String?, continueStr: String? = null) {
        var newContinueStr: String? = null
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val response = ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getAllNotificationsKT(wikiList, filter, continueStr)
                response.query?.notifications?.let {
                    // TODO: maybe add a logic to avoid adding same data into database.
                    notificationRepository.insertNotification(it.list.orEmpty())
                    newContinueStr = it.continueStr
                }
            }
        }
        // TODO: Save all notifications to database?
        if (!newContinueStr.isNullOrEmpty()) {
            fetchAndSave(wikiList, filter, newContinueStr)
        }
    }

    fun getList(): Flow<List<Notification>> {
        return notificationRepository.getAllNotifications()
    }
}
