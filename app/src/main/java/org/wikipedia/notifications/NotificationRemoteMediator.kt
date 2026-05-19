package org.wikipedia.notifications

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import org.wikipedia.Constants
import org.wikipedia.notifications.db.Notification

@OptIn(ExperimentalPagingApi::class)
class NotificationRemoteMediator(private val repository: NotificationRepository): RemoteMediator<Int, Notification>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Notification>
    ): MediatorResult {
        return try {
            val continueStr = when (loadType) {
                LoadType.REFRESH -> {
                    null
                }
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val key = repository.getRemoteKey(
                        Constants.NOTIFICATIONS_DB_REMOTE_KEY
                    ) ?: return MediatorResult.Success(
                        endOfPaginationReached = true
                    )
                    // above code will return if NULL is stored as key in the database
                    key // a non-NULL value is present.
                }
            }

            // upon refresh (also used for initial loading) load all notifications from the server
            // to the local database
            val nextContinueStr = if (loadType == LoadType.REFRESH) {
                repository.syncAll(Constants.NOTIFICATIONS_FILTER_CONFIG)
                null
            }
            else {
                repository.fetchAndSave(Constants.NOTIFICATIONS_FILTER_CONFIG, continueStr)
            }
            if (loadType == LoadType.REFRESH) {
                repository.clearRemoteKeys()
            }
            repository.saveRemoteKey(Constants.NOTIFICATIONS_DB_REMOTE_KEY, nextContinueStr)

            MediatorResult.Success(endOfPaginationReached = nextContinueStr == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
